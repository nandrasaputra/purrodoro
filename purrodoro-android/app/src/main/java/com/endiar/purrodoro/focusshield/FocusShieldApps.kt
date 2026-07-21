package com.endiar.purrodoro.focusshield

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

object FocusShieldApps {

    /** Common distraction packages suggested when installed. */
    val SUGGESTED_PACKAGES: Set<String> = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.google.android.youtube",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.snapchat.android",
        "com.ss.android.ugc.trill",
    )

    fun loadLaunchableApps(pm: PackageManager, ownPackage: String): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ownPackage) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    .ifBlank { pkg }
                LaunchableApp(
                    packageName = pkg,
                    label = label,
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun isSystemApp(pm: PackageManager, packageName: String): Boolean {
        return runCatching {
            val flags = pm.getApplicationInfo(packageName, 0).flags
            flags and ApplicationInfo.FLAG_SYSTEM != 0
        }.getOrDefault(false)
    }
}
