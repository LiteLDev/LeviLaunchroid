package org.levimc.launcher.core.minecraft

import android.util.Log
import java.io.File

object MinecraftCacheCleaner {
    private const val TAG = "MCCacheCleaner"

    private const val EPHEMERAL_CACHE_MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000

    private const val PACK_CACHE_MAX_BYTES = 256L * 1024 * 1024

    private val EPHEMERAL_SUBDIRS = arrayOf(
        "WebView",
        "http_cache",
        "httpclient"
    )

    private val SIZE_LIMITED_SUBDIRS = arrayOf(
        "resource_packs",
        "premium_cache",
        "persona_cache",
        "pack_cache",
        "skin_packs"
    )

    data class CleanupResult(
        val deletedFiles: Int = 0,
        val deletedBytes: Long = 0,
        val errors: Int = 0,
    ) {
        operator fun plus(other: CleanupResult) = CleanupResult(
            deletedFiles + other.deletedFiles,
            deletedBytes + other.deletedBytes,
            errors + other.errors,
        )
    }

    @JvmStatic
    fun cleanupBeforeLaunch(
        cacheDir: File?,
        dataDir: File?,
        externalFilesDir: File?,
        trace: LaunchTrace? = null,
    ): CleanupResult {
        var total = CleanupResult()

        if (cacheDir != null && cacheDir.isDirectory) {
            trace?.mark("Cache cleanup started", cacheDir.absolutePath)
            total = total + cleanEphemeralCaches(cacheDir)
            total = total + enforceSizeLimits(cacheDir)
            total = total + cleanStaleTempFiles(cacheDir)
        }

        if (externalFilesDir != null && externalFilesDir.isDirectory) {
            total = total + enforceSizeLimit(File(externalFilesDir, "premium_cache"), PACK_CACHE_MAX_BYTES)
            total = total + enforceSizeLimit(File(externalFilesDir, "server_resource_packs"), PACK_CACHE_MAX_BYTES)
            total = total + enforceSizeLimit(File(externalFilesDir, "minecraftpe/server_resource_packs"), PACK_CACHE_MAX_BYTES)
            total = total + enforceSizeLimit(File(externalFilesDir, "minecraftpe/packcache"), PACK_CACHE_MAX_BYTES)
            total = total + cleanStaleTempFiles(externalFilesDir)
        }
        
        trace?.mark(
            "Cache cleanup finished",
            "${total.deletedFiles} files, ${total.deletedBytes / 1024}KB freed, ${total.errors} errors"
        )
        if (dataDir != null && dataDir.isDirectory) {
            total = total + cleanStaleDataFiles(dataDir, trace)
        }

        if (total.deletedFiles > 0) {
            Log.i(TAG, "Cleaned ${total.deletedFiles} files, freed ${total.deletedBytes / 1024}KB")
        }
        return total
    }

    /**
     * Deletes stale files in ephemeral cache subdirectories.
     */
    private fun cleanEphemeralCaches(cacheDir: File): CleanupResult {
        var result = CleanupResult()
        val cutoff = System.currentTimeMillis() - EPHEMERAL_CACHE_MAX_AGE_MS

        for (subdir in EPHEMERAL_SUBDIRS) {
            val dir = File(cacheDir, subdir)
            if (!dir.isDirectory) continue
            result = result + deleteStaleRecursive(dir, cutoff, deleteEmptyDirs = true)
        }
        return result
    }

    private fun enforceSizeLimits(cacheDir: File): CleanupResult {
        var result = CleanupResult()

        for (subdir in SIZE_LIMITED_SUBDIRS) {
            val dir = File(cacheDir, subdir)
            if (!dir.isDirectory) continue
            result = result + enforceSizeLimit(dir, PACK_CACHE_MAX_BYTES)
        }
        return result
    }

