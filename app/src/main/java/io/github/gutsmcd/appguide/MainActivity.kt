package io.github.gutsmcd.appguide

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import java.util.concurrent.Executors

/** App list with search, category filter and "Fetch all". Built in code, no AndroidX. */
class MainActivity : Activity() {
    private lateinit var loc: Context
    private var all: List<AppItem> = emptyList()
    private var shown: List<AppItem> = emptyList()
    private var query = ""
    private var filter: String? = null
    private val adapter = Adapter()
    private lateinit var chipRow: LinearLayout
    private lateinit var countView: TextView
    private lateinit var fetchRow: LinearLayout
    private val loader = Executors.newSingleThreadExecutor()
    private val fetchListener: () -> Unit = { if (!isFinishing) { renderFetch(); if (!Fetcher.running) applyFilter() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = savedInstanceState?.getString("q") ?: ""
        filter = savedInstanceState?.getString("f")
        build()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("q", query); out.putString("f", filter)
    }

    override fun onResume() {
        super.onResume()
        Fetcher.addListener(fetchListener)
        reload()
        renderFetch()
    }

    override fun onPause() {
        Fetcher.removeListener(fetchListener)
        super.onPause()
    }

    private fun s(id: Int) = loc.getString(id)
    private fun dp(v: Float) = Ui.dp(this, v)

    private fun reload() {
        loader.execute {
            Store.load(this)
            val items = Catalog.load(this)
            runOnUiThread { if (!isFinishing) { all = items; applyFilter() } }
        }
    }

    fun categoryOf(item: AppItem): String = Store.get(this, item.pkg).category ?: item.systemCategory

    private fun build() {
        loc = Prefs.localized(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG); fitsSystemWindows = true
            setPadding(dp(16f), dp(16f), dp(16f), 0)
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(Ui.text(this, s(R.string.app_name), 26f, bold = true))
        titles.addView(Ui.text(this, s(R.string.tagline), 14f, Ui.DIM))
        head.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        val ja = Prefs.lang(this) == Prefs.LANG_JA
        head.addView(Ui.chip(this, if (ja) "English" else "日本語", false) {
            Prefs.setLang(this, if (ja) Prefs.LANG_EN else Prefs.LANG_JA); build()
        })
        col.addView(head)

        col.addView(Ui.text(this, s(R.string.internet_note), 12f, Ui.DIM).apply { setPadding(0, dp(8f), 0, dp(8f)) })

        val search = EditText(this).apply {
            hint = s(R.string.search_hint); setText(query); setSingleLine()
            setTextColor(Ui.LIGHT); setHintTextColor(Ui.DIM)
            background = Ui.round(Ui.CARD, dp(12f).toFloat())
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun afterTextChanged(e: Editable?) { query = e?.toString() ?: ""; applyFilter() }
            })
        }
        col.addView(search)

        chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10f), 0, dp(6f)) }
        col.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chipRow) })

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        countView = Ui.text(this, "", 13f, Ui.DIM)
        bar.addView(countView, LinearLayout.LayoutParams(0, -2, 1f))
        val recent = Prefs.sortRecent(this)
        bar.addView(Ui.chip(this, s(if (recent) R.string.sort_recent else R.string.sort_name), false) {
            Prefs.setSortRecent(this, !recent); build()
        })
        col.addView(bar)

        fetchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8f), 0, dp(8f)) }
        col.addView(fetchRow)

        val list = ListView(this).apply {
            adapter = this@MainActivity.adapter; divider = null; dividerHeight = 0
            setOnItemClickListener { _, _, pos, _ ->
                startActivity(Intent(this@MainActivity, DetailActivity::class.java).putExtra("pkg", shown[pos].pkg))
            }
        }
        col.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(col)
        renderFetch()
        applyFilter()
    }

    private fun renderFetch() {
        if (!::fetchRow.isInitialized) return
        fetchRow.removeAllViews()
        if (Fetcher.running) {
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = Fetcher.total.coerceAtLeast(1); progress = Fetcher.progress }
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            box.addView(Ui.text(this, String.format(loc.resources.configuration.locales[0], s(R.string.fetch_all_progress), Fetcher.progress, Fetcher.total), 13f))
            box.addView(pb)
            fetchRow.addView(box, LinearLayout.LayoutParams(0, -2, 1f))
            fetchRow.addView(Ui.button(this, s(R.string.stop), false) { Fetcher.cancelAll() })
        } else {
            fetchRow.addView(Ui.button(this, s(R.string.fetch_all), false) {
                val todo = all.filter { Store.get(this, it.pkg).fetchedAt == 0L }.ifEmpty { all }
                Fetcher.startAll(this, todo.map { it.pkg })
            })
            if (Fetcher.lastDone) fetchRow.addView(Ui.text(this,
                String.format(loc.resources.configuration.locales[0], s(R.string.fetch_all_done), Fetcher.found, Fetcher.missing), 12f, Ui.DIM)
                .apply { setPadding(dp(10f), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun applyFilter() {
        if (!::chipRow.isInitialized) return
        val cats = all.groupingBy { categoryOf(it) }.eachCount()
        chipRow.removeAllViews()
        fun add(key: String?, label: String) {
            chipRow.addView(Ui.chip(this, label, filter == key) { filter = key; applyFilter() },
                LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6f) })
        }
        add(null, "${s(R.string.all)} ${all.size}")
        val keys = Categories.DEFAULTS + Store.customCategories(this)
        for (k in keys) { val n = cats[k] ?: 0; if (n > 0 || k == filter) add(k, "${Prefs.catLabel(loc, k)} $n") }
        val recent = Prefs.sortRecent(this)
        shown = all.filter { item ->
            val e = Store.get(this, item.pkg)
            (filter == null || categoryOf(item) == filter) &&
                Text.matches(query, item.label, item.pkg, e.note, e.playDesc, Prefs.catLabel(loc, categoryOf(item)))
        }.let { l -> if (recent) l.sortedByDescending { it.updated } else l.sortedBy { it.label.lowercase() } }
        countView.text = if (shown.isEmpty() && all.isNotEmpty()) s(R.string.no_results)
            else String.format(loc.resources.configuration.locales[0], s(R.string.apps_count), shown.size)
        adapter.notifyDataSetChanged()
    }

    inner class Adapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(p: Int) = shown[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, convert: View?, parent: ViewGroup?): View {
            val row = (convert as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4f), dp(8f), dp(4f), dp(8f))
                addView(ImageView(context), LinearLayout.LayoutParams(dp(44f), dp(44f)).apply { marginEnd = dp(12f) })
                val t = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                t.addView(Ui.text(context, "", 16f, bold = true).apply { setSingleLine() })
                t.addView(Ui.text(context, "", 13f, Ui.DIM).apply { maxLines = 2 })
                addView(t, LinearLayout.LayoutParams(0, -2, 1f))
            }
            val item = shown[p]
            val e = Store.get(this@MainActivity, item.pkg)
            (row.getChildAt(0) as ImageView).setImageDrawable(Catalog.icon(this@MainActivity, item))
            val t = row.getChildAt(1) as LinearLayout
            (t.getChildAt(0) as TextView).text = item.label
            val extra = e.note.ifBlank { e.playDesc }.replace('\n', ' ')
            (t.getChildAt(1) as TextView).text = Prefs.catLabel(loc, categoryOf(item)) + if (extra.isNotBlank()) " · $extra" else ""
            return row
        }
    }
}
