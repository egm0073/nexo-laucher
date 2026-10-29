package com.nexo.launcher.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import java.text.Normalizer

private val MARKS = Regex("\\p{Mn}+")

fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(MARKS, "").lowercase()

fun sectionOf(label: String): Char {
    val c = label.normalized().firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

fun Context.safeStart(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

fun openAlarms(ctx: Context) {
    ctx.safeStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))
}

fun openCalendar(ctx: Context) {
    val ok = ctx.safeStart(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
    if (!ok) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        ctx.safeStart(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun webSearch(ctx: Context, query: String) {
    val ok = ctx.safeStart(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))
    if (!ok) ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
}
