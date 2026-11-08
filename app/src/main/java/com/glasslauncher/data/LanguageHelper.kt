package com.glasslauncher.data

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SupportedLanguage(val tag: String, val displayName: String, val nativeName: String)

object LanguageHelper {
    private const val PREFS_NAME = "glass_language_prefs"
    private const val KEY_LANG = "selected_language"

    val languages = listOf(
        SupportedLanguage("default", "System Default", "Default"),
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("hi", "Hindi", "हिन्दी"),
        SupportedLanguage("es", "Spanish", "Español"),
        SupportedLanguage("fr", "French", "Français"),
        SupportedLanguage("de", "German", "Deutsch"),
        SupportedLanguage("pt", "Portuguese", "Português"),
        SupportedLanguage("ja", "Japanese", "日本語"),
        SupportedLanguage("ru", "Russian", "Русский"),
        SupportedLanguage("ar", "Arabic", "العربية"),
        SupportedLanguage("zh", "Chinese", "中文"),
    )

    private val _currentLanguage = MutableStateFlow("default")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun resolveSystemLanguage(): String {
        val sys = Locale.getDefault().language.lowercase()
        val dict = DICTIONARY ?: return "en"
        return if (dict.containsKey(sys)) sys else "en"
    }

    fun init(context: Context) {
        val saved = getSelectedLanguage(context)
        if (saved == "default") {
            _currentLanguage.value = resolveSystemLanguage()
        } else {
            _currentLanguage.value = saved
        }
    }

