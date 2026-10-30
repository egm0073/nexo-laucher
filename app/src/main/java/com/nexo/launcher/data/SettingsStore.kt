package com.nexo.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class SettingsStore(private val context: Context) {

    private object K {
        val homeIcon = stringPreferencesKey("home_icon_style")
        val drawerIcon = stringPreferencesKey("drawer_icon_style")
        val iconSize = intPreferencesKey("icon_size")
        val textSize = intPreferencesKey("text_size")
        val font = stringPreferencesKey("font")
        val align = stringPreferencesKey("align")
        val accent = longPreferencesKey("accent")
        val lightText = booleanPreferencesKey("light_text")
        val dim = floatPreferencesKey("dim")
        val showClock = booleanPreferencesKey("show_clock")
        val clock24 = booleanPreferencesKey("clock_24")
        val showDate = booleanPreferencesKey("show_date")
        val showBattery = booleanPreferencesKey("show_battery")
        val showAlarm = booleanPreferencesKey("show_alarm")
        val swipeDown = booleanPreferencesKey("swipe_down_notif")
        val autoKeyboard = booleanPreferencesKey("drawer_auto_keyboard")
        val homeItems = stringPreferencesKey("home_items")
        val hidden = stringSetPreferencesKey("hidden")
        val folders = stringPreferencesKey("folders")
        val widgets = stringPreferencesKey("widgets")
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { read(it) }

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.dataStore.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun read(p: Preferences): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            homeIconStyle = enumOr(p[K.homeIcon], d.homeIconStyle),
            drawerIconStyle = enumOr(p[K.drawerIcon], d.drawerIconStyle),
            iconSizeDp = p[K.iconSize] ?: d.iconSizeDp,
            textSizeSp = p[K.textSize] ?: d.textSizeSp,
            font = enumOr(p[K.font], d.font),
            align = enumOr(p[K.align], d.align),
            accent = p[K.accent] ?: d.accent,
            lightText = p[K.lightText] ?: d.lightText,
            dim = p[K.dim] ?: d.dim,
            showClock = p[K.showClock] ?: d.showClock,
            clock24 = p[K.clock24] ?: d.clock24,
            showDate = p[K.showDate] ?: d.showDate,
            showBattery = p[K.showBattery] ?: d.showBattery,
            showNextAlarm = p[K.showAlarm] ?: d.showNextAlarm,
            swipeDownNotifications = p[K.swipeDown] ?: d.swipeDownNotifications,
            drawerAutoKeyboard = p[K.autoKeyboard] ?: d.drawerAutoKeyboard,
            homeItems = p[K.homeItems]?.let(::decodeList) ?: emptyList(),
            hidden = p[K.hidden] ?: emptySet(),
            folders = p[K.folders]?.let(::decodeFolders) ?: emptyList(),
            widgets = p[K.widgets]?.let(::decodeWidgets) ?: emptyList(),
            loaded = true,
        )
    }

    private fun write(p: MutablePreferences, s: LauncherSettings) {
        p[K.homeIcon] = s.homeIconStyle.name
        p[K.drawerIcon] = s.drawerIconStyle.name
        p[K.iconSize] = s.iconSizeDp
        p[K.textSize] = s.textSizeSp
        p[K.font] = s.font.name
        p[K.align] = s.align.name
        p[K.accent] = s.accent
        p[K.lightText] = s.lightText
        p[K.dim] = s.dim
        p[K.showClock] = s.showClock
        p[K.clock24] = s.clock24
        p[K.showDate] = s.showDate
        p[K.showBattery] = s.showBattery
        p[K.showAlarm] = s.showNextAlarm
        p[K.swipeDown] = s.swipeDownNotifications
        p[K.autoKeyboard] = s.drawerAutoKeyboard
        p[K.homeItems] = JSONArray(s.homeItems).toString()
        p[K.hidden] = s.hidden
        p[K.folders] = encodeFolders(s.folders)
        p[K.widgets] = encodeWidgets(s.widgets)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun decodeList(s: String): List<String> = runCatching {
        val a = JSONArray(s)
        List(a.length()) { a.getString(it) }
    }.getOrDefault(emptyList())

    private fun encodeFolders(list: List<Folder>): String {
        val a = JSONArray()
        list.forEach { f ->
            a.put(JSONObject().put("id", f.id).put("name", f.name).put("apps", JSONArray(f.apps)))
        }
        return a.toString()
    }

    private fun decodeFolders(s: String): List<Folder> = runCatching {
        val a = JSONArray(s)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val apps = o.getJSONArray("apps")
            Folder(o.getString("id"), o.getString("name"), List(apps.length()) { apps.getString(it) })
        }
    }.getOrDefault(emptyList())

    private fun encodeWidgets(list: List<WidgetEntry>): String {
        val a = JSONArray()
        list.forEach { w ->
            a.put(
                JSONObject().put("id", w.id).put("h", w.heightDp)
                    .put("p", w.page).put("x", w.x).put("y", w.y).put("w", w.widthDp),
            )
        }
        return a.toString()
    }

    private fun decodeWidgets(s: String): List<WidgetEntry> = runCatching {
        val a = JSONArray(s)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            WidgetEntry(o.getInt("id"), o.getInt("h"), o.optInt("p"), o.optInt("x"), o.optInt("y"), o.optInt("w"))
        }
    }.getOrDefault(emptyList())
}
