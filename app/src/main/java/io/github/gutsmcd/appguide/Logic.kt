package io.github.gutsmcd.appguide

import java.util.Locale

/** Pure helpers (no Android types) so they can be unit-tested on the JVM. */
data class PlayInfo(val description: String, val screenshots: List<String>)

object PlayParser {
    const val MAX_SHOTS = 4
    private val META_DESC = Regex("""<meta\s+name="description"[^>]*?content="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val OG_DESC = Regex("""<meta\s+[^>]*property="og:description"[^>]*?content="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val IMG = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val SRC = Regex("""\ssrc="(https://play-lh\.googleusercontent\.com/[^"\s]+)"""")

    fun playUrl(pkg: String, lang: String): String =
        "https://play.google.com/store/apps/details?id=$pkg&hl=${if (lang == "ja") "ja" else "en"}"

    /** Returns null when the page has no description (not a normal app page). */
    fun parse(html: String): PlayInfo? {
        val raw = META_DESC.find(html)?.groupValues?.get(1) ?: OG_DESC.find(html)?.groupValues?.get(1)
        val desc = raw?.let { unescape(it).trim() }.orEmpty()
        if (desc.isEmpty()) return null
        val shots = LinkedHashSet<String>()
        for (m in IMG.findAll(html)) {
            val tag = m.value
            if (!tag.contains("data-screenshot-index") && !tag.contains("alt=\"Screenshot", true)) continue
            val src = SRC.find(tag)?.groupValues?.get(1) ?: continue
            shots += resize(unescape(src))
            if (shots.size >= MAX_SHOTS) break
        }
        return PlayInfo(desc, shots.toList())
    }

    /** Ask Google's image server for a phone-friendly width. */
    fun resize(url: String): String = url.substringBefore('=') + "=w720"

    fun unescape(s: String): String {
        var out = s.replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        out = Regex("&#(\\d+);").replace(out) { it.groupValues[1].toIntOrNull()?.let { c -> String(Character.toChars(c)) } ?: it.value }
        out = Regex("&#x([0-9a-fA-F]+);").replace(out) { it.groupValues[1].toIntOrNull(16)?.let { c -> String(Character.toChars(c)) } ?: it.value }
        return out.replace("&amp;", "&")
    }
}

object Categories {
    val DEFAULTS = listOf("health", "work", "photo", "tools", "learning", "games", "travel", "money", "social", "other")

    /** Map Android's ApplicationInfo.category (API 26) to one of the default keys. */
    fun fromSystem(cat: Int): String = when (cat) {
        0 -> "games"            // CATEGORY_GAME
        1, 2, 3 -> "photo"      // AUDIO, VIDEO, IMAGE
        4 -> "social"           // SOCIAL
        5 -> "learning"         // NEWS
        6 -> "travel"           // MAPS
        7 -> "work"             // PRODUCTIVITY
        8 -> "tools"            // ACCESSIBILITY
        else -> "other"         // UNDEFINED
    }

    fun isDefault(key: String) = key in DEFAULTS
}

object Text {
    fun matches(query: String, vararg fields: String?): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return true
        val hay = fields.filterNotNull().joinToString("\n").lowercase(Locale.ROOT)
        return q.split(Regex("\\s+")).all { hay.contains(it) }
    }

    fun size(bytes: Long): String = when {
        bytes <= 0 -> "—"
        bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%d KB", (bytes + 1023) / 1024)
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0)
        else -> String.format(Locale.ROOT, "%.2f GB", bytes / 1073741824.0)
    }

    fun cleanCategory(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(30)
}
