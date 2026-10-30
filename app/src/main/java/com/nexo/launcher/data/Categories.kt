package com.nexo.launcher.data

import android.content.pm.ApplicationInfo

/** Categorías del cajón, en el orden en que se muestran. */
enum class AppCategory(val label: String) {
    COMMUNICATION("Comunicación"),
    SOCIAL("Redes sociales"),
    PHOTO("Fotos y cámara"),
    MUSIC("Música y audio"),
    VIDEO("Vídeo"),
    GAMES("Juegos"),
    NEWS("Noticias y lectura"),
    MAPS("Mapas y viajes"),
    SHOPPING("Compras y finanzas"),
    PRODUCTIVITY("Productividad"),
    TOOLS("Herramientas y sistema"),
    OTHER("Otras"),
}

/** Qué apps aparecen primero en cada categoría. */
enum class DrawerRecent(val label: String) {
    USED("Usadas recientemente"),
    INSTALLED("Instaladas recientemente"),
}

/**
 * Muchas apps (sobre todo las del sistema) no declaran categoría, así que se completa con
 * palabras clave del nombre del paquete. Se comprueban en orden: la primera que coincide gana.
 */
private val KEYWORDS: List<Pair<AppCategory, List<String>>> = listOf(
    AppCategory.SOCIAL to listOf(
        "facebook", "instagram", "twitter", "tiktok", "musically", "snapchat", "reddit", "linkedin",
        "pinterest", "tumblr", "mastodon", "threads", "bereal", "zhiliaoapp",
    ),
    AppCategory.COMMUNICATION to listOf(
        "whatsapp", "telegram", "signal", "messag", "mms", "sms", "dialer", "phone", "contacts", "gm",
        "mail", "outlook", "skype", "teams", "zoom", "discord", "viber", "wechat", "line", "meet", "duo",
        "slack", "thunderbird", "incallui", "telecom",
    ),
    AppCategory.PHOTO to listOf("camera", "photo", "gallery", "snapseed", "lightroom", "picsart", "vsco", "scanner"),
    AppCategory.MUSIC to listOf(
        "spotify", "music", "deezer", "soundcloud", "tidal", "podcast", "audible", "shazam", "radio",
        "ivoox", "audio", "recorder",
    ),
    AppCategory.VIDEO to listOf(
        "youtube", "netflix", "primevideo", "avod", "disney", "hbo", "max", "twitch", "vlc", "video",
        "player", "movistar", "atresplayer", "rtve", "plex", "crunchyroll", "filmin", "skyshowtime",
    ),
    AppCategory.NEWS to listOf(
        "news", "noticias", "kindle", "book", "reader", "feedly", "medium", "elpais", "elmundo",
        "pocket", "wikipedia", "comic",
    ),
    AppCategory.MAPS to listOf(
        "maps", "waze", "ubercab", "cabify", "bolt", "booking", "airbnb", "renfe", "tripadvisor", "citymapper",
        "moovit", "transit", "navigation", "flight", "ryanair", "vueling", "iberia", "blablacar",
    ),
    AppCategory.SHOPPING to listOf(
        "amazon", "shop", "ebay", "aliexpress", "wallapop", "vinted", "shein", "temu", "zara", "lidl",
        "bank", "banco", "bbva", "santander", "caixa", "ing", "revolut", "paypal", "bizum", "wallet",
        "pay", "glovo", "justeat", "deliveroo", "crypto", "binance", "coinbase",
    ),
    AppCategory.PRODUCTIVITY to listOf(
        "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "drive", "dropbox", "onedrive",
        "notion", "evernote", "keep", "notes", "todo", "tasks", "calendar", "trello", "pdf", "translate",
        "obsidian", "onenote",
    ),
    AppCategory.TOOLS to listOf(
        "settings", "clock", "deskclock", "calculator", "files", "filemanager", "documentsui", "weather",
        "vending", "store", "authenticator", "vpn", "security", "flashlight", "torch", "launcher",
        "chrome", "browser", "firefox", "opera", "brave", "duckduckgo", "googlequicksearchbox",
        "assistant", "health", "fit", "wear", "home", "smartthings", "backup", "battery", "cleaner",
    ),
)

fun autoCategory(info: ApplicationInfo?, packageName: String): AppCategory {
    if (info != null) {
        @Suppress("DEPRECATION")
        if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) return AppCategory.GAMES
        when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.MUSIC
            ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.PHOTO
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> return AppCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> return AppCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> return AppCategory.TOOLS
        }
    }
    // Se trocea el paquete ("com.google.android.apps.maps") para no confundir "ing" con "settings".
    val pkg = packageName.lowercase()
    val parts = pkg.split('.', '_')
    for ((category, words) in KEYWORDS) {
        if (words.any { w -> if (w.length <= 4) w in parts else pkg.contains(w) }) return category
    }
    if (info != null && info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return AppCategory.TOOLS
    return AppCategory.OTHER
}
