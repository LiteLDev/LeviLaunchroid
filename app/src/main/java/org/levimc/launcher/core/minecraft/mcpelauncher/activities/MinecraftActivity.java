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
package org.levimc.launcher.core.minecraft.mcpelauncher.activities;

import android.content.res.AssetManager;
import android.os.Bundle;

import org.levimc.launcher.core.minecraft.mcpelauncher.Application;
import org.levimc.launcher.core.minecraft.pesdk.PESdk;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class MinecraftActivity extends com.mojang.minecraftpe.MainActivity {
    protected PESdk getPESdk() {
        return Application.mPESdk;
    }

    @Override
    public AssetManager getAssets() {
        return getPESdk().getGameManager().getAssets();
    }

    @Override
    public void onCreate(Bundle p1) {
        getPESdk().getGameManager().onMinecraftActivityCreate(this, p1);
        super.onCreate(p1);
    }
}
