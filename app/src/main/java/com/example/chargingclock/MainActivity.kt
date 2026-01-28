package com.example.chargingclock

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory // !!! ADDED IMPORT !!!
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

// Compose Imports
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : AppCompatActivity() {

    // --- Managers ---
    private lateinit var weatherManager: WeatherManager
    private lateinit var calendarManager: CalendarManager
    private lateinit var batteryManager: BatteryManager
    private lateinit var brightnessManager: BrightnessManager
    private lateinit var clockManager: ClockManager
    private lateinit var musicUIManager: MusicUIManager
    // -----------------

    private var currentThemeId = 0
    private var currentTypeface: Typeface? = null

    private var globalTextColor = Color.WHITE
    private var globalThemeColor = Color.BLUE
    private val handler = Handler(Looper.getMainLooper())

    // UI Elements
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var ivBackground: ImageView
    private lateinit var batteryStatusText: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var viewBgLeft: View
    private lateinit var viewBgRight: View
    private lateinit var ivBlurLeft: ImageView
    private lateinit var ivBlurRight: ImageView

    // Clock
    private lateinit var clockContainer: LinearLayout
    private lateinit var tsHour1: TextSwitcher; private lateinit var tsHour2: TextSwitcher
    private lateinit var tvSeparator: TextView
    private lateinit var tsMinute1: TextSwitcher; private lateinit var tsMinute2: TextSwitcher
    private lateinit var textDateSmall: TextClock

    // Containers
    private lateinit var containerSide: FrameLayout
    private lateinit var dateInfoLayout: LinearLayout; private lateinit var textDateLarge: TextView
    private lateinit var alarmContainer: LinearLayout; private lateinit var ivAlarmIcon: ImageView
    private lateinit var tvNextAlarmTime: TextView; private lateinit var tvMiniWeather: TextView

    // Calendar
    private lateinit var calendarLayout: LinearLayout; private lateinit var tvMonthName: TextView
    private lateinit var calendarHeader: LinearLayout; private lateinit var calendarGrid: GridLayout
    private lateinit var tvEvent: TextView

    // Weather (tvWeatherIcon REMOVED)
    private lateinit var weatherLayout: LinearLayout; private lateinit var tvLocation: TextView
    private lateinit var tvWeatherTemp: TextView; private lateinit var tvWeatherCondition: TextView
    private lateinit var composeWeatherIcon: ComposeView

    // Music
    private lateinit var musicLayout: LinearLayout; private lateinit var ivAlbumArt: ImageView
    private lateinit var tvTrackTitle: TextView; private lateinit var tvTrackArtist: TextView
    private lateinit var seekBarMusic: SeekBar
    private lateinit var btnPrev: ImageButton; private lateinit var btnPlayPause: ImageButton; private lateinit var btnNext: ImageButton

    // Compose State
    private var composeIconColorState = mutableStateOf(ComposeColor.White)
    private var composeWeatherConditionState = mutableStateOf("clear")

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            clockManager.updateTime()
            updateNextAlarm()
            if (calendarLayout.visibility == View.VISIBLE) {
                calendarManager.drawCalendar(globalTextColor, globalThemeColor, currentTypeface)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        currentThemeId = prefs.getInt("THEME_COLOR_ID", 0)
        setTheme(getThemeResId(currentThemeId))

        setContentView(R.layout.activity_main)

        initViews()
        initManagers()
        initListeners()
    }

    private fun setupWindowFlags() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true) } else { window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.rootLayout)
        ivBackground = findViewById(R.id.ivBackground)
        batteryStatusText = findViewById(R.id.batteryStatus)
        btnSettings = findViewById(R.id.btnSettings)
        viewBgLeft = findViewById(R.id.viewBgLeft); viewBgRight = findViewById(R.id.viewBgRight)
        ivBlurLeft = findViewById(R.id.ivBlurLeft); ivBlurRight = findViewById(R.id.ivBlurRight)
        setupBlurViewClipping(ivBlurLeft); setupBlurViewClipping(ivBlurRight)

        clockContainer = findViewById(R.id.clockContainer)
        tsHour1 = findViewById(R.id.tsHour1); tsHour2 = findViewById(R.id.tsHour2)
        tvSeparator = findViewById(R.id.tvSeparator)
        tsMinute1 = findViewById(R.id.tsMinute1); tsMinute2 = findViewById(R.id.tsMinute2)

        containerSide = findViewById(R.id.containerSide)
        textDateSmall = findViewById(R.id.textDateSmall)
        dateInfoLayout = findViewById(R.id.dateInfoLayout); textDateLarge = findViewById(R.id.textDateLarge)
        alarmContainer = findViewById(R.id.alarmContainer); ivAlarmIcon = findViewById(R.id.ivAlarmIcon); tvNextAlarmTime = findViewById(R.id.tvNextAlarmTime)
        tvMiniWeather = findViewById(R.id.tvMiniWeather)

        calendarLayout = findViewById(R.id.calendarLayout); tvMonthName = findViewById(R.id.tvMonthName)
        calendarHeader = findViewById(R.id.calendarHeader); calendarGrid = findViewById(R.id.calendarGrid); tvEvent = findViewById(R.id.tvEvent)

        weatherLayout = findViewById(R.id.weatherLayout); tvLocation = findViewById(R.id.tvLocation)
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp); tvWeatherCondition = findViewById(R.id.tvWeatherCondition)

        // !!! CHANGE: tvWeatherIcon removed
        composeWeatherIcon = findViewById(R.id.composeWeatherIcon)

        musicLayout = findViewById(R.id.musicLayout); ivAlbumArt = findViewById(R.id.ivAlbumArt)
        tvTrackTitle = findViewById(R.id.tvTrackTitle); tvTrackArtist = findViewById(R.id.tvTrackArtist)
        seekBarMusic = findViewById(R.id.seekBarMusic)
        btnPrev = findViewById(R.id.btnPrev); btnPlayPause = findViewById(R.id.btnPlayPause); btnNext = findViewById(R.id.btnNext)
    }

    private fun initManagers() {
        batteryManager = BatteryManager(this, batteryStatusText)
        batteryManager.register()

        brightnessManager = BrightnessManager(this) { isNight ->
            if (isNight) applyNightText() else applyAppearance(getSharedPreferences("AppConfig", Context.MODE_PRIVATE))
        }

        clockManager = ClockManager(this, tsHour1, tsHour2, tsMinute1, tsMinute2, tvSeparator)
        clockManager.setupSwitchers()

        musicUIManager = MusicUIManager(tvTrackTitle, tvTrackArtist, ivAlbumArt, seekBarMusic, btnPlayPause, handler)

        // !!! CHANGE: Removed tvWeatherIcon from constructor
        weatherManager = WeatherManager(
            this,
            WeatherRepository(this),
            weatherLayout,
            tvLocation,
            tvWeatherTemp,
            tvWeatherCondition,
            tvMiniWeather,
            composeWeatherIcon,
            composeIconColorState,
            composeWeatherConditionState,
            handler
        )
        weatherManager.initCompose()

        calendarManager = CalendarManager(this, CalendarRepository(this), calendarLayout, tvMonthName, calendarHeader, calendarGrid, tvEvent)
    }

    private fun initListeners() {
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        val controls = MusicService.currentController?.transportControls
        btnPrev.setOnClickListener { controls?.skipToPrevious() ?: checkPlayer() }
        btnNext.setOnClickListener { controls?.skipToNext() ?: checkPlayer() }
        btnPlayPause.setOnClickListener {
            val controller = MusicService.currentController
            if (controller != null) {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) controller.transportControls?.pause() else controller.transportControls?.play()
            } else checkPlayer()
        }

        seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) { musicUIManager.isTrackingTouch = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                musicUIManager.isTrackingTouch = false
                MusicService.currentController?.transportControls?.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })
    }

    private fun checkPlayer() {
        MusicService.instance?.refreshControllers()
        Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()

        val timeFilter = IntentFilter().apply { addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED) }
        registerReceiver(timeTickReceiver, timeFilter)

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        currentThemeId = prefs.getInt("THEME_COLOR_ID", 0)

        brightnessManager.isAutoBrightnessEnabled = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        weatherManager.isAutoLocationEnabled = prefs.getBoolean("AUTO_LOCATION", false)
        val isSplitMode = prefs.getBoolean("IS_SPLIT_MODE", false)
        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)

        applyAppearance(prefs)
        applyCustomFont(prefs)

        val showBattery = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        batteryStatusText.visibility = if (showBattery) View.VISIBLE else View.GONE

        brightnessManager.start()
        weatherManager.startUpdates()
        musicUIManager.stopUpdates()

        updateLayoutConstraints(isSplitMode)

        val showPanels = prefs.getBoolean("SHOW_PANELS", false)
        if (showPanels) {
            viewBgLeft.visibility = View.VISIBLE; ivBlurLeft.visibility = View.VISIBLE
            if (isSplitMode) { viewBgRight.visibility = View.VISIBLE; ivBlurRight.visibility = View.VISIBLE } else { viewBgRight.visibility = View.GONE; ivBlurRight.visibility = View.GONE }
        } else {
            viewBgLeft.visibility = View.GONE; ivBlurLeft.visibility = View.GONE; viewBgRight.visibility = View.GONE; ivBlurRight.visibility = View.GONE
        }

        if (isSplitMode) {
            containerSide.visibility = View.VISIBLE; textDateSmall.visibility = View.GONE; clockManager.applySize(130f)
            dateInfoLayout.visibility = View.GONE; calendarLayout.visibility = View.GONE; weatherLayout.visibility = View.GONE; musicLayout.visibility = View.GONE

            when (sideMode) {
                0 -> {
                    dateInfoLayout.visibility = View.VISIBLE
                    val showAlarm = prefs.getBoolean("SHOW_NEXT_ALARM", false)
                    val showMiniWeather = prefs.getBoolean("SHOW_MINI_WEATHER", false)
                    alarmContainer.visibility = if (showAlarm) View.VISIBLE else View.GONE
                    tvMiniWeather.visibility = if (showMiniWeather) View.VISIBLE else View.GONE
                    alarmContainer.setOnClickListener { try { val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS); intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK; startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "Clock not found", Toast.LENGTH_SHORT).show() } }
                    updateNextAlarm()
                }
                1 -> {
                    calendarLayout.visibility = View.VISIBLE
                    calendarManager.drawCalendar(globalTextColor, globalThemeColor, currentTypeface)
                }
                2 -> { weatherLayout.visibility = View.VISIBLE }
                3 -> {
                    musicLayout.visibility = View.VISIBLE
                    if (checkNotificationPermission()) {
                        MusicService.updateUI = { controller -> musicUIManager.updateUI(controller) }
                        MusicService.instance?.refreshControllers()
                        musicUIManager.updateUI(MusicService.currentController)
                        musicUIManager.startUpdates()
                    }
                }
            }
        } else {
            containerSide.visibility = View.GONE
            val showDate = prefs.getBoolean("SHOW_CLOCK_DATE", true)
            if (showDate) { textDateSmall.visibility = View.VISIBLE; clockManager.applySize(200f) } else { textDateSmall.visibility = View.GONE; clockManager.applySize(250f) }
        }

        clockManager.updateTime()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(timeTickReceiver) } catch (e: Exception) {}
        brightnessManager.stop()
        weatherManager.stopUpdates()
        musicUIManager.stopUpdates()
        MusicService.updateUI = null
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryManager.unregister()
    }

    // --- Helpers ---
    private fun updateLayoutConstraints(isSplit: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)
        if (isSplit) {
            constraintSet.connect(R.id.viewBgLeft, ConstraintSet.END, R.id.guidelineVertical, ConstraintSet.START)
            constraintSet.connect(R.id.clockContainer, ConstraintSet.END, R.id.guidelineVertical, ConstraintSet.START)
        } else {
            constraintSet.connect(R.id.viewBgLeft, ConstraintSet.END, R.id.rootLayout, ConstraintSet.END)
            constraintSet.connect(R.id.clockContainer, ConstraintSet.END, R.id.rootLayout, ConstraintSet.END)
        }
        constraintSet.applyTo(rootLayout)
    }

    private fun checkNotificationPermission(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            Toast.makeText(this, "Music access required", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return false
        }
        return true
    }

    private fun updateNextAlarm() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("SHOW_NEXT_ALARM", false)) { alarmContainer.visibility = View.GONE; return }
        val info = (getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock
        if (info != null) {
            val format = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
            tvNextAlarmTime.text = SimpleDateFormat(format, Locale.getDefault()).format(Date(info.triggerTime))
            alarmContainer.visibility = View.VISIBLE
        } else { alarmContainer.visibility = View.GONE }
    }

    private fun applyAppearance(prefs: android.content.SharedPreferences) {
        val textColorId = prefs.getInt("TEXT_COLOR_ID", 0)
        val defaultTextColors = intArrayOf(Color.WHITE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED)
        val textColor = prefs.getInt("TEXT_COLOR_VALUE_$textColorId", defaultTextColors.getOrElse(textColorId) { Color.WHITE })
        globalTextColor = textColor

        clockManager.applyColor(textColor)

        // !!! CHANGE: tvWeatherIcon removed from list
        val viewsToColor = listOf(textDateSmall, textDateLarge, tvMiniWeather, tvWeatherTemp, tvWeatherCondition, tvTrackTitle, tvEvent, tvLocation, tvNextAlarmTime)
        for(v in viewsToColor) v.setTextColor(textColor)

        tvTrackArtist.setTextColor(Color.LTGRAY)
        btnPrev.setColorFilter(textColor); btnPlayPause.setColorFilter(textColor); btnNext.setColorFilter(textColor)
        ivAlarmIcon.setColorFilter(textColor)

        composeIconColorState.value = ComposeColor(textColor)
        seekBarMusic.thumb.setTintList(null); seekBarMusic.progressDrawable.setTintList(null); ivAlbumArt.clearColorFilter()

        val showShadow = prefs.getBoolean("SHOW_TEXT_SHADOW", true)
        val shadowColor = Color.parseColor("#80000000")
        val radius = if (showShadow) 12f else 0f
        val dx = if (showShadow) 4f else 0f
        val dy = if (showShadow) 4f else 0f
        for(v in viewsToColor) v.setShadowLayer(radius, dx, dy, shadowColor)

        val themeColorId = prefs.getInt("THEME_COLOR_ID", 0)
        val defaultThemeColors = intArrayOf(Color.parseColor("#448AFF"), Color.parseColor("#FF5252"), Color.parseColor("#69F0AE"), Color.parseColor("#FFFF00"), Color.parseColor("#E040FB"))
        val themeColor = prefs.getInt("THEME_COLOR_VALUE_$themeColorId", defaultThemeColors.getOrElse(themeColorId) { Color.parseColor("#448AFF") })
        globalThemeColor = themeColor

        calendarManager.drawCalendar(textColor, themeColor, currentTypeface)

        val bgImageUriStr = prefs.getString("BG_IMAGE_URI", null)
        val bgAlpha = prefs.getInt("BG_IMAGE_ALPHA", 255)
        val showPanels = prefs.getBoolean("SHOW_PANELS", false)
        val isBgFill = prefs.getBoolean("BG_SCALE_FILL", true)

        if (bgImageUriStr != null) {
            try {
                val uri = Uri.parse(bgImageUriStr)
                ivBackground.visibility = View.VISIBLE
                ivBackground.setImageURI(uri)
                ivBackground.scaleType = if (isBgFill) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                ivBackground.imageAlpha = if (showPanels) 255 else bgAlpha

                ivBlurLeft.setImageURI(uri); ivBlurLeft.imageAlpha = bgAlpha
                alignBackgrounds(uri, isBgFill)
                ivBlurRight.setImageURI(uri); ivBlurRight.imageAlpha = bgAlpha
                rootLayout.setBackgroundColor(Color.BLACK)
            } catch (e: Exception) { hideBackgroundImages(); applyBackgroundColor(prefs) }
        } else { hideBackgroundImages(); applyBackgroundColor(prefs) }

        val panelColorId = prefs.getInt("PANEL_COLOR_ID", 0)
        val defaultPanelColor = intArrayOf(Color.WHITE, Color.parseColor("#444444"), Color.parseColor("#B71C1C"), Color.parseColor("#E65100"), Color.parseColor("#1B5E20")).getOrElse(panelColorId) { Color.WHITE }
        val panelBaseColor = prefs.getInt("PANEL_COLOR_VALUE_$panelColorId", defaultPanelColor)
        val alphaPercent = prefs.getInt("PANEL_ALPHA", 30)
        val alpha255 = (alphaPercent * 255) / 100
        applyPanelStyle(panelBaseColor, alpha255, prefs.getInt("PANEL_BLUR_RADIUS", 0))
    }

    private fun applyNightText() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val color = prefs.getInt("NIGHT_FILTER_COLOR", Color.parseColor("#9EA793"))

        ivBackground.visibility = View.GONE; ivBlurLeft.visibility = View.GONE; ivBlurRight.visibility = View.GONE
        rootLayout.setBackgroundColor(Color.BLACK)

        clockManager.applyColor(color)

        // !!! CHANGE: tvWeatherIcon removed
        val viewsToColor = listOf(textDateSmall, textDateLarge, tvMiniWeather, tvWeatherTemp, tvWeatherCondition, tvTrackTitle, tvTrackArtist, tvEvent, tvLocation, tvNextAlarmTime, batteryStatusText)
        for(v in viewsToColor) {
            v.setTextColor(color)
            v.setShadowLayer(0f, 0f, 0f, 0)
        }

        btnPrev.setColorFilter(color); btnPlayPause.setColorFilter(color); btnNext.setColorFilter(color)
        ivAlarmIcon.setColorFilter(color)

        composeIconColorState.value = ComposeColor(color)
        seekBarMusic.thumb.setTint(color); seekBarMusic.progressDrawable.setTint(color)
        ivAlbumArt.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)

        batteryManager.updateNightMode(true, color)

        globalTextColor = color; globalThemeColor = color
        calendarManager.drawCalendar(color, color, currentTypeface)
        applyPanelStyle(color, 50, 0)
    }

    private fun hideBackgroundImages() { ivBackground.visibility = View.GONE; ivBlurLeft.setImageDrawable(null); ivBlurRight.setImageDrawable(null) }
    private fun applyBackgroundColor(prefs: android.content.SharedPreferences) { val bgColorId = prefs.getInt("BG_COLOR_ID", 0); val defaultBgColors = intArrayOf(Color.parseColor("#333333"), Color.parseColor("#888888"), Color.parseColor("#0D47A1"), Color.parseColor("#B71C1C"), Color.parseColor("#1B5E20")); val defaultColor = defaultBgColors.getOrElse(bgColorId) { Color.DKGRAY }; val bgColor = prefs.getInt("BG_COLOR_VALUE_$bgColorId", defaultColor); rootLayout.setBackgroundColor(bgColor) }
    private fun getThemeResId(id: Int): Int { return when(id) { 1 -> R.style.Theme_ChargingClock_Red; 2 -> R.style.Theme_ChargingClock_Green; 3 -> R.style.Theme_ChargingClock_Yellow; 4 -> R.style.Theme_ChargingClock_Purple; else -> R.style.Theme_ChargingClock_Blue } }
    private fun applyPanelStyle(color: Int, alpha: Int, blurRadius: Int) {
        val finalColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        (viewBgLeft.background.mutate() as? GradientDrawable)?.setColor(finalColor)
        (viewBgRight.background.mutate() as? GradientDrawable)?.setColor(finalColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val effect = if (blurRadius > 0) { val r = (blurRadius / 2.0f).coerceAtLeast(0.1f); RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR) } else null
            ivBlurLeft.setRenderEffect(effect); ivBlurRight.setRenderEffect(effect)
        }
    }
    private fun hideSystemUI() { window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) }

    private fun alignBackgrounds(uri: Uri, isFill: Boolean) {
        ivBlurLeft.post {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val screenWidth = rootLayout.width.toFloat(); val screenHeight = rootLayout.height.toFloat()
                    val bitmapWidth = bitmap.width.toFloat(); val bitmapHeight = bitmap.height.toFloat()
                    val scale = if (isFill) { if (bitmapWidth * screenHeight > screenWidth * bitmapHeight) screenHeight / bitmapHeight else screenWidth / bitmapWidth } else { min(screenWidth / bitmapWidth, screenHeight / bitmapHeight) }
                    val dx = (screenWidth - bitmapWidth * scale) * 0.5f
                    val dy = (screenHeight - bitmapHeight * scale) * 0.5f
                    val matrixLeft = Matrix().apply { setScale(scale, scale); postTranslate(dx, dy); postTranslate(-ivBlurLeft.left.toFloat(), -ivBlurLeft.top.toFloat()) }
                    ivBlurLeft.imageMatrix = matrixLeft; ivBlurLeft.setImageBitmap(bitmap)
                    val matrixRight = Matrix().apply { setScale(scale, scale); postTranslate(dx, dy); postTranslate(-ivBlurRight.left.toFloat(), -ivBlurRight.top.toFloat()) }
                    ivBlurRight.imageMatrix = matrixRight; ivBlurRight.setImageBitmap(bitmap)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun applyCustomFont(prefs: android.content.SharedPreferences) {
        val fontUriString = prefs.getString("CUSTOM_FONT_URI", null)
        if (fontUriString != null) {
            try {
                val pfd = contentResolver.openFileDescriptor(Uri.parse(fontUriString), "r")
                if (pfd != null) {
                    val typeface = Typeface.Builder(pfd.fileDescriptor).build()
                    pfd.close(); currentTypeface = typeface
                    clockManager.applyFont(typeface)
                    // !!! CHANGE: tvWeatherIcon removed
                    val views = listOf(textDateSmall, textDateLarge, tvWeatherTemp, tvWeatherCondition, batteryStatusText, tvTrackTitle, tvTrackArtist, tvEvent, tvLocation, tvNextAlarmTime, tvMiniWeather, tvMonthName)
                    for(v in views) v.typeface = typeface
                    if (calendarLayout.visibility == View.VISIBLE) calendarManager.drawCalendar(globalTextColor, globalThemeColor, typeface)
                }
            } catch (e: Exception) { resetFonts() }
        } else { resetFonts() }
    }

    private fun resetFonts() {
        currentTypeface = null
        clockManager.applyFont(Typeface.create("sans-serif-thin", Typeface.NORMAL))
        textDateSmall.typeface = Typeface.DEFAULT; textDateLarge.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        tvMonthName.typeface = Typeface.DEFAULT_BOLD
        // !!! CHANGE: tvWeatherIcon removed
        val views = listOf(tvWeatherCondition, batteryStatusText, tvTrackTitle, tvTrackArtist, tvEvent, tvLocation, tvNextAlarmTime, tvMiniWeather)
        for(v in views) v.typeface = Typeface.DEFAULT
        if (calendarLayout.visibility == View.VISIBLE) calendarManager.drawCalendar(globalTextColor, globalThemeColor, null)
    }

    private fun setupBlurViewClipping(imageView: ImageView) {
        imageView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        imageView.clipToOutline = true
        imageView.setBackgroundResource(R.drawable.bg_panel_rounded)
    }
}