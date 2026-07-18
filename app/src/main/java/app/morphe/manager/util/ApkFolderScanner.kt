/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.util

import androidx.core.content.pm.PackageInfoCompat
import java.io.File

/** Metadata for a regular APK discovered in a user-selected folder. */
data class DiscoveredApk(
    val file: File,
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

/**
 * Reads APK archive metadata from a folder without opening a file picker.
 *
 * This deliberately supports only plain APK files: split archives still need the existing
 * picker/merge flow, while a plain APK can be selected directly from the version list.
 */
class ApkFolderScanner(private val pm: PM) {
    fun scan(folderPath: String, includeSubdirectories: Boolean, packageName: String): List<DiscoveredApk> {
        val folder = File(folderPath)
        if (!folder.isDirectory || !folder.canRead()) return emptyList()

        val files = if (includeSubdirectories) {
            folder.walkTopDown().maxDepth(MAX_DEPTH)
        } else {
            folder.listFiles()?.asSequence() ?: emptySequence()
        }

        return files
            .asSequence()
            .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
            .mapNotNull { file ->
                val info = pm.getPackageInfo(file) ?: return@mapNotNull null
                val versionName = info.versionName?.takeUnless(String::isBlank) ?: return@mapNotNull null
                if (info.packageName != packageName) return@mapNotNull null

                DiscoveredApk(
                    file = file,
                    packageName = info.packageName,
                    versionName = versionName,
                    versionCode = PackageInfoCompat.getLongVersionCode(info)
                )
            }
            .sortedWith(compareByDescending<DiscoveredApk> { it.file.lastModified() }.thenBy { it.file.name })
            .toList()
    }

    private companion object {
        // Enough for normal Downloads hierarchies while preventing a costly full-storage walk.
        const val MAX_DEPTH = 16
    }
}
