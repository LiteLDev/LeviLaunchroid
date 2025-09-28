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
package org.levimc.launcher.core.minecraft.mcpelauncher.data

import androidx.preference.PreferenceManager
import org.levimc.launcher.core.minecraft.mcpelauncher.Application

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
object Preferences {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(Application.getContext())

    @JvmStatic
    val minecraftPackageName: String?
        get() = preferences.getString("pkg_name", "com.mojang.minecraftpe")

    @JvmStatic
    fun setMinecraftPackageName(str: String?) {
        preferences.edit().putString("pkg_name", str).apply()
    }

    @JvmStatic
    var openGameFailed: String?
        get() = preferences.getString("open_game_failed_msg", null)
        set(str) {
            preferences.edit().putString("open_game_failed_msg", str).apply()
        }
}