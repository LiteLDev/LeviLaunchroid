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
package org.levimc.launcher.core.minecraft.pesdk.utils;

import android.content.res.AssetManager;

import java.lang.reflect.Method;

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class AssetOverrideManager {
    private static AssetOverrideManager mInstance;
    private AssetManager mLocalAssetManager;

    private AssetOverrideManager() {
        try {
            mLocalAssetManager = AssetManager.class.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addAssetOverride(AssetManager mgr, String packageResourcePath) {
        try {
            Method method = AssetManager.class.getMethod("addAssetPath", String.class);
            method.invoke(mgr, packageResourcePath);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static AssetOverrideManager getInstance() {
        if (mInstance == null)
            return mInstance = new AssetOverrideManager();
        return mInstance;
    }

    public static void newInstance() {
        mInstance = new AssetOverrideManager();
    }

    public void addAssetOverride(String packageResourcePath) {
        try {
            Method method = AssetManager.class.getMethod("addAssetPath", String.class);
            method.invoke(mLocalAssetManager, packageResourcePath);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    AssetManager getAssetManager() {
        return mLocalAssetManager;
    }
}
