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

import android.content.res.AssetManager;
import android.os.Bundle;

import com.mojang.minecraftpe.MainActivity;

import org.jetbrains.annotations.NotNull;
import org.levimc.launcher.core.minecraft.pesdk.utils.AssetOverrideManager;
import org.levimc.launcher.core.minecraft.pesdk.utils.MinecraftInfo;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class GameManager {
    private final PESdk mPESdk;
    private final ArrayList<String> patchAssetPath = new ArrayList<>();

    GameManager(PESdk pesdk) {
        mPESdk = pesdk;
    }

    public AssetManager getAssets() {
        return mPESdk.getMinecraftInfo().getAssets();
    }

    public void onMinecraftActivityCreate(@NotNull MainActivity activity, Bundle savedInstanceState) {
        //AssetOverrideManager.addAssetOverride(activity.getAssets(), MinecraftInfo.getMinecraftPackageContext().getPackageResourcePath());

        String basePath = MinecraftInfo.getMinecraftPackageContext().getPackageResourcePath();
        patchAssetPath.add(basePath);
        /* In 1.17.30(beta version unknown), almost all assets files were moved to
         * split_install_pack.apk, including bootstrap.json, a file that is crucial to
         * launching the game.
         */
        String splitPath = basePath.replace("base.apk", "split_install_pack.apk");
        File splitFile = new File(splitPath);
        if (splitFile.exists()) {
            patchAssetPath.add(splitPath);
        }

        for (String assetPath : patchAssetPath) {
            AssetOverrideManager.addAssetOverride(activity.getAssets(), assetPath);
        }
    }
}