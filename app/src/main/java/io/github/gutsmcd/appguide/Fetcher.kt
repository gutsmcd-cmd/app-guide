package io.github.gutsmcd.appguide

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Reads public Google Play pages, only when the user taps a Fetch button. */
object Fetcher {
    enum class Result { OK, NOT_FOUND, ERROR }

    private val exec = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private fun get(url: String, max: Int): ByteArray? {
        val con = URL(url).openConnection() as HttpURLConnection
        try {
            con.connectTimeout = 15000; con.readTimeout = 20000
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            if (con.responseCode == 404) return null
            if (con.responseCode !in 200..299) throw IOException("HTTP ${con.responseCode}")
            con.inputStream.use { ins ->
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(16384)
                while (true) {
                    val n = ins.read(buf); if (n < 0) break
                    bos.write(buf, 0, n)
                    if (bos.size() > max) throw IOException("too big")
                }
                return bos.toByteArray()
            }
        } finally { con.disconnect() }
    }

    /** Blocking; call off the main thread. */
    fun fetchNow(c: Context, pkg: String): Result {
        return try {
            val html = get(PlayParser.playUrl(pkg, Prefs.lang(c)), 8 * 1024 * 1024)
            val e = Store.get(c, pkg)
            val info = html?.let { PlayParser.parse(String(it, Charsets.UTF_8)) }
            if (info == null) {
                e.notOnPlay = true; e.fetchedAt = System.currentTimeMillis()
                Store.put(c, pkg, e); return Result.NOT_FOUND
            }
            var n = 0
            for (u in info.screenshots) {
                val bytes = runCatching { get(u, 4 * 1024 * 1024) }.getOrNull() ?: continue
                Store.shotFile(c, pkg, n).writeBytes(bytes); n++
            }
            for (i in n until PlayParser.MAX_SHOTS) Store.shotFile(c, pkg, i).delete()
            e.playDesc = info.description; e.shots = n; e.notOnPlay = false
            e.fetchedAt = System.currentTimeMillis()
            Store.put(c, pkg, e)
            Result.OK
        } catch (_: Exception) { Result.ERROR }
    }

    fun fetchOne(c: Context, pkg: String, done: (Result) -> Unit) {
        val app = c.applicationContext
        exec.execute { val r = fetchNow(app, pkg); main.post { done(r) } }
    }

    // ---- Fetch all: survives screen rotation, cancelable ----
    @Volatile var running = false; private set
    @Volatile private var cancel = false
    var progress = 0; private set
    var total = 0; private set
    var found = 0; private set
    var missing = 0; private set
    var lastDone = false; private set
    private val listeners = LinkedHashSet<() -> Unit>()
    fun addListener(l: () -> Unit) { listeners += l }
    fun removeListener(l: () -> Unit) { listeners -= l }
    private fun notifyAll_() = main.post { listeners.toList().forEach { it() } }

    fun startAll(c: Context, pkgs: List<String>) {
        if (running) return
        val app = c.applicationContext
        running = true; cancel = false; lastDone = false
        progress = 0; total = pkgs.size; found = 0; missing = 0
        notifyAll_()
        exec.execute {
            var errorsInRow = 0
            for (p in pkgs) {
                if (cancel) break
                when (fetchNow(app, p)) {
                    Result.OK -> { found++; errorsInRow = 0 }
                    Result.NOT_FOUND -> { missing++; errorsInRow = 0 }
                    Result.ERROR -> errorsInRow++
                }
                progress++
                notifyAll_()
                if (errorsInRow >= 3 && found + missing == 0) break // probably offline
                Thread.sleep(300) // be gentle with Google Play
            }
            running = false; lastDone = !cancel
            notifyAll_()
        }
    }

    fun cancelAll() { cancel = true }
}