    private fun cleanStaleTempFiles(cacheDir: File): CleanupResult {
        var deleted = 0
        var bytes = 0L
        var errors = 0
        val cutoff = System.currentTimeMillis() - EPHEMERAL_CACHE_MAX_AGE_MS

        val files = cacheDir.listFiles() ?: return CleanupResult()
        for (file in files) {
            if (!file.isFile) continue
            val name = file.name
            if (name.endsWith(".tmp") || name.endsWith(".temp") || name.startsWith("tmp_")) {
                if (file.lastModified() < cutoff) {
                    val size = file.length()
                    if (safeDelete(file)) {
                        deleted++
                        bytes += size
                    } else {
                        errors++
                    }
                }
            }
        }
        return CleanupResult(deleted, bytes, errors)
    }

    private fun cleanStaleDataFiles(dataDir: File, trace: LaunchTrace?): CleanupResult {
        var deleted = 0
        var bytes = 0L
        var errors = 0

        val files = dataDir.listFiles() ?: return CleanupResult()
        for (file in files) {
            if (!file.isFile) continue
            val name = file.name
            if (name.startsWith("OfflineStorage") ||
                name.startsWith("1ds-") ||
                name.startsWith("mat-") ||
                (name.endsWith("-wal") && file.length() > 4 * 1024 * 1024) ||
                (name.endsWith("-journal") && file.length() > 2 * 1024 * 1024)
            ) {
                val size = file.length()
                if (safeDelete(file)) {
                    deleted++
                    bytes += size
                    trace?.mark("Cleaned stale data file", "$name (${size / 1024}KB)")
                } else {
                    errors++
                }
            }
        }
        return CleanupResult(deleted, bytes, errors)
    }

    private fun enforceSizeLimit(dir: File, maxBytes: Long): CleanupResult {
        val allFiles = mutableListOf<File>()
        collectFilesRecursive(dir, allFiles)

        var totalSize = allFiles.sumOf { it.length() }
        if (totalSize <= maxBytes) return CleanupResult()

        // Sort oldest first for eviction
        allFiles.sortBy { it.lastModified() }

        var deleted = 0
        var freedBytes = 0L
        var errors = 0

        for (file in allFiles) {
            if (totalSize <= maxBytes) break
            val size = file.length()
            if (safeDelete(file)) {
                deleted++
                freedBytes += size
                totalSize -= size
            } else {
                errors++
            }
        }

        // Clean up any now-empty directories
        cleanEmptyDirs(dir)

        return CleanupResult(deleted, freedBytes, errors)
    }

    private fun deleteStaleRecursive(dir: File, cutoffMs: Long, deleteEmptyDirs: Boolean): CleanupResult {
        var deleted = 0
        var bytes = 0L
        var errors = 0

        val children = dir.listFiles() ?: return CleanupResult()
        for (child in children) {
            if (child.isDirectory) {
                val sub = deleteStaleRecursive(child, cutoffMs, deleteEmptyDirs)
                deleted += sub.deletedFiles
                bytes += sub.deletedBytes
                errors += sub.errors
                if (deleteEmptyDirs) {
                    val remaining = child.listFiles()
                    if (remaining == null || remaining.isEmpty()) {
                        child.delete()
                    }
                }
            } else if (child.isFile && child.lastModified() < cutoffMs) {
                val size = child.length()
                if (safeDelete(child)) {
                    deleted++
                    bytes += size
                } else {
                    errors++
                }
            }
        }
        return CleanupResult(deleted, bytes, errors)
    }

    private fun collectFilesRecursive(dir: File, out: MutableList<File>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isFile) {
                out.add(child)
            } else if (child.isDirectory) {
                collectFilesRecursive(child, out)
            }
        }
    }

    private fun cleanEmptyDirs(dir: File) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                cleanEmptyDirs(child)
                val remaining = child.listFiles()
                if (remaining == null || remaining.isEmpty()) {
                    child.delete()
                }
            }
        }
    }

    private fun safeDelete(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete ${file.name}: ${e.message}")
            false
        }
    }
}
