/*
 * Copyright (C) 2018-2021 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.levimc.launcher.core.minecraft.pesdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.levimc.launcher.core.minecraft.pesdk.utils.MinecraftInfo;
import org.levimc.launcher.util.Logger;

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class PESdk {
    private MinecraftInfo mMinecraftInfo;
    private GameManager mGameManager;
    private boolean mIsInited;
    private boolean mIsInitializing;
    private int mInitRetryCount = 0;
    private static final int MAX_INIT_RETRIES = 2;

    public PESdk(Context context) {
        initializeComponents(context);
    }

    private void initializeComponents(Context context) {
        if (mIsInitializing) {
            return;
        }

        mIsInitializing = true;

        try {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            mMinecraftInfo = new MinecraftInfo(context);
            mGameManager = new GameManager(this);
            mIsInited = true;
        } catch (Exception e) {
            Logger.get().error("Failed to initialize PESdk: " + e.getMessage(), e);

            if (mInitRetryCount < MAX_INIT_RETRIES) {
                mInitRetryCount++;
                Logger.get().info("Retrying PESdk initialization (attempt " + mInitRetryCount + " of " + MAX_INIT_RETRIES + ")");

                mMinecraftInfo = null;
                mGameManager = null;

                System.gc();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mIsInitializing = false;
                    initializeComponents(context);
                }, 500);
            } else {
                mIsInitializing = false;
                throw new RuntimeException("Failed to initialize PESdk after multiple attempts", e);
            }
        } finally {
            mIsInitializing = false;
        }
    }

    public MinecraftInfo getMinecraftInfo() {
        if (mMinecraftInfo == null) {
            throw new IllegalStateException("MinecraftInfo is not initialized");
        }
        return mMinecraftInfo;
    }

    public GameManager getGameManager() {
        if (mGameManager == null) {
            throw new IllegalStateException("GameManager is not initialized");
        }
        return mGameManager;
    }

    public boolean isInitialized() {
        return mIsInited && mMinecraftInfo != null && mGameManager != null;
    }
}
