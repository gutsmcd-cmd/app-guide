package io.github.gutsmcd.appguide

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.io.File

class AppItem(
    val pkg: String,
    val label: String,
    val installed: Long,
    val updated: Long,
    val sizeBytes: Long,
    val systemCategory: String,
    val info: ApplicationInfo,
) {
    var icon: Drawable? = null
}

/** Lists apps that appear in the launcher, using the <queries> launcher intent (no QUERY_ALL_PACKAGES). */
object Catalog {
    fun load(c: Context): List<AppItem> {
        val pm = c.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == c.packageName || !seen.add(pkg)) continue
            val pi = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: continue
            val ai = pi.applicationInfo ?: continue
            var size = 0L
            runCatching {
                size = File(ai.sourceDir).length()
                ai.splitSourceDirs?.forEach { size += File(it).length() }
            }
            out += AppItem(pkg, ai.loadLabel(pm).toString(), pi.firstInstallTime, pi.lastUpdateTime,
                size, Categories.fromSystem(ai.category), ai)
        }
        return out
    }

    fun icon(c: Context, item: AppItem): Drawable? {
        item.icon?.let { return it }
        return runCatching { item.info.loadIcon(c.packageManager) }.getOrNull().also { item.icon = it }
    }

    fun launchIntent(pm: PackageManager, pkg: String) = pm.getLaunchIntentForPackage(pkg)
}
