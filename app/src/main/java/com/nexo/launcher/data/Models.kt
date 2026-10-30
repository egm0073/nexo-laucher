package com.nexo.launcher.data

enum class IconStyle(val label: String) {
    NONE("Sin iconos"),
    COLOR("Color"),
    GRAYSCALE("Grises"),
    SILHOUETTE("Silueta"),
}

enum class SilhouetteColor(val label: String) {
    BLACK("Negro"),
    WHITE("Blanco"),
    TEXT("Como el texto"),
    ACCENT("Acento"),
}

enum class FontChoice(val label: String) {
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Mono"),
    CURSIVE("Manuscrita"),
    INTER("Inter"),
    LATO("Lato"),
    MANROPE("Manrope"),
    NUNITO_SANS("Nunito Sans"),
    WORK_SANS("Work Sans"),
    MONTSERRAT("Montserrat"),
    RALEWAY("Raleway"),
    JOST("Jost"),
    QUICKSAND("Quicksand"),
}

enum class AlignChoice(val label: String) {
    START("Izquierda"),
    CENTER("Centro"),
    END("Derecha"),
}

data class Folder(val id: String, val name: String, val apps: List<String>)

/**
 * page 0: lista bajo el reloj (solo cuenta la altura).
 * page 1: segunda pantalla, con posición y tamaño libres en dp (widthDp 0 = ancho completo).
 */
data class WidgetEntry(
    val id: Int,
    val heightDp: Int,
    val page: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val widthDp: Int = 0,
)

/** Elementos de la pantalla principal: "app:<clave>" o "folder:<id>". */
object HomeItem {
    fun app(key: String) = "app:$key"
    fun folder(id: String) = "folder:$id"
}

data class LauncherSettings(
    val homeIconStyle: IconStyle = IconStyle.NONE,
    val drawerIconStyle: IconStyle = IconStyle.NONE,
    val iconSizeDp: Int = 28,
    val silhouetteColor: SilhouetteColor = SilhouetteColor.BLACK,
    val textSizeSp: Int = 26,
    val font: FontChoice = FontChoice.SANS,
    val align: AlignChoice = AlignChoice.START,
    val accent: Long = 0xFF000000,
    val lightText: Boolean = true,
    val dim: Float = 0.25f,
    val showClock: Boolean = true,
    val clock24: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showNextAlarm: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    val drawerAutoKeyboard: Boolean = false,
    val drawerByCategory: Boolean = true,
    /** Categoría elegida a mano: clave de app → [AppCategory]. */
    val categoryOverrides: Map<String, AppCategory> = emptyMap(),
    /** Última vez que se abrió cada app desde el launcher: clave → milisegundos. */
    val lastUsed: Map<String, Long> = emptyMap(),
    val homeItems: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val folders: List<Folder> = emptyList(),
    val widgets: List<WidgetEntry> = emptyList(),
    val loaded: Boolean = false,
)
