package org.levimc.launcher.core.minecraft.pesdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.levimc.launcher.core.minecraft.mcpelauncher.data.Preferences

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

object MinecraftPackageHelper {
    private const val TAG = "MinecraftPackageHelper"

    private val KNOWN_MINECRAFT_PACKAGES = arrayOf(
        "com.mojang.minecraftpe",
        "com.mojang.minecraftpe.beta",
        "com.mojang.minecraftpe.preview",
    )
    fun isPackageInstalled(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false

        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun findInstalledMinecraftPackages(context: Context): List<String> {
        val installedPackages = mutableListOf<String>()

        for (packageName in KNOWN_MINECRAFT_PACKAGES) {
            if (isPackageInstalled(context, packageName)) {
                installedPackages.add(packageName)
                Log.d(TAG, "Found installed Minecraft package: $packageName")
            }
        }

        return installedPackages
    }

    fun autoDetectMinecraftPackage(context: Context): String? {
        val installedPackages = findInstalledMinecraftPackages(context)

        if (installedPackages.isEmpty()) {
            Log.w(TAG, "No Minecraft packages found on device")
            return null
        }

        val preferredPackage = installedPackages.firstOrNull { it == "com.mojang.minecraftpe" }
            ?: installedPackages.first()

        Log.i(TAG, "Auto-detected Minecraft package: $preferredPackage")
        Preferences.setMinecraftPackageName(preferredPackage)

        return preferredPackage
    }
    fun getMinecraftInstallationInfo(context: Context): String {
        val info = StringBuilder()
        val currentPackage = Preferences.minecraftPackageName

        info.append("Current configured package: $currentPackage\n")
        info.append("Package installed: ${isPackageInstalled(context, currentPackage)}\n")

        val installedPackages = findInstalledMinecraftPackages(context)
        info.append("All installed Minecraft packages: ${installedPackages.joinToString()}\n")

        if (currentPackage != null && isPackageInstalled(context, currentPackage)) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(currentPackage, 0)
                @Suppress("DEPRECATION")
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
                info.append("Version: ${packageInfo.versionName} ($versionCode)\n")
                info.append("Install location: ${packageInfo.applicationInfo?.sourceDir}\n")
                info.append("Native lib dir: ${packageInfo.applicationInfo?.nativeLibraryDir}\n")
                info.append("Split APKs: ${packageInfo.applicationInfo?.splitPublicSourceDirs?.joinToString()}\n")
            } catch (e: Exception) {
                info.append("Error getting package info: ${e.message}\n")
            }
        }

        return info.toString()
    }
    fun suggestFixes(context: Context): List<String> {
        val suggestions = mutableListOf<String>()
        val currentPackage = Preferences.minecraftPackageName

        if (!isPackageInstalled(context, currentPackage)) {
            suggestions.add("Install Minecraft PE from Google Play Store")

            val installedPackages = findInstalledMinecraftPackages(context)
            if (installedPackages.isNotEmpty()) {
                suggestions.add("Switch to detected Minecraft package: ${installedPackages.first()}")
            }
        }

        suggestions.add("Ensure Minecraft PE is updated to the latest version")
        suggestions.add("Try clearing Minecraft PE app data and cache")
        suggestions.add("Restart the device and try again")

        return suggestions
    }
}