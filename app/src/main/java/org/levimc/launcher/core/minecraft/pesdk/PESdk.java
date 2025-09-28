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

import org.levimc.launcher.core.minecraft.pesdk.utils.MinecraftInfo;


/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class PESdk {
    private final MinecraftInfo mMinecraftInfo;
    private final GameManager mGameManager;
    private boolean mIsInited;

    public PESdk(Context context) {
        mMinecraftInfo = new MinecraftInfo(context);
        mGameManager = new GameManager(this);
        mIsInited = false;
    }

    public MinecraftInfo getMinecraftInfo() {
        return mMinecraftInfo;
    }


    public GameManager getGameManager() {
        return mGameManager;
    }
}