    fun getSelectedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "default") ?: "default"
    }

    fun getEffectiveLanguage(context: Context): String {
        val saved = getSelectedLanguage(context)
        if (saved != "default" && DICTIONARY.containsKey(saved)) return saved
        return resolveSystemLanguage()
    }

    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, tag)
            .apply()

        val effective = if (tag == "default") resolveSystemLanguage() else tag
        _currentLanguage.value = effective

        val locale = Locale.forLanguageTag(effective)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (tag == "default") {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(tag)
            }
        }
    }

    fun wrap(context: Context): Context {
        val tag = getEffectiveLanguage(context)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    /** Resolves active language code: either explicit code or device default code */
    fun activeLanguageCode(): String {
        val tag = _currentLanguage.value
        if (tag != "default" && DICTIONARY?.containsKey(tag) == true) return tag
        return resolveSystemLanguage()
    }

    /** Translate key using currently active language flow */
    fun t(key: String, lang: String? = null): String {
        val target = lang ?: activeLanguageCode()
        val map = DICTIONARY?.get(target) ?: DICTIONARY?.get("en")
        return map?.get(key) ?: DICTIONARY?.get("en")?.get(key) ?: key
    }

    /** Composable translation helper reacting to language updates in real time */
    @Composable
    fun string(key: String): String {
        val lang by currentLanguage.collectAsState()
        val target = if (lang != "default" && DICTIONARY?.containsKey(lang) == true) lang else resolveSystemLanguage()
        return t(key, target)
    }

    private val DICTIONARY: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            "settings" to "Settings",
            "home_screen" to "Home Screen",
            "finish_setup" to "Finish Setting Up",
            "glass" to "Glass",
            "look" to "Look",
            "widgets_panels" to "Widgets & Panels",
            "dock_search" to "Dock & Search",
            "app_icons" to "App Icons",
            "style" to "Style",
            "icon_pack" to "Icon Pack",
            "app_icon_size" to "App Icon Size",
            "home_screen_layout" to "Home Screen Layout",
            "grid_auto" to "Auto (Adaptive)",
            "grid_4x6" to "4 × 6 (Standard)",
            "grid_4x7" to "4 × 7 (Tall Screen)",
            "show_app_names" to "Show App Names",
            "app_name_size" to "App Name Size",
            "arrange_auto" to "Arrange Icons Automatically",
            "control_centre" to "Control Centre & Overlay",
            "control_centre_overlay" to "Control Centre in All Apps (Overlay)",
            "notification_badges" to "Notification Badges & Centre",
            "widgets" to "Widgets",
            "choose_photos" to "Choose Featured Photos",
            "weather_location" to "Weather Location",
            "device_gps" to "Device GPS",
            "language" to "Language",
            "about_glass" to "About Glass OS",
            "feature_tour" to "Feature Tour",
            "welcome" to "Welcome to Glass OS",
            "choose_language" to "Choose your preferred language",
            "continue_btn" to "Continue",
            "done" to "Done",
            "search" to "Search",
            "edit" to "Edit",
            "folder" to "Folder",
            "app_library" to "App Library",
            "set_default_home_title" to "Set Default Launcher",
            "set_default_home_desc" to "Set Glass OS as your default launcher so your phone opens directly to your custom home screen, widgets, and dock.",
            "default_home_set_desc" to "Glass OS is your default launcher! You will return here whenever you press Home or swipe up.",
            "set_default_home_btn" to "Set as Default Launcher",
            "default_home_set" to "Default Launcher Set",
            "continue_tour_btn" to "Continue to Tour",
            "skip_for_now" to "Skip for Now",
            "not_default_prompt_desc" to "Glass OS is not your default home app. Set it as default so your phone opens here whenever you swipe up or press Home.",
            "maybe_later" to "Maybe Later",
        ),
        "hi" to mapOf(
            "settings" to "सेटिंग्स",
            "home_screen" to "होम स्क्रीन",
            "finish_setup" to "सेटअप पूरा करें",
            "glass" to "ग्लास",
            "look" to "रूप",
            "widgets_panels" to "विजेट्स और पैनल",
            "dock_search" to "डॉक और खोज",
            "app_icons" to "ऐप आइकन",
            "style" to "शैली",
            "icon_pack" to "आइकन पैक",
            "app_icon_size" to "ऐप आइकन का आकार",
            "home_screen_layout" to "होम स्क्रीन लेआउट",
            "grid_auto" to "ऑटो (अनुकूली)",
            "grid_4x6" to "4 × 6 (मानक)",
            "grid_4x7" to "4 × 7 (लंबी स्क्रीन)",
            "show_app_names" to "ऐप के नाम दिखाएं",
            "app_name_size" to "ऐप नाम का आकार",
            "arrange_auto" to "आइकन स्वतः व्यवस्थित करें",
            "control_centre" to "नियंत्रण केंद्र और ओवरले",
            "control_centre_overlay" to "सभी ऐप्स में नियंत्रण केंद्र (ओवरले)",
            "notification_badges" to "सूचना बैज और केंद्र",
            "widgets" to "विजेट्स",
            "choose_photos" to "चुनिंदा फ़ोटो चुनें",
            "weather_location" to "मौसम स्थान",
            "device_gps" to "डिवाइस जीपीएस",
            "language" to "भाषा",
            "about_glass" to "ग्लास ओएस के बारे में",
            "feature_tour" to "फीचर टूर",
            "welcome" to "ग्लास ओएस में आपका स्वागत है",
            "choose_language" to "अपनी पसंदीदा भाषा चुनें",
            "continue_btn" to "जारी रखें",
            "done" to "संपन्न",
            "search" to "खोजें",
            "edit" to "संपादित करें",
            "folder" to "फ़ोल्डर",
            "app_library" to "ऐप लाइब्रेरी",
            "set_default_home_title" to "डिफ़ॉल्ट लॉन्चर सेट करें",
            "set_default_home_desc" to "ग्लास ओएस को अपना डिफ़ॉल्ट लॉन्चर बनाएं ताकि आपका फ़ोन सीधे आपकी होम स्क्रीन, विजेट्स और डॉक पर खुले।",
            "default_home_set_desc" to "ग्लास ओएस आपका डिफ़ॉल्ट लॉन्चर है! होम बटन दबाने या ऊपर स्वाइप करने पर आप यहीं वापस आएंगे।",
            "set_default_home_btn" to "डिफ़ॉल्ट लॉन्चर बनाएं",
            "default_home_set" to "डिफ़ॉल्ट लॉन्चर सेट है",
            "continue_tour_btn" to "टूर जारी रखें",
            "skip_for_now" to "अभी के लिए छोड़ें",
            "not_default_prompt_desc" to "ग्लास ओएस आपका डिफ़ॉल्ट होम ऐप नहीं है। इसे डिफ़ॉल्ट बनाएं ताकि स्वाइप करने या होम दबाने पर यह खुले।",
            "maybe_later" to "बाद में",
        ),
        "es" to mapOf(
            "settings" to "Ajustes",
            "home_screen" to "Pantalla de inicio",
            "finish_setup" to "Finalizar configuración",
            "glass" to "Cristal",
            "look" to "Aspecto",
            "widgets_panels" to "Widgets y paneles",
            "dock_search" to "Dock y búsqueda",
            "app_icons" to "Iconos de apps",
            "style" to "Estilo",
            "icon_pack" to "Paquete de iconos",
            "app_icon_size" to "Tamaño de iconos",
            "home_screen_layout" to "Disposición de pantalla",
            "grid_auto" to "Auto (Adaptable)",
            "grid_4x6" to "4 × 6 (Estándar)",
            "grid_4x7" to "4 × 7 (Pantalla alta)",
            "show_app_names" to "Mostrar nombres de apps",
            "app_name_size" to "Tamaño de nombres",
            "arrange_auto" to "Organizar iconos automáticamente",
            "control_centre" to "Centro de control y superposición",
            "control_centre_overlay" to "Centro de control en todas las apps",
            "notification_badges" to "Globos de notificación y centro",
            "widgets" to "Widgets",
            "choose_photos" to "Elegir fotos destacadas",
            "weather_location" to "Ubicación del tiempo",
            "device_gps" to "GPS del dispositivo",
            "language" to "Idioma",
            "about_glass" to "Acerca de Glass OS",
            "feature_tour" to "Recorrido de funciones",
            "welcome" to "Bienvenido a Glass OS",
            "choose_language" to "Elige tu idioma preferido",
            "continue_btn" to "Continuar",
            "done" to "Listo",
            "search" to "Buscar",
            "edit" to "Editar",
            "folder" to "Carpeta",
            "app_library" to "Biblioteca de apps",
            "set_default_home_title" to "Establecer lanzador predeterminado",
            "set_default_home_desc" to "Establece Glass OS como tu lanzador predeterminado para abrir directamente tu pantalla de inicio, widgets y dock.",
            "default_home_set_desc" to "¡Glass OS es tu lanzador predeterminado! Volverás aquí cada vez que presiones Inicio o deslices hacia arriba.",
            "set_default_home_btn" to "Establecer como predeterminado",
            "default_home_set" to "Lanzador predeterminado activo",
            "continue_tour_btn" to "Continuar al recorrido",
            "skip_for_now" to "Omitir por ahora",
            "not_default_prompt_desc" to "Glass OS no es tu app de inicio predeterminada. Configúrala como predeterminada para usarla al presionar Inicio.",
            "maybe_later" to "Quizás más tarde",
        ),
        "fr" to mapOf(
            "settings" to "Paramètres",
            "home_screen" to "Écran d'accueil",
            "finish_setup" to "Terminer la configuration",
            "glass" to "Verre",
            "look" to "Aspect",
            "widgets_panels" to "Widgets et panneaux",
            "dock_search" to "Dock et recherche",
            "app_icons" to "Icônes d'applis",
            "style" to "Style",
            "icon_pack" to "Pack d'icônes",
            "app_icon_size" to "Taille des icônes",
            "home_screen_layout" to "Disposition de l'écran",
            "grid_auto" to "Auto (Adaptatif)",
            "grid_4x6" to "4 × 6 (Standard)",
            "grid_4x7" to "4 × 7 (Grand écran)",
            "show_app_names" to "Afficher les noms d'applis",
            "app_name_size" to "Taille des noms",
            "arrange_auto" to "Réorganiser automatiquement",
            "control_centre" to "Centre de contrôle et volet",
            "control_centre_overlay" to "Centre de contrôle partout (volet)",
            "notification_badges" to "Pastilles et centre de notifications",
            "widgets" to "Widgets",
            "choose_photos" to "Choisir les photos à la une",
            "weather_location" to "Lieu météo",
            "device_gps" to "GPS de l'appareil",
            "language" to "Langue",
            "about_glass" to "À propos de Glass OS",
            "feature_tour" to "Visite guidée",
            "welcome" to "Bienvenue sur Glass OS",
            "choose_language" to "Choisissez votre langue",
            "continue_btn" to "Continuer",
            "done" to "Terminé",
            "search" to "Recherche",
            "edit" to "Modifier",
            "folder" to "Dossier",
            "app_library" to "Bibliothèque d'apps",
            "set_default_home_title" to "Définir comme lanceur par défaut",
            "set_default_home_desc" to "Définissez Glass OS comme lanceur par défaut pour accéder directement à vos widgets et votre écran d'accueil.",
            "default_home_set_desc" to "Glass OS est votre lanceur par défaut ! Vous reviendrez ici chaque fois que vous appuierez sur Accueil.",
            "set_default_home_btn" to "Définir par défaut",
            "default_home_set" to "Lanceur par défaut actif",
            "continue_tour_btn" to "Continuer la visite",
            "skip_for_now" to "Ignorer pour l'instant",
            "not_default_prompt_desc" to "Glass OS n'est pas votre application d'accueil par défaut. Définissez-la comme lanceur par défaut.",
            "maybe_later" to "Plus tard",
        ),
        "de" to mapOf(
            "settings" to "Einstellungen",
            "home_screen" to "Startbildschirm",
            "finish_setup" to "Einrichtung abschließen",
            "glass" to "Glas",
            "look" to "Aussehen",
            "widgets_panels" to "Widgets & Paneele",
            "dock_search" to "Dock & Suche",
            "app_icons" to "App-Symbole",
            "style" to "Stil",
            "icon_pack" to "Symbol-Paket",
            "app_icon_size" to "Symbolgröße",
            "home_screen_layout" to "Startbildschirm-Layout",
            "grid_auto" to "Auto (Adaptiv)",
            "grid_4x6" to "4 × 6 (Standard)",
            "grid_4x7" to "4 × 7 (Großer Bildschirm)",
            "show_app_names" to "App-Namen anzeigen",
            "app_name_size" to "Name-Größe",
            "arrange_auto" to "Symbole automatisch anordnen",
            "control_centre" to "Kontrollzentrum & Overlay",
            "control_centre_overlay" to "Kontrollzentrum in allen Apps (Overlay)",
            "notification_badges" to "Benachrichtigungen & Badges",
            "widgets" to "Widgets",
            "choose_photos" to "Fotos auswählen",
            "weather_location" to "Wetter-Standort",
            "device_gps" to "Geräte-GPS",
            "language" to "Sprache",
            "about_glass" to "Über Glass OS",
            "feature_tour" to "Funktionstour",
            "welcome" to "Willkommen bei Glass OS",
            "choose_language" to "Wählen Sie Ihre Sprache",
            "continue_btn" to "Weiter",
            "done" to "Fertig",
            "search" to "Suchen",
            "edit" to "Bearbeiten",
            "folder" to "Ordner",
            "app_library" to "App-Mediathek",
            "set_default_home_title" to "Standard-Launcher festlegen",
            "set_default_home_desc" to "Legen Sie Glass OS als Standard-Launcher fest, damit Ihr Startbildschirm mit Widgets und Dock direkt öffnet.",
            "default_home_set_desc" to "Glass OS ist Ihr Standard-Launcher! Sie kehren hierher zurück, wenn Sie auf Start drücken.",
            "set_default_home_btn" to "Als Standard festlegen",
            "default_home_set" to "Standard-Launcher aktiv",
            "continue_tour_btn" to "Weiter zur Tour",
            "skip_for_now" to "Vorerst überspringen",
            "not_default_prompt_desc" to "Glass OS ist nicht Ihr Standard-Startbildschirm. Legen Sie es als Standard fest, um es immer zu nutzen.",
            "maybe_later" to "Vielleicht später",
        ),
        "pt" to mapOf(
            "settings" to "Configurações",
            "home_screen" to "Tela de início",
            "finish_setup" to "Concluir configuração",
            "glass" to "Vidro",
            "look" to "Visual",
            "widgets_panels" to "Widgets e painéis",
            "dock_search" to "Dock e busca",
            "app_icons" to "Ícones de apps",
            "style" to "Estilo",
            "icon_pack" to "Pacote de ícones",
            "app_icon_size" to "Tamanho do ícone",
            "home_screen_layout" to "Layout da tela de início",
            "grid_auto" to "Automático (Adaptativo)",
            "grid_4x6" to "4 × 6 (Padrão)",
            "grid_4x7" to "4 × 7 (Tela alta)",
            "show_app_names" to "Mostrar nomes dos apps",
            "app_name_size" to "Tamanho do nome",
            "arrange_auto" to "Organizar ícones automaticamente",
            "control_centre" to "Central de controle e sobreposição",
            "control_centre_overlay" to "Central de controle em todos os apps",
            "notification_badges" to "Selos e central de notificações",
            "widgets" to "Widgets",
            "choose_photos" to "Escolher fotos em destaque",
            "weather_location" to "Localização do clima",
            "device_gps" to "GPS do dispositivo",
            "language" to "Idioma",
            "about_glass" to "Sobre o Glass OS",
            "feature_tour" to "Tour de recursos",
            "welcome" to "Bem-vindo ao Glass OS",
            "choose_language" to "Escolha seu idioma preferido",
            "continue_btn" to "Continuar",
            "done" to "Concluído",
            "search" to "Pesquisar",
            "edit" to "Editar",
            "folder" to "Pasta",
            "app_library" to "Biblioteca de apps",
            "set_default_home_title" to "Definir iniciador padrão",
            "set_default_home_desc" to "Defina o Glass OS como iniciador padrão para abrir diretamente sua tela inicial personalizada e widgets.",
            "default_home_set_desc" to "O Glass OS é o seu iniciador padrão! Você voltará aqui sempre que tocar no botão Início.",
            "set_default_home_btn" to "Definir como padrão",
            "default_home_set" to "Iniciador padrão ativo",
            "continue_tour_btn" to "Continuar para o tour",
            "skip_for_now" to "Pular por enquanto",
            "not_default_prompt_desc" to "O Glass OS não é o seu app de início padrão. Defina-o como padrão para usar sempre que voltar ao início.",
            "maybe_later" to "Mais tarde",
        ),
        "ja" to mapOf(
            "settings" to "設定",
            "home_screen" to "ホーム画面",
            "finish_setup" to "セットアップを完了",
            "glass" to "グラス",
            "look" to "外観",
            "widgets_panels" to "ウィジェットとパネル",
            "dock_search" to "Dockと検索",
            "app_icons" to "アプリアイコン",
            "style" to "スタイル",
            "icon_pack" to "アイコンパック",
            "app_icon_size" to "アイコンサイズ",
            "home_screen_layout" to "ホーム画面の配置",
            "grid_auto" to "自動 (適応)",
            "grid_4x6" to "4 × 6 (標準)",
            "grid_4x7" to "4 × 7 (縦長画面)",
            "show_app_names" to "アプリ名を表示",
            "app_name_size" to "アプリ名のサイズ",
            "arrange_auto" to "アイコンを自動整列",
            "control_centre" to "コントロールセンターとオーバーレイ",
            "control_centre_overlay" to "すべてのアプリでコントロールセンター",
            "notification_badges" to "通知バッジと通知センター",
            "widgets" to "ウィジェット",
            "choose_photos" to "おすすめの写真を選択",
            "weather_location" to "天気の場所",
            "device_gps" to "デバイスGPS",
            "language" to "言語",
            "about_glass" to "Glass OSについて",
            "feature_tour" to "機能ツアー",
            "welcome" to "Glass OSへようこそ",
            "choose_language" to "お好みの言語を選択",
            "continue_btn" to "続ける",
            "done" to "完了",
            "search" to "検索",
            "edit" to "編集",
            "folder" to "フォルダ",
            "app_library" to "Appライブラリ",
            "set_default_home_title" to "デフォルトのランチャーに設定",
            "set_default_home_desc" to "ホーム画面、ウィジェット、ドックを直接開くために、Glass OSをデフォルトのランチャーに設定します。",
            "default_home_set_desc" to "Glass OSがデフォルトのランチャーです！ホームボタンを押すとここに戻ります。",
            "set_default_home_btn" to "デフォルトに設定",
            "default_home_set" to "デフォルト設定完了",
            "continue_tour_btn" to "ツアーを続ける",
            "skip_for_now" to "今はスキップ",
            "not_default_prompt_desc" to "Glass OSはデフォルトのホームアプリではありません。ホームボタンで開くようにデフォルトに設定してください。",
            "maybe_later" to "後で",
        ),
        "ru" to mapOf(
            "settings" to "Настройки",
            "home_screen" to "Главный экран",
            "finish_setup" to "Завершить настройку",
            "glass" to "Стекло",
            "look" to "Вид",
            "widgets_panels" to "Виджеты и панели",
            "dock_search" to "Док и поиск",
            "app_icons" to "Значки приложений",
            "style" to "Стиль",
            "icon_pack" to "Пакет значков",
            "app_icon_size" to "Размер значков",
            "home_screen_layout" to "Макет главного экрана",
            "grid_auto" to "Авто (Адаптивный)",
            "grid_4x6" to "4 × 6 (Стандарт)",
            "grid_4x7" to "4 × 7 (Высокий экран)",
            "show_app_names" to "Показывать названия",
            "app_name_size" to "Размер названий",
            "arrange_auto" to "Авторасстановка значков",
            "control_centre" to "Пункт управления и оверлей",
            "control_centre_overlay" to "Пункт управления во всех приложениях",
            "notification_badges" to "Бейджи и центр уведомлений",
            "widgets" to "Виджеты",
            "choose_photos" to "Выбрать фото",
            "weather_location" to "Местоположение погоды",
            "device_gps" to "GPS устройства",
            "language" to "Язык",
            "about_glass" to "О Glass OS",
            "feature_tour" to "Обзор функций",
            "welcome" to "Добро пожаловать в Glass OS",
            "choose_language" to "Выберите язык",
            "continue_btn" to "Продолжить",
            "done" to "Готово",
            "search" to "Поиск",
            "edit" to "Изменить",
            "folder" to "Папка",
            "app_library" to "Библиотека приложений",
            "set_default_home_title" to "Сделать лаунчером по умолчанию",
            "set_default_home_desc" to "Установите Glass OS лаунчером по умолчанию, чтобы сразу открывать главный экран, виджеты и док.",
            "default_home_set_desc" to "Glass OS установлен лаунчером по умолчанию! Вы будете возвращаться сюда при нажатии Домой.",
            "set_default_home_btn" to "Сделать по умолчанию",
            "default_home_set" to "Лаунчер по умолчанию установлен",
            "continue_tour_btn" to "Продолжить тур",
            "skip_for_now" to "Пропустить",
            "not_default_prompt_desc" to "Glass OS не является вашим главным экраном по умолчанию. Установите его по умолчанию для удобного доступа.",
            "maybe_later" to "Позже",
        ),
        "ar" to mapOf(
            "settings" to "الإعدادات",
            "home_screen" to "الشاشة الرئيسية",
            "finish_setup" to "إكمال الإعداد",
            "glass" to "زجاج",
            "look" to "المظهر",
            "widgets_panels" to "الأدوات واللوحات",
            "dock_search" to "شريط التطبيقات والبحث",
            "app_icons" to "أيقونات التطبيقات",
            "style" to "النمط",
            "icon_pack" to "حزمة الأيقونات",
            "app_icon_size" to "حجم الأيقونة",
            "home_screen_layout" to "تخطيط الشاشة الرئيسية",
            "grid_auto" to "تلقائي (متكيف)",
            "grid_4x6" to "4 × 6 (قياسي)",
            "grid_4x7" to "4 × 7 (شاشة طويلة)",
            "show_app_names" to "إظهار أسماء التطبيقات",
            "app_name_size" to "حجم اسم التطبيق",
            "arrange_auto" to "ترتيب الأيقونات تلقائياً",
            "control_centre" to "مركز التحكم والتراكب",
            "control_centre_overlay" to "مركز التحكم في جميع التطبيقات",
            "notification_badges" to "شارات ومركز الإشعارات",
            "widgets" to "الأدوات",
            "choose_photos" to "اختر الصور المميزة",
            "weather_location" to "موقع الطقس",
            "device_gps" to "نظام تحديد المواقع بالجهاز",
            "language" to "اللغة",
            "about_glass" to "حول Glass OS",
            "feature_tour" to "جولة الميزات",
            "welcome" to "مرحبًا بك في Glass OS",
            "choose_language" to "اختر لغتك المفضلة",
            "continue_btn" to "متابعة",
            "done" to "تم",
            "search" to "بحث",
            "edit" to "تعديل",
            "folder" to "مجلد",
            "app_library" to "مكتبة التطبيقات",
            "set_default_home_title" to "تعيين المشغّل الافتراضي",
            "set_default_home_desc" to "عيّن Glass OS كمشغّل افتراضي ليفتح هاتفك مباشرة على شاشتك الرئيسية وأدواتك المصغّرة.",
            "default_home_set_desc" to "تم تعيين Glass OS كمشغّل افتراضي! ستعود إلى هنا عند الضغط على زر الرئيسية.",
            "set_default_home_btn" to "تعيين كافتراضي",
            "default_home_set" to "تم تعيين المشغّل الافتراضي",
            "continue_tour_btn" to "متابعة الجولة",
            "skip_for_now" to "تخطي الآن",
            "not_default_prompt_desc" to "Glass OS ليس تطبيق الشاشة الرئيسية الافتراضي لديك. عيّنه كافتراضي لفتحه عند الضغط على زر الرئيسية.",
            "maybe_later" to "ربما لاحقاً",
        ),
        "zh" to mapOf(
            "settings" to "设置",
            "home_screen" to "主屏幕",
            "finish_setup" to "完成设置",
            "glass" to "玻璃",
            "look" to "外观",
            "widgets_panels" to "小部件与面板",
            "dock_search" to "程序坞与搜索",
            "app_icons" to "应用图标",
            "style" to "样式",
            "icon_pack" to "图标包",
            "app_icon_size" to "图标大小",
            "home_screen_layout" to "主屏幕布局",
            "grid_auto" to "自动 (自适应)",
            "grid_4x6" to "4 × 6 (标准)",
            "grid_4x7" to "4 × 7 (长屏)",
            "show_app_names" to "显示应用名称",
            "app_name_size" to "应用名称大小",
            "arrange_auto" to "自动排列图标",
            "control_centre" to "控制中心与悬浮",
            "control_centre_overlay" to "全局控制中心 (悬浮)",
            "notification_badges" to "通知标记与通知中心",
            "widgets" to "小部件",
            "choose_photos" to "选择精选照片",
            "weather_location" to "天气位置",
            "device_gps" to "设备 GPS",
            "language" to "语言",
            "about_glass" to "关于 Glass OS",
            "feature_tour" to "功能导览",
            "welcome" to "欢迎使用 Glass OS",
            "choose_language" to "选择您的首选语言",
            "continue_btn" to "继续",
            "done" to "完成",
            "search" to "搜索",
            "edit" to "编辑",
            "folder" to "文件夹",
            "app_library" to "App 资源库",
            "set_default_home_title" to "设置为默认启动器",
            "set_default_home_desc" to "将 Glass OS 设置为默认启动器，以便手机直接打开您的主屏幕、小部件和程序坞。",
            "default_home_set_desc" to "Glass OS 已设置为您的默认启动器！每当您轻触主屏幕或向上滑动时都会返回这里。",
            "set_default_home_btn" to "设置为默认",
            "default_home_set" to "已设为默认启动器",
            "continue_tour_btn" to "继续功能导览",
            "skip_for_now" to "暂时跳过",
            "not_default_prompt_desc" to "Glass OS 尚不是您的默认主屏幕应用。请将其设为默认，以便按主页键或上滑时直接打开。",
            "maybe_later" to "稍后再说",
        ),
    )
}
