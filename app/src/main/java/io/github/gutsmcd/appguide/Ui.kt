package io.github.gutsmcd.appguide

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

/** Shared colors and tiny view builders (no AndroidX). */
object Ui {
    const val BG = 0xFF0E1110.toInt()
    const val CARD = 0xFF1A1F1D.toInt()
    const val ACCENT = 0xFF2BB673.toInt()
    const val LIGHT = 0xFFE8ECEA.toInt()
    const val DIM = 0xFF9AA5A0.toInt()

    fun dp(c: Context, v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.resources.displayMetrics).toInt()

    fun text(c: Context, s: CharSequence, sp: Float, color: Int = LIGHT, bold: Boolean = false) = TextView(c).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    fun round(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    fun button(c: Context, s: String, primary: Boolean, onClick: () -> Unit) = Button(c).apply {
        text = s; isAllCaps = false; setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTextColor(if (primary) Color.BLACK else LIGHT)
        background = round(if (primary) ACCENT else CARD, dp(c, 22f).toFloat(), if (primary) 0 else dp(c, 1f), 0xFF3A4440.toInt())
        minHeight = dp(c, 44f); setPadding(dp(c, 16f), 0, dp(c, 16f), 0)
        setOnClickListener { onClick() }
    }

    fun chip(c: Context, s: String, on: Boolean, onClick: () -> Unit) = TextView(c).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(if (on) Color.BLACK else LIGHT)
        background = round(if (on) ACCENT else CARD, dp(c, 16f).toFloat())
        setPadding(dp(c, 14f), dp(c, 8f), dp(c, 14f), dp(c, 8f))
        setOnClickListener { onClick() }
    }
}
