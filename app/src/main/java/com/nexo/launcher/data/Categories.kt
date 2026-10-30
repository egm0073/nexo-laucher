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
    BANKS("Bancos"),
    SHOPPING("Compras y pagos"),
    AI("Inteligencia artificial"),
    NOTES("Notas y guardados"),
    PRODUCTIVITY("Productividad"),
    TOOLS("Herramientas y sistema"),
    OTHER("Otras"),
}

/**
 * Se buscan antes que la categoría declarada: ChatGPT o Keep se declaran como «productividad»
 * y WhatsApp o Telegram como «redes sociales», pero aquí la mensajería va en Comunicación.
 */
private val PRIORITY_KEYWORDS: List<Pair<AppCategory, List<String>>> = listOf(
    AppCategory.COMMUNICATION to listOf(
        "whatsapp", "telegram", "challegram", "signal", "securesms", "messag", "orca", "sms", "mms",
        "viber", "wechat", "tencent.mm", "line", "discord", "skype", "teams", "slack", "threema",
        "wire", "kakao", "beeper", "zalo", "imo", "botim",
    ),
    AppCategory.AI to listOf(
        "ai", "openai", "chatgpt", "anthropic", "claude", "bard", "copilot", "perplexity",
        "deepseek", "mistral", "grok", "poe", "character", "inflection", "replika", "huggingface",
        "qwen", "kimi", "moonshot",
    ),
    AppCategory.BANKS to listOf(
        "bank", "banco", "banca", "bancsabadell", "sabadell", "bbva", "santander", "openbank", "caixa",
        "ing", "revolut", "n26", "number26", "bankinter", "unicaja", "kutxabank", "ibercaja", "abanca",
        "evo", "evobanco", "imaginbank", "cajamar", "ruralvia", "cajarural", "deutschebank", "triodos",
        "myinvestor", "transferwise", "hsbc", "barclays", "citi", "wellsfargo", "bunq",
        "monzo", "selfbank", "pibank", "laboralkutxa", "cajasur",
    ),
    AppCategory.NOTES to listOf(
        "keep", "notes", "note", "evernote", "notion", "obsidian", "onenote", "instapaper", "readitlater",
        "raindrop", "omnivore", "getmatter", "readwise", "joplin", "standardnotes", "simplenote",
        "squid", "notesnook", "bear", "memo", "memos", "logseq", "anytype", "colornote",
    ),
)

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
        "wikipedia", "comic",
    ),
    AppCategory.MAPS to listOf(
        "maps", "waze", "ubercab", "cabify", "bolt", "booking", "airbnb", "renfe", "tripadvisor", "citymapper",
        "moovit", "transit", "navigation", "flight", "ryanair", "vueling", "iberia", "blablacar",
    ),
    AppCategory.SHOPPING to listOf(
        "amazon", "shop", "ebay", "aliexpress", "wallapop", "vinted", "shein", "temu", "zara", "lidl",
        "paypal", "bizum", "wallet", "pay", "glovo", "justeat", "deliveroo", "crypto", "binance", "coinbase",
    ),
    AppCategory.PRODUCTIVITY to listOf(
        "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "drive", "dropbox", "onedrive",
        "todo", "tasks", "calendar", "trello", "pdf", "translate",
    ),
    AppCategory.TOOLS to listOf(
        "settings", "clock", "deskclock", "calculator", "files", "filemanager", "documentsui", "weather",
        "vending", "store", "authenticator", "vpn", "security", "flashlight", "torch", "launcher",
        "chrome", "browser", "firefox", "opera", "brave", "duckduckgo", "googlequicksearchbox",
        "assistant", "health", "fit", "wear", "home", "smartthings", "backup", "battery", "cleaner",
    ),
)

fun autoCategory(info: ApplicationInfo?, packageName: String): AppCategory {
    // Se trocea el paquete ("com.google.android.apps.maps") para no confundir "ing" con "settings".
    val pkg = packageName.lowercase()
    val parts = pkg.split('.', '_')
    fun List<Pair<AppCategory, List<String>>>.match(): AppCategory? = firstOrNull { (_, words) ->
        words.any { w -> if (w.length <= 4) w in parts else pkg.contains(w) }
    }?.first

    PRIORITY_KEYWORDS.match()?.let { return it }
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
    KEYWORDS.match()?.let { return it }
    if (info != null && info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return AppCategory.TOOLS
    return AppCategory.OTHER
}
