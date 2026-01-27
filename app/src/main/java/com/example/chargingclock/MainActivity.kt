package com.example.chargingclock

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.CalendarView
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileDescriptor
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var currentThemeId = 0
    private var currentTextColorValue = 0
    private var currentThemeColorValue = 0
    private var currentTypeface: Typeface? = null

    private var lat = "55.75"
    private var lon = "37.62"

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var ivBackground: ImageView
    private lateinit var batteryStatusText: TextView
    private lateinit var btnSettings: ImageButton

    private lateinit var viewBgLeft: View
    private lateinit var viewBgRight: View
    private lateinit var ivBlurLeft: ImageView
    private lateinit var ivBlurRight: ImageView

    private lateinit var containerSide: FrameLayout
    private lateinit var clockContainer: LinearLayout
    private lateinit var tsHour1: TextSwitcher
    private lateinit var tsHour2: TextSwitcher
    private lateinit var tvSeparator: TextView
    private lateinit var tsMinute1: TextSwitcher
    private lateinit var tsMinute2: TextSwitcher
    private lateinit var textDateSmall: TextClock

    private lateinit var dateInfoLayout: LinearLayout
    private lateinit var textDateLarge: TextView
    private lateinit var alarmContainer: LinearLayout
    private lateinit var ivAlarmIcon: ImageView
    private lateinit var tvNextAlarmTime: TextView
    private lateinit var tvMiniWeather: TextView

    private lateinit var calendarLayout: LinearLayout
    private lateinit var tvMonthName: TextView
    private lateinit var calendarHeader: LinearLayout
    private lateinit var calendarGrid: GridLayout
    private lateinit var tvEvent: TextView

    private lateinit var weatherLayout: LinearLayout
    private lateinit var tvLocation: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvWeatherIcon: TextView

    private lateinit var musicLayout: LinearLayout
    private lateinit var ivAlbumArt: ImageView
    private lateinit var tvTrackTitle: TextView
    private lateinit var tvTrackArtist: TextView
    private lateinit var seekBarMusic: SeekBar
    private lateinit var btnPrev: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton

    private var mediaSessionManager: MediaSessionManager? = null
    private var isTrackingTouch = false
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var locationManager: LocationManager
    private var isAutoBrightnessEnabled = false
    private var isNightFilterEnabled = false // Фильтр ночного режима
    private var isAutoLocationEnabled = false
    private var isNightModeActive = false
    private var brightnessAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var globalTextColor = Color.WHITE
    private var globalThemeColor = Color.BLUE

    private val weatherUpdateRunnable = object : Runnable {
        override fun run() {
            if (isAutoLocationEnabled) { requestLocationUpdate() } else { fetchWeather() }
            handler.postDelayed(this, 3600000)
        }
    }

    private val musicProgressRunnable = object : Runnable {
        override fun run() {
            updateMusicProgress()
            if (MusicService.currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClockTime()
            updateNextAlarm()
            drawCustomCalendar(globalTextColor, globalThemeColor)
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> { batteryStatusText.text = "Заряжается"; updateBatteryColor() }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    batteryStatusText.text = "Работа от батареи"
                    batteryStatusText.setTextColor(Color.RED)
                    val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("BG_MODE_ENABLED", false)) {
                        this@MainActivity.finishAffinity()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true) } else { window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        currentThemeId = prefs.getInt("THEME_COLOR_ID", 0)
        setTheme(getThemeResId(currentThemeId))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        ivBackground = findViewById(R.id.ivBackground)
        batteryStatusText = findViewById(R.id.batteryStatus)
        btnSettings = findViewById(R.id.btnSettings)

        viewBgLeft = findViewById(R.id.viewBgLeft)
        viewBgRight = findViewById(R.id.viewBgRight)
        ivBlurLeft = findViewById(R.id.ivBlurLeft)
        ivBlurRight = findViewById(R.id.ivBlurRight)

        setupBlurViewClipping(ivBlurLeft)
        setupBlurViewClipping(ivBlurRight)

        clockContainer = findViewById(R.id.clockContainer)
        tsHour1 = findViewById(R.id.tsHour1)
        tsHour2 = findViewById(R.id.tsHour2)
        tvSeparator = findViewById(R.id.tvSeparator)
        tsMinute1 = findViewById(R.id.tsMinute1)
        tsMinute2 = findViewById(R.id.tsMinute2)
        setupTextSwitcher(tsHour1); setupTextSwitcher(tsHour2); setupTextSwitcher(tsMinute1); setupTextSwitcher(tsMinute2)

        containerSide = findViewById(R.id.containerSide)
        textDateSmall = findViewById(R.id.textDateSmall)
        dateInfoLayout = findViewById(R.id.dateInfoLayout)
        textDateLarge = findViewById(R.id.textDateLarge)
        alarmContainer = findViewById(R.id.alarmContainer)
        ivAlarmIcon = findViewById(R.id.ivAlarmIcon)
        tvNextAlarmTime = findViewById(R.id.tvNextAlarmTime)
        tvMiniWeather = findViewById(R.id.tvMiniWeather)

        calendarLayout = findViewById(R.id.calendarLayout)
        tvMonthName = findViewById(R.id.tvMonthName)
        calendarHeader = findViewById(R.id.calendarHeader)
        calendarGrid = findViewById(R.id.calendarGrid)
        tvEvent = findViewById(R.id.tvEvent)

        weatherLayout = findViewById(R.id.weatherLayout)
        tvLocation = findViewById(R.id.tvLocation)
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp)
        tvWeatherCondition = findViewById(R.id.tvWeatherCondition)
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon)
        musicLayout = findViewById(R.id.musicLayout)
        ivAlbumArt = findViewById(R.id.ivAlbumArt)
        tvTrackTitle = findViewById(R.id.tvTrackTitle)
        tvTrackArtist = findViewById(R.id.tvTrackArtist)
        seekBarMusic = findViewById(R.id.seekBarMusic)
        btnPrev = findViewById(R.id.btnPrev)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        btnPrev.setOnClickListener { MusicService.currentController?.transportControls?.skipToPrevious() ?: run { MusicService.instance?.refreshControllers(); Toast.makeText(this, "Плеер не найден", Toast.LENGTH_SHORT).show() } }
        btnNext.setOnClickListener { MusicService.currentController?.transportControls?.skipToNext() ?: run { MusicService.instance?.refreshControllers(); Toast.makeText(this, "Плеер не найден", Toast.LENGTH_SHORT).show() } }
        btnPlayPause.setOnClickListener {
            val controller = MusicService.currentController
            if (controller != null) {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) controller.transportControls?.pause() else controller.transportControls?.play()
            } else { MusicService.instance?.refreshControllers(); Toast.makeText(this, "Плеер не найден", Toast.LENGTH_SHORT).show() }
        }

        seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isTrackingTouch = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTrackingTouch = false
                val controller = MusicService.currentController
                if (controller != null) { controller.transportControls?.seekTo(seekBar?.progress?.toLong() ?: 0L); updateMusicProgress() }
            }
        })

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        val filter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
        registerReceiver(powerReceiver, filter)
        updateInitialBatteryStatus()
    }

    private fun setupBlurViewClipping(imageView: ImageView) {
        imageView.outlineProvider = ViewOutlineProvider.BACKGROUND
        imageView.clipToOutline = true
        imageView.setBackgroundResource(R.drawable.bg_panel_rounded)
    }

    private fun setupTextSwitcher(ts: TextSwitcher) {
        ts.setFactory {
            val t = TextView(this)
            t.gravity = Gravity.CENTER
            t.includeFontPadding = false
            t.setTextColor(Color.WHITE)
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100f)
            t.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            t.fontFeatureSettings = "tnum"
            t
        }
        ts.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        ts.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()

        val timeFilter = IntentFilter().apply { addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED) }
        registerReceiver(timeTickReceiver, timeFilter)

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        val newThemeId = prefs.getInt("THEME_COLOR_ID", 0)
        val textColorId = prefs.getInt("TEXT_COLOR_ID", 0)
        val defaultTextColors = intArrayOf(Color.WHITE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED)
        val defaultTextColor = defaultTextColors.getOrElse(textColorId) { Color.WHITE }
        val newTextColorValue = prefs.getInt("TEXT_COLOR_VALUE_$textColorId", defaultTextColor)
        val themeColorId = prefs.getInt("THEME_COLOR_ID", 0)
        val defaultThemeColors = intArrayOf(Color.parseColor("#448AFF"), Color.parseColor("#FF5252"), Color.parseColor("#69F0AE"), Color.parseColor("#FFFF00"), Color.parseColor("#E040FB"))
        val defaultThemeColor = defaultThemeColors.getOrElse(themeColorId) { Color.parseColor("#448AFF") }
        val newThemeColorValue = prefs.getInt("THEME_COLOR_VALUE_$themeColorId", defaultThemeColor)

        if (newThemeId != currentThemeId || (currentTextColorValue != 0 && newTextColorValue != currentTextColorValue) || (currentThemeColorValue != 0 && newThemeColorValue != currentThemeColorValue)) {
            currentThemeId = newThemeId
            currentTextColorValue = newTextColorValue
            currentThemeColorValue = newThemeColorValue
            recreate()
            return
        }

        currentThemeId = newThemeId
        currentTextColorValue = newTextColorValue
        currentThemeColorValue = newThemeColorValue

        isNightModeActive = false
        isAutoBrightnessEnabled = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        isNightFilterEnabled = prefs.getBoolean("NIGHT_FILTER_ENABLED", prefs.getBoolean("RED_TINT_ENABLED", false))
        isAutoLocationEnabled = prefs.getBoolean("AUTO_LOCATION", false)
        val isSplitMode = prefs.getBoolean("IS_SPLIT_MODE", false)
        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)

        // Применяем настройки вида
        applyAppearance(prefs)
        applyCustomFont(prefs)

        val showBattery = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        batteryStatusText.visibility = if (showBattery) View.VISIBLE else View.GONE

        if (isAutoBrightnessEnabled) { lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) } } else { sensorManager.unregisterListener(this); resetBrightnessAndFilter() }

        handler.removeCallbacks(weatherUpdateRunnable)
        handler.removeCallbacks(musicProgressRunnable)

        updateLayoutConstraints(isSplitMode)

        val showPanels = prefs.getBoolean("SHOW_PANELS", false)
        if (showPanels) {
            viewBgLeft.visibility = View.VISIBLE
            ivBlurLeft.visibility = View.VISIBLE
            if (isSplitMode) { viewBgRight.visibility = View.VISIBLE; ivBlurRight.visibility = View.VISIBLE } else { viewBgRight.visibility = View.GONE; ivBlurRight.visibility = View.GONE }
        } else {
            viewBgLeft.visibility = View.GONE; ivBlurLeft.visibility = View.GONE; viewBgRight.visibility = View.GONE; ivBlurRight.visibility = View.GONE
        }

        if (isSplitMode) {
            containerSide.visibility = View.VISIBLE
            textDateSmall.visibility = View.GONE
            setClockSize(130f)
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
                    if (showMiniWeather) weatherUpdateRunnable.run()
                }
                1 -> { calendarLayout.visibility = View.VISIBLE; drawCustomCalendar(globalTextColor, globalThemeColor) }
                2 -> { weatherLayout.visibility = View.VISIBLE; weatherUpdateRunnable.run() }
                3 -> {
                    musicLayout.visibility = View.VISIBLE
                    if (checkNotificationPermission()) {
                        MusicService.updateUI = { controller -> updateMusicUI(controller) }
                        MusicService.instance?.refreshControllers()
                        updateMusicUI(MusicService.currentController)
                        handler.post(musicProgressRunnable)
                    }
                }
            }
        } else {
            containerSide.visibility = View.GONE
            val showDate = prefs.getBoolean("SHOW_CLOCK_DATE", true)
            if (showDate) { textDateSmall.visibility = View.VISIBLE; setClockSize(200f) } else { textDateSmall.visibility = View.GONE; setClockSize(250f) }
        }
        updateClockTime()
    }

    private fun applyTextShadow(isEnabled: Boolean) {
        val radius = if (isEnabled) 12f else 0f
        val dx = if (isEnabled) 4f else 0f
        val dy = if (isEnabled) 4f else 0f
        val color = Color.parseColor("#80000000")

        fun setShadow(tv: TextView) { tv.setShadowLayer(radius, dx, dy, color) }

        setShadow(textDateSmall)
        setShadow(textDateLarge)
        setShadow(batteryStatusText)
        setShadow(tvSeparator)
        setShadow(tvMiniWeather)
        setShadow(tvLocation)
        setShadow(tvWeatherTemp)
        setShadow(tvWeatherCondition)
        setShadow(tvWeatherIcon)
        setShadow(tvNextAlarmTime)
        setShadow(tvTrackTitle)
        setShadow(tvTrackArtist)
        setShadow(tvEvent)
        setShadow(tvMonthName)

        val switchers = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2)
        for (ts in switchers) {
            for (i in 0 until ts.childCount) {
                val child = ts.getChildAt(i)
                if (child is TextView) setShadow(child)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(timeTickReceiver) } catch (e: Exception) {}
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(weatherUpdateRunnable)
        handler.removeCallbacks(musicProgressRunnable)
        MusicService.updateUI = null
        brightnessAnimator?.cancel()
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

    private fun updateMusicUI(controller: MediaController?) {
        if (controller == null) {
            tvTrackTitle.text = "Play Music"; tvTrackArtist.text = "Waiting..."
            seekBarMusic.progress = 0; seekBarMusic.max = 100
            ivAlbumArt.setImageResource(R.drawable.ic_music_note)
            return
        }
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Artist"
        val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        tvTrackTitle.text = title
        tvTrackArtist.text = artist
        if (bitmap != null) ivAlbumArt.setImageBitmap(bitmap) else try { ivAlbumArt.setImageResource(R.drawable.ic_music_note) } catch (e: Exception) {}
        if (playbackState?.state == PlaybackState.STATE_PLAYING) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            handler.removeCallbacks(musicProgressRunnable)
            handler.post(musicProgressRunnable)
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            handler.removeCallbacks(musicProgressRunnable)
        }
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (duration > 0) seekBarMusic.max = duration.toInt()
        updateMusicProgress()
    }

    private fun updateMusicProgress() {
        val controller = MusicService.currentController
        if (!isTrackingTouch && controller != null) {
            val playbackState = controller.playbackState
            if (playbackState != null) {
                var currentPos = playbackState.position
                if (playbackState.state == PlaybackState.STATE_PLAYING) {
                    val lastUpdateTime = playbackState.lastPositionUpdateTime
                    if (lastUpdateTime > 0) {
                        val timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime
                        val speed = playbackState.playbackSpeed
                        currentPos += (timeDelta * speed).toLong()
                    }
                }
                if (currentPos > seekBarMusic.max) currentPos = seekBarMusic.max.toLong()
                if (currentPos < 0) currentPos = 0
                seekBarMusic.progress = currentPos.toInt()
            }
        }
    }

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

    private fun updateClockTime() {
        val format = if (DateFormat.is24HourFormat(this)) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        val timeString = sdf.format(Date())
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hours = parts[0]; val minutes = parts[1]
            if (hours.length == 2) { setSwitcherText(tsHour1, hours[0].toString()); setSwitcherText(tsHour2, hours[1].toString()) } else { setSwitcherText(tsHour1, "0"); setSwitcherText(tsHour2, hours) }
            if (minutes.length == 2) { setSwitcherText(tsMinute1, minutes[0].toString()); setSwitcherText(tsMinute2, minutes[1].toString()) }
        }
        if (dateInfoLayout.visibility == View.VISIBLE) {
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            val dateFormatMode = prefs.getInt("DATE_FORMAT_MODE", 1)
            val pattern = when(dateFormatMode) { 0 -> "EEEE, d"; 1 -> "EEEE, d MMMM"; 2 -> "d MMMM, EEEE"; else -> "EEEE, d MMMM" }
            val dateSdf = SimpleDateFormat(pattern, Locale.getDefault())
            var dateStr = dateSdf.format(Date())
            if (dateStr.isNotEmpty()) { dateStr = dateStr.substring(0, 1).uppercase(Locale.getDefault()) + dateStr.substring(1) }
            textDateLarge.text = dateStr
        }
        if (calendarLayout.visibility == View.VISIBLE) drawCustomCalendar(globalTextColor, globalThemeColor)
    }

    private fun drawCustomCalendar(textColor: Int, themeColor: Int) {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val monthFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
        val monthStr = monthFormat.format(calendar.time)
        tvMonthName.text = monthStr.substring(0, 1).uppercase() + monthStr.substring(1)
        tvMonthName.setTextColor(textColor)
        if (currentTypeface != null) tvMonthName.typeface = currentTypeface

        // ТЕНЬ
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val showShadow = prefs.getBoolean("SHOW_TEXT_SHADOW", true)
        val radius = if (showShadow) 8f else 0f
        val dx = if (showShadow) 2f else 0f
        val dy = if (showShadow) 2f else 0f
        val shadowColor = Color.parseColor("#80000000")
        tvMonthName.setShadowLayer(if(showShadow) 12f else 0f, if(showShadow) 4f else 0f, if(showShadow) 4f else 0f, shadowColor)

        calendarHeader.removeAllViews()
        val weekDays = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val cellSize = dpToPx(40)

        for (day in weekDays) {
            val tv = TextView(this)
            tv.text = day
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.argb(150, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
            tv.textSize = 14f
            if (currentTypeface != null) tv.typeface = currentTypeface
            tv.setShadowLayer(radius, dx, dy, shadowColor)
            tv.layoutParams = LinearLayout.LayoutParams(cellSize, ViewGroup.LayoutParams.WRAP_CONTENT)
            calendarHeader.addView(tv)
        }

        calendarGrid.removeAllViews()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeek == 0) dayOfWeek = 7
        val emptyCells = dayOfWeek - 1

        for (i in 0 until emptyCells) {
            val emptyView = View(this)
            emptyView.layoutParams = GridLayout.LayoutParams().apply { width = cellSize; height = cellSize }
            calendarGrid.addView(emptyView)
        }

        for (i in 1..daysInMonth) {
            val tvDay = TextView(this)
            tvDay.text = i.toString()
            tvDay.gravity = Gravity.CENTER
            tvDay.textSize = 16f
            tvDay.layoutParams = GridLayout.LayoutParams().apply { width = cellSize; height = cellSize }
            if (currentTypeface != null) tvDay.typeface = currentTypeface
            tvDay.setShadowLayer(radius, dx, dy, shadowColor)

            if (i == currentDay) {
                val circle = GradientDrawable()
                circle.shape = GradientDrawable.OVAL
                circle.setColor(themeColor)
                tvDay.background = circle
                tvDay.setTextColor(Color.WHITE)
                if (currentTypeface == null) tvDay.typeface = Typeface.DEFAULT_BOLD
            } else {
                tvDay.setTextColor(textColor)
                tvDay.background = null
            }
            calendarGrid.addView(tvDay)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun updateNextAlarm() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val showAlarm = prefs.getBoolean("SHOW_NEXT_ALARM", false)
        if (!showAlarm) { alarmContainer.visibility = View.GONE; return }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val info = alarmManager.nextAlarmClock
        if (info != null) {
            val date = Date(info.triggerTime)
            val format = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            tvNextAlarmTime.text = sdf.format(date)
            alarmContainer.visibility = View.VISIBLE
        } else {
            alarmContainer.visibility = View.GONE
        }
    }

    private fun checkCalendarPermission() { if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 200) } else { readCalendarEvent() } }
    private fun readCalendarEvent() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val projection = arrayOf("title", "dtstart")
                val now = System.currentTimeMillis()
                val selection = "dtstart >= ?"
                val selectionArgs = arrayOf(now.toString())
                val sortOrder = "dtstart ASC LIMIT 1"
                val cursor = contentResolver.query(Uri.parse("content://com.android.calendar/events"), projection, selection, selectionArgs, sortOrder)
                if (cursor != null && cursor.moveToFirst()) {
                    val titleIndex = cursor.getColumnIndex("title")
                    val timeIndex = cursor.getColumnIndex("dtstart")
                    val title = if (titleIndex >= 0) cursor.getString(titleIndex) else "Event"
                    val time = if (timeIndex >= 0) cursor.getLong(timeIndex) else 0L
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeStr = sdf.format(Date(time))
                    withContext(Dispatchers.Main) { tvEvent.text = "$timeStr $title"; tvEvent.visibility = View.VISIBLE }
                    cursor.close()
                } else { withContext(Dispatchers.Main) { tvEvent.text = "No events"; tvEvent.visibility = View.VISIBLE } }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    private fun fetchWeather() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("YANDEX_API_KEY", "")
        if (apiKey.isNullOrEmpty()) { CoroutineScope(Dispatchers.Main).launch { tvWeatherCondition.text = "Enter Key"; tvWeatherTemp.text = "--"; tvWeatherIcon.text = ""; tvLocation.text = "" }; return }
        CoroutineScope(Dispatchers.IO).launch {
            var cityName = getCityName(lat.toDouble(), lon.toDouble())
            try {
                val url = URL("https://api.weather.yandex.ru/v2/forecast?lat=$lat&lon=$lon&lang=ru_RU")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("X-Yandex-Weather-Key", apiKey)
                if (connection.responseCode == 200) {
                    val data = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(data)
                    val fact = json.getJSONObject("fact")
                    val temp = fact.getInt("temp")
                    val condition = fact.getString("condition")
                    if (cityName == "Location") {
                        val geoObject = json.optJSONObject("geo_object")
                        val locality = geoObject?.optJSONObject("locality")?.optString("name")
                        val district = geoObject?.optJSONObject("district")?.optString("name")
                        if (!locality.isNullOrEmpty()) cityName = locality else if (!district.isNullOrEmpty()) cityName = district
                    }
                    val conditionRu = translateCondition(condition)
                    val icon = getWeatherIcon(condition)
                    withContext(Dispatchers.Main) {
                        tvWeatherTemp.text = "${if(temp > 0) "+" else ""}$temp°"
                        tvWeatherCondition.text = conditionRu
                        tvWeatherIcon.text = icon
                        tvLocation.text = "$cityName ➤"
                        tvMiniWeather.text = "$icon ${if(temp > 0) "+" else ""}$temp° $conditionRu"
                    }
                } else { withContext(Dispatchers.Main) { if (connection.responseCode == 403) tvWeatherCondition.text = "Invalid Key" else tvWeatherCondition.text = "Err: ${connection.responseCode}" } }
            } catch (e: Exception) { withContext(Dispatchers.Main) { tvWeatherCondition.text = "No Net" } }
        }
    }
    private fun getCityName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) { val address = addresses[0]; address.locality ?: address.subAdminArea ?: address.adminArea ?: "Location" } else "Location"
        } catch (e: Exception) { "Location" }
    }
    private fun getWeatherIcon(cond: String): String { return when(cond) { "clear" -> "☀️"; "partly-cloudy" -> "⛅"; "cloudy" -> "☁️"; "overcast" -> "☁️"; "drizzle" -> "🌦️"; "light-rain" -> "🌧️"; "rain" -> "🌧️"; "moderate-rain" -> "🌧️"; "heavy-rain" -> "⛈️"; "showers" -> "☔"; "wet-snow" -> "🌨️"; "light-snow" -> "🌨️"; "snow" -> "❄️"; "hail" -> "🌨️"; "thunderstorm" -> "⚡"; else -> "🌡️" } }
    private fun translateCondition(cond: String): String { return when(cond) { "clear" -> "Ясно"; "partly-cloudy" -> "Малооблачно"; "cloudy" -> "Облачно с проясн."; "overcast" -> "Пасмурно"; "drizzle" -> "Морось"; "light-rain" -> "Небольшой дождь"; "rain" -> "Дождь"; "moderate-rain" -> "Дождь"; "heavy-rain" -> "Ливень"; "showers" -> "Ливень"; "wet-snow" -> "Мокрый снег"; "light-snow" -> "Снег"; "snow" -> "Снег"; "hail" -> "Град"; "thunderstorm" -> "Гроза"; else -> cond } }

    private fun applyAppearance(prefs: android.content.SharedPreferences) {
        val textColorId = prefs.getInt("TEXT_COLOR_ID", 0)
        val defaultTextColors = intArrayOf(Color.WHITE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED)
        val textColor = prefs.getInt("TEXT_COLOR_VALUE_$textColorId", defaultTextColors.getOrElse(textColorId) { Color.WHITE })
        globalTextColor = textColor

        setClockColor(textColor)
        textDateSmall.setTextColor(textColor)
        textDateLarge.setTextColor(textColor)
        tvMiniWeather.setTextColor(textColor)
        tvWeatherTemp.setTextColor(textColor)
        tvWeatherCondition.setTextColor(textColor)
        tvWeatherIcon.setTextColor(textColor)
        tvTrackTitle.setTextColor(textColor)
        tvTrackArtist.setTextColor(Color.LTGRAY)
        tvEvent.setTextColor(textColor)
        tvLocation.setTextColor(textColor)
        btnPrev.setColorFilter(textColor)
        btnPlayPause.setColorFilter(textColor)
        btnNext.setColorFilter(textColor)
        tvNextAlarmTime.setTextColor(textColor)
        ivAlarmIcon.setColorFilter(textColor)

        // Сброс фильтров
        seekBarMusic.thumb.setTintList(null)
        seekBarMusic.progressDrawable.setTintList(null)
        ivAlbumArt.clearColorFilter()

        // Тень
        val showShadow = prefs.getBoolean("SHOW_TEXT_SHADOW", true)
        applyTextShadow(showShadow)

        val themeColorId = prefs.getInt("THEME_COLOR_ID", 0)
        val defaultThemeColors = intArrayOf(Color.parseColor("#448AFF"), Color.parseColor("#FF5252"), Color.parseColor("#69F0AE"), Color.parseColor("#FFFF00"), Color.parseColor("#E040FB"))
        val themeColor = prefs.getInt("THEME_COLOR_VALUE_$themeColorId", defaultThemeColors.getOrElse(themeColorId) { Color.parseColor("#448AFF") })
        globalThemeColor = themeColor

        drawCustomCalendar(textColor, themeColor)

        val bgImageUriStr = prefs.getString("BG_IMAGE_URI", null)
        val bgAlpha = prefs.getInt("BG_IMAGE_ALPHA", 255)
        val showPanels = prefs.getBoolean("SHOW_PANELS", false)
        val isBgFill = prefs.getBoolean("BG_SCALE_FILL", true)

        if (bgImageUriStr != null) {
            try {
                val uri = Uri.parse(bgImageUriStr)
                ivBackground.visibility = View.VISIBLE
                ivBackground.setImageURI(uri)
                if (isBgFill) ivBackground.scaleType = ImageView.ScaleType.CENTER_CROP else ivBackground.scaleType = ImageView.ScaleType.FIT_CENTER
                if (showPanels) ivBackground.imageAlpha = 255 else ivBackground.imageAlpha = bgAlpha
                ivBlurLeft.setImageURI(uri); ivBlurLeft.imageAlpha = bgAlpha
                alignBackgrounds(uri, isBgFill)
                ivBlurRight.setImageURI(uri); ivBlurRight.imageAlpha = bgAlpha
                rootLayout.setBackgroundColor(Color.BLACK)
            } catch (e: Exception) { hideBackgroundImages(); applyBackgroundColor(prefs) }
        } else { hideBackgroundImages(); applyBackgroundColor(prefs) }

        val panelColorId = prefs.getInt("PANEL_COLOR_ID", 0)
        val defaultPanelColors = intArrayOf(Color.WHITE, Color.parseColor("#444444"), Color.parseColor("#B71C1C"), Color.parseColor("#E65100"), Color.parseColor("#1B5E20"))
        val defaultPanelColor = defaultPanelColors.getOrElse(panelColorId) { Color.WHITE }
        val panelBaseColor = prefs.getInt("PANEL_COLOR_VALUE_$panelColorId", defaultPanelColor)
        val alphaPercent = prefs.getInt("PANEL_ALPHA", 30)
        val alpha255 = (alphaPercent * 255) / 100
        val blurRadius = prefs.getInt("PANEL_BLUR_RADIUS", 0)
        applyPanelStyle(panelBaseColor, alpha255, blurRadius)
    }

    private fun alignBackgrounds(uri: Uri, isFill: Boolean) {
        ivBlurLeft.post {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val screenWidth = rootLayout.width.toFloat()
                    val screenHeight = rootLayout.height.toFloat()
                    val bitmapWidth = bitmap.width.toFloat()
                    val bitmapHeight = bitmap.height.toFloat()
                    val scale: Float
                    var dx = 0f; var dy = 0f
                    if (isFill) {
                        if (bitmapWidth * screenHeight > screenWidth * bitmapHeight) { scale = screenHeight / bitmapHeight; dx = (screenWidth - bitmapWidth * scale) * 0.5f } else { scale = screenWidth / bitmapWidth; dy = (screenHeight - bitmapHeight * scale) * 0.5f }
                    } else {
                        scale = min(screenWidth / bitmapWidth, screenHeight / bitmapHeight)
                        dx = (screenWidth - bitmapWidth * scale) * 0.5f; dy = (screenHeight - bitmapHeight * scale) * 0.5f
                    }
                    val matrixLeft = Matrix(); matrixLeft.setScale(scale, scale); matrixLeft.postTranslate(dx, dy); matrixLeft.postTranslate(-ivBlurLeft.left.toFloat(), -ivBlurLeft.top.toFloat())
                    ivBlurLeft.imageMatrix = matrixLeft; ivBlurLeft.setImageBitmap(bitmap)
                    val matrixRight = Matrix(); matrixRight.setScale(scale, scale); matrixRight.postTranslate(dx, dy); matrixRight.postTranslate(-ivBlurRight.left.toFloat(), -ivBlurRight.top.toFloat())
                    ivBlurRight.imageMatrix = matrixRight; ivBlurRight.setImageBitmap(bitmap)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun hideBackgroundImages() { ivBackground.visibility = View.GONE; ivBlurLeft.setImageDrawable(null); ivBlurRight.setImageDrawable(null) }
    private fun applyPanelStyle(color: Int, alpha: Int, blurRadius: Int) {
        val finalColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        (viewBgLeft.background.mutate() as? GradientDrawable)?.setColor(finalColor)
        (viewBgRight.background.mutate() as? GradientDrawable)?.setColor(finalColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val effect = if (blurRadius > 0) { val r = (blurRadius / 2.0f).coerceAtLeast(0.1f); RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR) } else null
            ivBlurLeft.setRenderEffect(effect); ivBlurRight.setRenderEffect(effect)
        }
    }

    private fun applyCustomFont(prefs: android.content.SharedPreferences) {
        val fontUriString = prefs.getString("CUSTOM_FONT_URI", null)
        if (fontUriString != null) {
            try {
                val uri = Uri.parse(fontUriString)
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val fd: FileDescriptor = pfd.fileDescriptor
                    val typeface = Typeface.Builder(fd).build()
                    pfd.close()
                    currentTypeface = typeface
                    setClockTypeface(typeface)
                    textDateSmall.typeface = typeface; textDateLarge.typeface = typeface
                    tvWeatherTemp.typeface = typeface; tvWeatherCondition.typeface = typeface; tvWeatherIcon.typeface = typeface
                    batteryStatusText.typeface = typeface
                    tvTrackTitle.typeface = typeface; tvTrackArtist.typeface = typeface
                    tvEvent.typeface = typeface; tvLocation.typeface = typeface; tvNextAlarmTime.typeface = typeface; tvMiniWeather.typeface = typeface
                    tvMonthName.typeface = typeface
                    if (calendarLayout.visibility == View.VISIBLE) drawCustomCalendar(globalTextColor, globalThemeColor)
                }
            } catch (e: Exception) { resetFonts() }
        } else { resetFonts() }
    }

    private fun resetFonts() {
        currentTypeface = null
        val defaultTypeface = Typeface.DEFAULT
        val clockTypeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        setClockTypeface(clockTypeface)
        textDateSmall.typeface = defaultTypeface; textDateLarge.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        tvMonthName.typeface = Typeface.DEFAULT_BOLD
        tvWeatherTemp.typeface = clockTypeface; tvWeatherCondition.typeface = defaultTypeface; tvWeatherIcon.typeface = defaultTypeface
        batteryStatusText.typeface = defaultTypeface
        tvTrackTitle.typeface = defaultTypeface; tvTrackArtist.typeface = defaultTypeface
        tvEvent.typeface = defaultTypeface; tvLocation.typeface = defaultTypeface; tvNextAlarmTime.typeface = defaultTypeface; tvMiniWeather.typeface = defaultTypeface
        if (calendarLayout.visibility == View.VISIBLE) drawCustomCalendar(globalTextColor, globalThemeColor)
    }

    private fun updateBatteryColor() {
        if (!isNightModeActive || !isNightFilterEnabled) {
            batteryStatusText.setTextColor(Color.GREEN)
        } else {
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            val nightColor = prefs.getInt("NIGHT_FILTER_COLOR", Color.parseColor("#9EA793"))
            batteryStatusText.setTextColor(nightColor)
        }
    }

    private fun setSwitcherText(switcher: TextSwitcher, text: String) { val current = (switcher.currentView as? TextView)?.text?.toString() ?: ""; if (current != text) { if (text.isEmpty()) { (switcher.nextView as TextView).text = ""; switcher.showNext() } else { switcher.setText(text) } } }
    private fun setClockSize(sizeSp: Float) { val switchers = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2); for (ts in switchers) { for (i in 0 until ts.childCount) { (ts.getChildAt(i) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp) } }; tvSeparator.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp) }
    private fun setClockColor(color: Int) { val switchers = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2); for (ts in switchers) { for (i in 0 until ts.childCount) { (ts.getChildAt(i) as TextView).setTextColor(color) } }; tvSeparator.setTextColor(color) }
    private fun setClockTypeface(tf: Typeface) { val switchers = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2); for (ts in switchers) { for (i in 0 until ts.childCount) { (ts.getChildAt(i) as TextView).typeface = tf; (ts.getChildAt(i) as TextView).fontFeatureSettings = "tnum" } }; tvSeparator.typeface = tf }
    private fun applyBackgroundColor(prefs: android.content.SharedPreferences) { val bgColorId = prefs.getInt("BG_COLOR_ID", 0); val defaultBgColors = intArrayOf(Color.parseColor("#333333"), Color.parseColor("#888888"), Color.parseColor("#0D47A1"), Color.parseColor("#B71C1C"), Color.parseColor("#1B5E20")); val defaultColor = defaultBgColors.getOrElse(bgColorId) { Color.DKGRAY }; val bgColor = prefs.getInt("BG_COLOR_VALUE_$bgColorId", defaultColor); rootLayout.setBackgroundColor(bgColor) }
    private fun getThemeResId(id: Int): Int { return when(id) { 1 -> R.style.Theme_ChargingClock_Red; 2 -> R.style.Theme_ChargingClock_Green; 3 -> R.style.Theme_ChargingClock_Yellow; 4 -> R.style.Theme_ChargingClock_Purple; else -> R.style.Theme_ChargingClock_Blue } }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (isAutoBrightnessEnabled && event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            if (lux < 5f) {
                if (!isNightModeActive) {
                    isNightModeActive = true
                    val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    val nightLevel = prefs.getInt("NIGHT_BRIGHTNESS_LEVEL", 25)
                    val targetBrightness = if (nightLevel < 1) 0.01f else nightLevel / 100f
                    animateScreenBrightness(targetBrightness)
                    if (isNightFilterEnabled) applyNightText()
                }
            } else {
                if (isNightModeActive) {
                    isNightModeActive = false
                    val systemBrightness = getSystemBrightness()
                    animateScreenBrightness(systemBrightness) { val params = window.attributes; params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; window.attributes = params }
                    val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    applyAppearance(prefs)
                }
            }
        }
    }

    private fun applyNightText() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val color = prefs.getInt("NIGHT_FILTER_COLOR", Color.parseColor("#9EA793"))

        ivBackground.visibility = View.GONE
        ivBlurLeft.visibility = View.GONE
        ivBlurRight.visibility = View.GONE
        rootLayout.setBackgroundColor(Color.BLACK)

        setClockColor(color)
        textDateSmall.setTextColor(color); textDateLarge.setTextColor(color); tvMiniWeather.setTextColor(color)
        tvWeatherTemp.setTextColor(color); tvWeatherCondition.setTextColor(color); tvWeatherIcon.setTextColor(color)
        tvTrackTitle.setTextColor(color); tvTrackArtist.setTextColor(color)
        tvEvent.setTextColor(color); tvLocation.setTextColor(color); tvNextAlarmTime.setTextColor(color)
        batteryStatusText.setTextColor(color)
        btnPrev.setColorFilter(color); btnPlayPause.setColorFilter(color); btnNext.setColorFilter(color)
        ivAlarmIcon.setColorFilter(color)

        seekBarMusic.thumb.setTint(color)
        seekBarMusic.progressDrawable.setTint(color)
        ivAlbumArt.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)

        globalTextColor = color
        globalThemeColor = color
        drawCustomCalendar(globalTextColor, globalThemeColor)
        applyPanelStyle(color, 50, 0)
    }

    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100); return }
        tvWeatherCondition.text = "Locating..."
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (lastLocation != null) { lat = lastLocation.latitude.toString(); lon = lastLocation.longitude.toString(); fetchWeather() }
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : LocationListener { override fun onLocationChanged(location: Location) { lat = location.latitude.toString(); lon = location.longitude.toString(); fetchWeather() }; override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}; override fun onProviderEnabled(provider: String) {}; override fun onProviderDisabled(provider: String) {} }, null)
    }

    private fun animateScreenBrightness(targetValue: Float, onAnimationEnd: (() -> Unit)? = null) {
        val currentBrightness = if (window.attributes.screenBrightness < 0) getSystemBrightness() else window.attributes.screenBrightness
        brightnessAnimator?.cancel()
        brightnessAnimator = ValueAnimator.ofFloat(currentBrightness, targetValue).apply {
            duration = 1000
            addUpdateListener { animator -> val value = animator.animatedValue as Float; val layoutParams = window.attributes; layoutParams.screenBrightness = value; window.attributes = layoutParams }
            addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) { onAnimationEnd?.invoke() } })
            start()
        }
    }

    private fun getSystemBrightness(): Float { return try { val curBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS); curBrightness / 255f } catch (e: Exception) { 0.5f } }
    private fun resetBrightnessAndFilter() { val layoutParams = window.attributes; layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; window.attributes = layoutParams; if (isNightModeActive) { isNightModeActive = false; val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE); applyAppearance(prefs) } }
    private fun updateInitialBatteryStatus() { val intent: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); val status: Int = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1; val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL; if (isCharging) { batteryStatusText.text = "Заряжается"; batteryStatusText.setTextColor(Color.GREEN) } else { batteryStatusText.text = "Работа от батареи"; batteryStatusText.setTextColor(Color.RED) } }
    override fun onDestroy() { super.onDestroy(); try { unregisterReceiver(powerReceiver) } catch (e: Exception) {} }
    private fun hideSystemUI() { window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) }
}