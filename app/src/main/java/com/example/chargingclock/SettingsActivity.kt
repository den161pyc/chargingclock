package com.example.chargingclock

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // UI Elements
    private lateinit var tvFontLabel: TextView
    private lateinit var tvAlphaLabel: TextView
    private lateinit var containerBgColor: LinearLayout
    private lateinit var containerRedTint: LinearLayout
    private lateinit var containerNightBrightness: LinearLayout
    private lateinit var containerThemeColor: LinearLayout
    private lateinit var containerSplitOptions: LinearLayout
    private lateinit var containerClockOptions: LinearLayout
    // containerCalendarOptions REMOVED
    private lateinit var containerDateOptions: LinearLayout
    private lateinit var containerPanelSettings: LinearLayout

    private lateinit var switchBgMode: Switch
    private lateinit var switchAutoBrightness: Switch
    private lateinit var switchRedTint: Switch
    private lateinit var seekBarNightBrightness: SeekBar
    private lateinit var tvNightBrightnessValue: TextView
    private lateinit var switchAutoLocation: Switch
    private lateinit var switchShowBattery: Switch

    private lateinit var rgTextColor: RadioGroup
    private lateinit var rgBgColor: RadioGroup
    private lateinit var rgThemeColor: RadioGroup
    private lateinit var rgDisplayMode: RadioGroup
    private lateinit var switchShowDate: Switch
    private lateinit var rgSideContent: RadioGroup
    private lateinit var switchShowNextAlarm: Switch
    private lateinit var switchShowMiniWeather: Switch
    private lateinit var rgDateFormat: RadioGroup

    private lateinit var switchShowPanels: Switch
    private lateinit var tvPanelAlphaLabel: TextView
    private lateinit var seekBarPanelAlpha: SeekBar
    private lateinit var tvPanelBlurLabel: TextView
    private lateinit var seekBarPanelBlur: SeekBar
    private lateinit var rgPanelColor: RadioGroup

    private lateinit var switchBgScaleFill: Switch

    // Standard Palettes (5 colors)
    private val defaultTextColors = intArrayOf(Color.WHITE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED)
    private val defaultBgColors = intArrayOf(Color.parseColor("#333333"), Color.parseColor("#888888"), Color.parseColor("#0D47A1"), Color.parseColor("#B71C1C"), Color.parseColor("#1B5E20"))
    private val defaultPanelColors = intArrayOf(Color.WHITE, Color.parseColor("#444444"), Color.parseColor("#B71C1C"), Color.parseColor("#E65100"), Color.parseColor("#1B5E20"))
    private val defaultThemeColors = intArrayOf(Color.parseColor("#448AFF"), Color.parseColor("#FF5252"), Color.parseColor("#69F0AE"), Color.parseColor("#FFFF00"), Color.parseColor("#E040FB"))

    private val fontPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            val fileName = getFileName(it)
            prefs.edit().putString("CUSTOM_FONT_URI", it.toString()).putString("CUSTOM_FONT_NAME", fileName).apply()
            if (::tvFontLabel.isInitialized) tvFontLabel.text = "Font: $fileName"
            Toast.makeText(this, "Font selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            prefs.edit().putString("BG_IMAGE_URI", it.toString()).apply()
            Toast.makeText(this, "Background selected!", Toast.LENGTH_SHORT).show()
            updateBgColorVisibility()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        initViews()
        setupBackgroundMode()

        setupColorRadioGroup(rgTextColor, "TEXT_COLOR", defaultTextColors)
        setupColorRadioGroup(rgBgColor, "BG_COLOR", defaultBgColors)
        setupColorRadioGroup(rgPanelColor, "PANEL_COLOR", defaultPanelColors)
        setupColorRadioGroup(rgThemeColor, "THEME_COLOR", defaultThemeColors)

        setupOtherSettings()
        setupBrightnessAndSystem()
        setupDisplayMode()
        setupPanelSettings()

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupColorRadioGroup(radioGroup: RadioGroup, keyPrefix: String, defaultColors: IntArray) {
        val savedIndex = prefs.getInt("${keyPrefix}_ID", 0)
        if (savedIndex < radioGroup.childCount) {
            (radioGroup.getChildAt(savedIndex) as RadioButton).isChecked = true
        }

        for (i in 0 until radioGroup.childCount) {
            if (i >= defaultColors.size) break

            val rb = radioGroup.getChildAt(i) as RadioButton
            val colorKey = "${keyPrefix}_VALUE_$i"
            val currentColor = prefs.getInt(colorKey, defaultColors[i])

            updateRadioButtonStyle(rb, currentColor)

            rb.setOnClickListener {
                if (checkColorConflict(keyPrefix, currentColor)) {
                    val oldIndex = prefs.getInt("${keyPrefix}_ID", 0)
                    if (oldIndex < radioGroup.childCount) {
                        (radioGroup.getChildAt(oldIndex) as RadioButton).isChecked = true
                    }
                } else {
                    prefs.edit().putInt("${keyPrefix}_ID", i).apply()
                }
            }

            rb.setOnLongClickListener {
                val activeColor = prefs.getInt(colorKey, defaultColors[i])
                showColorPickerDialog(activeColor) { newColor ->
                    if (checkColorConflict(keyPrefix, newColor)) {
                        return@showColorPickerDialog
                    }
                    prefs.edit().putInt(colorKey, newColor).apply()
                    updateRadioButtonStyle(rb, newColor)
                    rb.isChecked = true
                    prefs.edit().putInt("${keyPrefix}_ID", i).apply()

                    Toast.makeText(this, "Color saved", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val index = group.indexOfChild(group.findViewById(checkedId))
            if (index != -1) {
                prefs.edit().putInt("${keyPrefix}_ID", index).apply()
            }
        }
    }

    private fun updateRadioButtonStyle(rb: RadioButton, color: Int) {
        rb.backgroundTintList = ColorStateList.valueOf(color)
        val tickColor = if (isColorDark(color)) Color.WHITE else Color.BLACK
        rb.foregroundTintList = ColorStateList.valueOf(tickColor)
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun checkColorConflict(changingType: String, newColor: Int): Boolean {
        val bgId = prefs.getInt("BG_COLOR_ID", 0)
        val bgColor = prefs.getInt("BG_COLOR_VALUE_$bgId", Color.parseColor("#333333"))

        val textId = prefs.getInt("TEXT_COLOR_ID", 0)
        val textColor = prefs.getInt("TEXT_COLOR_VALUE_$textId", Color.WHITE)

        val panelId = prefs.getInt("PANEL_COLOR_ID", 0)
        val panelColor = prefs.getInt("PANEL_COLOR_VALUE_$panelId", Color.WHITE)
        val showPanels = prefs.getBoolean("SHOW_PANELS", false)

        val isNewWhite = (newColor == Color.WHITE)
        val isNewBlack = isColorDark(newColor) && (Color.red(newColor) < 30 && Color.green(newColor) < 30 && Color.blue(newColor) < 30)

        if (changingType == "TEXT_COLOR") {
            if (isNewWhite) {
                if (!showPanels && bgColor == Color.WHITE) {
                    Toast.makeText(this, "Can't select white text on white background!", Toast.LENGTH_LONG).show()
                    return true
                }
                if (showPanels && panelColor == Color.WHITE) {
                    Toast.makeText(this, "Can't select white text on white panel!", Toast.LENGTH_LONG).show()
                    return true
                }
            }
            if (isNewBlack) {
                if (!showPanels && isColorDark(bgColor) && (Color.red(bgColor) < 50)) {
                    Toast.makeText(this, "Text won't be visible on dark background!", Toast.LENGTH_LONG).show()
                    return true
                }
                if (showPanels && isColorDark(panelColor) && (Color.red(panelColor) < 50)) {
                    Toast.makeText(this, "Text won't be visible on dark panel!", Toast.LENGTH_LONG).show()
                    return true
                }
            }
        }
        else if (changingType == "BG_COLOR") {
            if (isNewWhite && !showPanels && textColor == Color.WHITE) {
                Toast.makeText(this, "Can't select white background with white text!", Toast.LENGTH_LONG).show()
                return true
            }
            if (isNewBlack && !showPanels && isColorDark(textColor) && (Color.red(textColor) < 50)) {
                Toast.makeText(this, "Can't select dark background with dark text!", Toast.LENGTH_LONG).show()
                return true
            }
        }
        else if (changingType == "PANEL_COLOR" && showPanels) {
            if (isNewWhite && textColor == Color.WHITE) {
                Toast.makeText(this, "Can't select white panel with white text!", Toast.LENGTH_LONG).show()
                return true
            }
            if (isNewBlack && isColorDark(textColor) && (Color.red(textColor) < 50)) {
                Toast.makeText(this, "Can't select dark panel with dark text!", Toast.LENGTH_LONG).show()
                return true
            }
        }
        return false
    }

    private fun setupOtherSettings() {
        findViewById<Button>(R.id.btnSelectBgImage).setOnClickListener { imagePickerLauncher.launch(arrayOf("image/*")) }
        findViewById<Button>(R.id.btnResetBgImage).setOnClickListener {
            prefs.edit().remove("BG_IMAGE_URI").apply()
            Toast.makeText(this, "Background reset", Toast.LENGTH_SHORT).show()
            updateBgColorVisibility()
        }

        switchBgScaleFill = findViewById(R.id.switchBgScaleFill)
        switchBgScaleFill.isChecked = prefs.getBoolean("BG_SCALE_FILL", true)
        switchBgScaleFill.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("BG_SCALE_FILL", isChecked).apply()
        }

        val savedAlpha = prefs.getInt("BG_IMAGE_ALPHA", 255)
        val seekBarAlpha = findViewById<SeekBar>(R.id.seekBarAlpha)
        seekBarAlpha.progress = savedAlpha
        updateAlphaLabel(savedAlpha)
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateAlphaLabel(progress)
                prefs.edit().putInt("BG_IMAGE_ALPHA", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        updateBgColorVisibility()
    }

    private fun showColorPickerDialog(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dpToPx(24)
            setPadding(p, p, p, p)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val preview = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(60))
            setBackgroundColor(initialColor)
            background = GradientDrawable().apply {
                setColor(initialColor)
                setStroke(2, Color.GRAY)
            }
        }
        dialogView.addView(preview)

        val hexInput = EditText(this).apply {
            hint = "HEX (e.g. #FF0000)"
            setText(String.format("#%06X", (0xFFFFFF and initialColor)))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialogView.addView(hexInput)

        var red = Color.red(initialColor)
        var green = Color.green(initialColor)
        var blue = Color.blue(initialColor)

        fun updateFromSliders() {
            val c = Color.rgb(red, green, blue)
            val drawable = GradientDrawable().apply {
                setColor(c)
                setStroke(2, Color.GRAY)
            }
            preview.background = drawable

            val hex = String.format("#%06X", (0xFFFFFF and c))
            if (!hexInput.hasFocus()) hexInput.setText(hex)
        }

        val sbRed = createColorSeekBar(Color.RED, red) { p -> red = p; updateFromSliders() }
        val sbGreen = createColorSeekBar(Color.GREEN, green) { p -> green = p; updateFromSliders() }
        val sbBlue = createColorSeekBar(Color.BLUE, blue) { p -> blue = p; updateFromSliders() }

        dialogView.addView(createLabel("Red"))
        dialogView.addView(sbRed)
        dialogView.addView(createLabel("Green"))
        dialogView.addView(sbGreen)
        dialogView.addView(createLabel("Blue"))
        dialogView.addView(sbBlue)

        hexInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (hexInput.hasFocus()) {
                    try {
                        val color = Color.parseColor(s.toString())
                        red = Color.red(color); sbRed.progress = red
                        green = Color.green(color); sbGreen.progress = green
                        blue = Color.blue(color); sbBlue.progress = blue
                        val drawable = GradientDrawable().apply {
                            setColor(color)
                            setStroke(2, Color.GRAY)
                        }
                        preview.background = drawable
                    } catch (e: Exception) { }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Select Color")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ -> onColorSelected(Color.rgb(red, green, blue)) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createColorSeekBar(tintColor: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            max = 255
            progress = initial
            progressTintList = ColorStateList.valueOf(tintColor)
            thumbTintList = ColorStateList.valueOf(tintColor)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { onChange(progress) }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply { this.text = text; setPadding(0, 20, 0, 0); setTextColor(Color.LTGRAY) }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun initViews() {
        tvFontLabel = findViewById(R.id.tvFontLabel)
        tvAlphaLabel = findViewById(R.id.tvAlphaLabel)

        containerBgColor = findViewById(R.id.containerBgColor)
        containerRedTint = findViewById(R.id.containerRedTint)
        containerNightBrightness = findViewById(R.id.containerNightBrightness)
        containerThemeColor = findViewById(R.id.containerThemeColor)
        containerSplitOptions = findViewById(R.id.containerSplitOptions)
        containerClockOptions = findViewById(R.id.containerClockOptions)

        // containerCalendarOptions removed

        containerDateOptions = findViewById(R.id.containerDateOptions)
        containerPanelSettings = findViewById(R.id.containerPanelSettings)

        switchBgMode = findViewById(R.id.switchBgMode)
        switchAutoBrightness = findViewById(R.id.switchAutoBrightness)
        switchRedTint = findViewById(R.id.switchRedTint)
        seekBarNightBrightness = findViewById(R.id.seekBarNightBrightness)
        tvNightBrightnessValue = findViewById(R.id.tvNightBrightnessValue)
        switchAutoLocation = findViewById(R.id.switchAutoLocation)
        switchShowBattery = findViewById(R.id.switchShowBattery)

        rgTextColor = findViewById(R.id.rgTextColor)
        rgBgColor = findViewById(R.id.rgBgColor)
        rgThemeColor = findViewById(R.id.rgThemeColor)
        rgDisplayMode = findViewById(R.id.rgDisplayMode)
        switchShowDate = findViewById(R.id.switchShowDate)
        rgSideContent = findViewById(R.id.rgSideContent)

        switchShowNextAlarm = findViewById(R.id.switchShowNextAlarm)
        switchShowMiniWeather = findViewById(R.id.switchShowMiniWeather)
        rgDateFormat = findViewById(R.id.rgDateFormat)

        switchShowPanels = findViewById(R.id.switchShowPanels)
        tvPanelAlphaLabel = findViewById(R.id.tvPanelAlphaLabel)
        seekBarPanelAlpha = findViewById(R.id.seekBarPanelAlpha)
        tvPanelBlurLabel = findViewById(R.id.tvPanelBlurLabel)
        seekBarPanelBlur = findViewById(R.id.seekBarPanelBlur)
        rgPanelColor = findViewById(R.id.rgPanelColor)

        switchBgScaleFill = findViewById(R.id.switchBgScaleFill)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        etApiKey.setText(prefs.getString("YANDEX_API_KEY", ""))
        etApiKey.addTextChangedListener { prefs.edit().putString("YANDEX_API_KEY", it.toString().trim()).apply() }

        findViewById<Button>(R.id.btnSelectFont).setOnClickListener { fontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream")) }
        findViewById<Button>(R.id.btnResetFont).setOnClickListener {
            prefs.edit().remove("CUSTOM_FONT_URI").remove("CUSTOM_FONT_NAME").apply()
            tvFontLabel.text = "Font: Standard"
            Toast.makeText(this, "Reset", Toast.LENGTH_SHORT).show()
        }
        val savedFontName = prefs.getString("CUSTOM_FONT_NAME", "Standard")
        tvFontLabel.text = "Font: $savedFontName"
    }

    private fun setupBackgroundMode() {
        switchBgMode.isChecked = prefs.getBoolean("BG_MODE_ENABLED", false)
        switchBgMode.setOnClickListener {
            val isChecked = switchBgMode.isChecked
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    switchBgMode.isChecked = false
                    Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    prefs.edit().putBoolean("BG_MODE_ENABLED", true).apply()
                }
            } else {
                prefs.edit().putBoolean("BG_MODE_ENABLED", false).apply()
            }
        }
    }

    private fun setupBrightnessAndSystem() {
        val isAutoBrightness = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        switchAutoBrightness.isChecked = isAutoBrightness
        updateAutoBrightnessVisibility(isAutoBrightness)

        switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("AUTO_BRIGHTNESS", isChecked).apply()
            updateAutoBrightnessVisibility(isChecked)
        }

        val savedNightBrightness = prefs.getInt("NIGHT_BRIGHTNESS_LEVEL", 1)
        seekBarNightBrightness.progress = savedNightBrightness
        tvNightBrightnessValue.text = "Night Brightness: $savedNightBrightness%"

        seekBarNightBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var value = progress
                if (value < 1) value = 1
                tvNightBrightnessValue.text = "Night Brightness: $value%"
                prefs.edit().putInt("NIGHT_BRIGHTNESS_LEVEL", value).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchRedTint.isChecked = prefs.getBoolean("RED_TINT_ENABLED", false)
        switchRedTint.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("RED_TINT_ENABLED", isChecked).apply() }

        switchAutoLocation.isChecked = prefs.getBoolean("AUTO_LOCATION", false)
        switchAutoLocation.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("AUTO_LOCATION", isChecked).apply() }

        switchShowBattery.isChecked = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        switchShowBattery.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_BATTERY_STATUS", isChecked).apply() }
    }

    private fun setupDisplayMode() {
        val isSplit = prefs.getBoolean("IS_SPLIT_MODE", false)
        if (isSplit) rgDisplayMode.check(R.id.rbModeSplit) else rgDisplayMode.check(R.id.rbModeClock)

        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            val splitSelected = (checkedId == R.id.rbModeSplit)
            prefs.edit().putBoolean("IS_SPLIT_MODE", splitSelected).apply()
            updateVisibility()
        }

        switchShowDate.isChecked = prefs.getBoolean("SHOW_CLOCK_DATE", true)
        switchShowDate.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_CLOCK_DATE", isChecked).apply() }

        switchShowNextAlarm.isChecked = prefs.getBoolean("SHOW_NEXT_ALARM", false)
        switchShowNextAlarm.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_NEXT_ALARM", isChecked).apply() }

        switchShowMiniWeather.isChecked = prefs.getBoolean("SHOW_MINI_WEATHER", false)
        switchShowMiniWeather.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_MINI_WEATHER", isChecked).apply() }

        val savedDateFormat = prefs.getInt("DATE_FORMAT_MODE", 1)
        (rgDateFormat.getChildAt(savedDateFormat) as? RadioButton)?.isChecked = true
        rgDateFormat.setOnCheckedChangeListener { _, checkedId ->
            val formatIndex = rgDateFormat.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("DATE_FORMAT_MODE", formatIndex).apply()
        }

        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)
        (rgSideContent.getChildAt(sideMode) as? RadioButton)?.isChecked = true
        rgSideContent.setOnCheckedChangeListener { _, checkedId ->
            val newMode = rgSideContent.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("SIDE_CONTENT_MODE", newMode).apply()
            updateVisibility()
        }

        updateVisibility()
    }

    private fun setupPanelSettings() {
        val showPanels = prefs.getBoolean("SHOW_PANELS", false)
        switchShowPanels.isChecked = showPanels
        containerPanelSettings.visibility = if (showPanels) View.VISIBLE else View.GONE

        val bgAlphaVisibility = if (showPanels) View.GONE else View.VISIBLE
        tvAlphaLabel.visibility = bgAlphaVisibility
        findViewById<SeekBar>(R.id.seekBarAlpha).visibility = bgAlphaVisibility

        switchShowPanels.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SHOW_PANELS", isChecked).apply()
            containerPanelSettings.visibility = if (isChecked) View.VISIBLE else View.GONE

            val newVisibility = if (isChecked) View.GONE else View.VISIBLE
            tvAlphaLabel.visibility = newVisibility
            findViewById<SeekBar>(R.id.seekBarAlpha).visibility = newVisibility
        }

        val panelAlpha = prefs.getInt("PANEL_ALPHA", 30)
        seekBarPanelAlpha.max = 100
        seekBarPanelAlpha.progress = panelAlpha
        tvPanelAlphaLabel.text = "Transparency: $panelAlpha%"

        seekBarPanelAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPanelAlphaLabel.text = "Transparency: $progress%"
                prefs.edit().putInt("PANEL_ALPHA", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val panelBlur = prefs.getInt("PANEL_BLUR_RADIUS", 0)
        seekBarPanelBlur.max = 100
        seekBarPanelBlur.progress = panelBlur
        tvPanelBlurLabel.text = "Blur Effect: $panelBlur%"

        seekBarPanelBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPanelBlurLabel.text = "Blur Effect: $progress%"
                prefs.edit().putInt("PANEL_BLUR_RADIUS", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateVisibility() {
        val isSplit = prefs.getBoolean("IS_SPLIT_MODE", false)
        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)
        if (isSplit) {
            containerSplitOptions.visibility = View.VISIBLE
            containerClockOptions.visibility = View.GONE

            containerThemeColor.visibility = if (sideMode == 1) View.VISIBLE else View.GONE
            containerDateOptions.visibility = if (sideMode == 0) View.VISIBLE else View.GONE
        } else {
            containerSplitOptions.visibility = View.GONE
            containerClockOptions.visibility = View.VISIBLE
            containerThemeColor.visibility = View.GONE
            containerDateOptions.visibility = View.GONE
        }
    }

    private fun updateAutoBrightnessVisibility(isEnabled: Boolean) {
        if (isEnabled) {
            containerRedTint.visibility = View.VISIBLE
            containerNightBrightness.visibility = View.VISIBLE
        } else {
            containerRedTint.visibility = View.GONE
            containerNightBrightness.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            if (prefs.getBoolean("BG_MODE_ENABLED", false)) {
                prefs.edit().putBoolean("BG_MODE_ENABLED", false).apply()
                switchBgMode.isChecked = false
            }
        }
    }

    private fun updateBgColorVisibility() {
        val hasBgImage = prefs.getString("BG_IMAGE_URI", null) != null
        if (hasBgImage) {
            containerBgColor.visibility = View.GONE
        } else {
            containerBgColor.visibility = View.VISIBLE
        }
    }

    private fun updateAlphaLabel(progress: Int) {
        val percent = (progress / 255f * 100).toInt()
        tvAlphaLabel.text = "Image Transparency: $percent%"
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            } catch (e: Exception) { e.printStackTrace() } finally { cursor?.close() }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "File"
    }
}