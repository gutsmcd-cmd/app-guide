package io.github.gutsmcd.appguide

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What the user (or a Fetch) saved about one app. */
class Entry(
    var category: String? = null,   // null = use Android's own category
    var note: String = "",
    var playDesc: String = "",
    var shots: Int = 0,
    var fetchedAt: Long = 0,
    var notOnPlay: Boolean = false,
)

/** Everything lives in one JSON file plus screenshot files, all in app-private storage. */
object Store {
    private var cache: MutableMap<String, Entry>? = null
    private var customCats: MutableList<String> = mutableListOf()
    private lateinit var file: File

    @Synchronized
    fun load(c: Context): MutableMap<String, Entry> {
        cache?.let { return it }
        file = File(c.applicationContext.filesDir, "guide.json")
        val map = HashMap<String, Entry>()
        if (file.exists()) runCatching {
            val root = JSONObject(file.readText())
            val apps = root.optJSONObject("apps") ?: JSONObject()
            for (k in apps.keys()) {
                val o = apps.getJSONObject(k)
                map[k] = Entry(
                    o.optString("cat").ifEmpty { null }, o.optString("note"), o.optString("desc"),
                    o.optInt("shots"), o.optLong("fetched"), o.optBoolean("notOnPlay"),
                )
            }
            val cats = root.optJSONArray("customCategories") ?: JSONArray()
            for (i in 0 until cats.length()) customCats += cats.getString(i)
        }
        cache = map
        return map
    }

    fun get(c: Context, pkg: String): Entry = load(c)[pkg] ?: Entry()

    @Synchronized
    fun put(c: Context, pkg: String, e: Entry) {
        load(c)[pkg] = e
        save()
    }

    fun customCategories(c: Context): List<String> { load(c); return customCats.toList() }

    @Synchronized
    fun addCategory(c: Context, name: String) {
        load(c)
        if (name.isNotEmpty() && !Categories.isDefault(name) && name !in customCats) { customCats += name; save() }
    }

    @Synchronized
    private fun save() {
        val apps = JSONObject()
        for ((k, e) in cache ?: return) {
            apps.put(k, JSONObject().apply {
                e.category?.let { put("cat", it) }
                put("note", e.note); put("desc", e.playDesc); put("shots", e.shots)
                put("fetched", e.fetchedAt); put("notOnPlay", e.notOnPlay)
            })
        }
        val root = JSONObject().put("apps", apps).put("customCategories", JSONArray(customCats))
        val tmp = File(file.parentFile, "guide.json.tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    fun shotFile(c: Context, pkg: String, i: Int) = File(File(c.filesDir, "shots").apply { mkdirs() }, "${pkg}_$i.img")
}
