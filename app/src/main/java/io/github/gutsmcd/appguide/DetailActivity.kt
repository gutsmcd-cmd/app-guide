package io.github.gutsmcd.appguide

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.util.Date

/** One app: icon, name, package, category, your note, and optional Google Play info. */
class DetailActivity : Activity() {
    private lateinit var loc: Context
    private lateinit var pkg: String
    private var item: AppItem? = null
    private var fetching = false
    private var noteDraft: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra("pkg") ?: return finish()
        noteDraft = savedInstanceState?.getString("note")
        item = Catalog.load(this).firstOrNull { it.pkg == pkg }
        if (item == null) return finish()
        render()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        noteDraft?.let { out.putString("note", it) }
    }

    private fun s(id: Int) = loc.getString(id)
    private fun dp(v: Float) = Ui.dp(this, v)
    private fun date(t: Long) = DateFormat.getMediumDateFormat(loc).format(Date(t))

    private fun render() {
        loc = Prefs.localized(this)
        val it = item ?: return
        val e = Store.get(this, pkg)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(24f), dp(20f), dp(28f)) }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(this).apply { setImageDrawable(Catalog.icon(this@DetailActivity, it)) },
            LinearLayout.LayoutParams(dp(64f), dp(64f)).apply { marginEnd = dp(14f) })
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        names.addView(Ui.text(this, it.label, 22f, bold = true))
        names.addView(Ui.text(this, it.pkg, 12f, Ui.DIM).apply { setTextIsSelectable(true) })
        head.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(head)

        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14f), 0, dp(6f)) }
        Catalog.launchIntent(packageManager, pkg)?.let { li ->
            btns.addView(Ui.button(this, s(R.string.open_app), true) { runCatching { startActivity(li) } },
                LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8f) })
        }
        btns.addView(Ui.button(this, s(R.string.app_info), false) {
            runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(btns)

        fun label(id: Int) = Ui.text(this, s(id), 13f, Ui.DIM).apply { setPadding(0, dp(14f), 0, dp(4f)) }

        col.addView(label(R.string.category))
        val cat = e.category ?: it.systemCategory
        col.addView(Ui.chip(this, Prefs.catLabel(loc, cat) + "  ▾", true) { pickCategory() }, LinearLayout.LayoutParams(-2, -2))

        col.addView(Ui.text(this, "${s(R.string.installed)}: ${date(it.installed)}   ${s(R.string.updated)}: ${date(it.updated)}\n${s(R.string.size)}: ${Text.size(it.sizeBytes)}", 13f, Ui.DIM)
            .apply { setPadding(0, dp(12f), 0, 0) })

        col.addView(label(R.string.note))
        val note = EditText(this).apply {
            setText(noteDraft ?: e.note); hint = s(R.string.note_hint); minLines = 3; gravity = Gravity.TOP
            setTextColor(Ui.LIGHT); setHintTextColor(Ui.DIM)
            background = Ui.round(Ui.CARD, dp(12f).toFloat()); setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
        }
        col.addView(note)
        col.addView(Ui.button(this, s(R.string.save), true) {
            val cur = Store.get(this, pkg); cur.note = note.text.toString().trim(); Store.put(this, pkg, cur)
            noteDraft = null
            Toast.makeText(this, s(R.string.saved), Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8f) })

        // Google Play section
        col.addView(label(R.string.play_desc))
        if (e.playDesc.isNotBlank()) col.addView(Ui.text(this, e.playDesc, 15f).apply { setTextIsSelectable(true) })
        if (e.notOnPlay) col.addView(Ui.text(this, s(R.string.not_on_play), 13f, Ui.DIM))
        if (e.fetchedAt > 0 && !e.notOnPlay) col.addView(Ui.text(this, String.format(s(R.string.fetched_on), date(e.fetchedAt)), 12f, Ui.DIM))
        if (e.shots > 0) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10f), 0, 0) }
            for (i in 0 until e.shots) {
                val f = Store.shotFile(this, pkg, i)
                val bmp = runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 2 }) }.getOrNull() ?: continue
                row.addView(ImageView(this).apply { setImageBitmap(bmp); adjustViewBounds = true },
                    LinearLayout.LayoutParams(-2, dp(260f)).apply { marginEnd = dp(8f) })
            }
            col.addView(HorizontalScrollView(this).apply { addView(row) })
        }
        col.addView(Ui.button(this, s(if (fetching) R.string.fetching else if (e.fetchedAt > 0) R.string.refetch_info else R.string.fetch_info), false) {
            if (fetching) return@button
            noteDraft = note.text.toString()
            fetching = true; render()
            Fetcher.fetchOne(this, pkg) { r ->
                fetching = false
                if (r == Fetcher.Result.ERROR) Toast.makeText(this, s(R.string.offline), Toast.LENGTH_LONG).show()
                if (!isFinishing) render()
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10f) })
        col.addView(Ui.text(this, s(R.string.internet_note), 12f, Ui.DIM).apply { setPadding(0, dp(10f), 0, 0) })

        note.setOnFocusChangeListener { _, _ -> noteDraft = note.text.toString() }
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); fitsSystemWindows = true; addView(col) })
    }

    private fun pickCategory() {
        val keys = Categories.DEFAULTS + Store.customCategories(this)
        val labels = keys.map { Prefs.catLabel(loc, it) } + s(R.string.new_category)
        AlertDialog.Builder(this).setTitle(s(R.string.category)).setItems(labels.toTypedArray()) { _, which ->
            if (which < keys.size) setCategory(keys[which]) else newCategory()
        }.show()
    }

    private fun newCategory() {
        val input = EditText(this).apply { setSingleLine() }
        AlertDialog.Builder(this).setTitle(s(R.string.new_category_title)).setView(input)
            .setPositiveButton(s(R.string.ok)) { _, _ ->
                val name = Text.cleanCategory(input.text.toString())
                if (name.isNotEmpty()) { Store.addCategory(this, name); setCategory(name) }
            }.setNegativeButton(s(R.string.cancel), null).show()
    }

    private fun setCategory(key: String) {
        val e = Store.get(this, pkg)
        e.category = if (key == item?.systemCategory) null else key
        Store.put(this, pkg, e)
        render()
    }
}
