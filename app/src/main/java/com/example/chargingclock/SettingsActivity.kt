package com.example.chargingclock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
    private lateinit var tvFontLabel: TextView
    private lateinit var tvAlphaLabel: TextView
    private lateinit var containerBgColor: LinearLayout // Ссылка на контейнер цветов

    // Лаунчер для шрифта
    private val fontPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            val fileName = getFileName(it)
            prefs.edit().putString("CUSTOM_FONT_URI", it.toString()).putString("CUSTOM_FONT_NAME", fileName).apply()
            tvFontLabel.text = "Шрифт: $fileName"
            Toast.makeText(this, "Шрифт выбран!", Toast.LENGTH_SHORT).show()
        }
    }

    // Лаунчер для фона (картинки)
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            prefs.edit().putString("BG_IMAGE_URI", it.toString()).apply()
            Toast.makeText(this, "Фон выбран!", Toast.LENGTH_SHORT).show()
            updateBgColorVisibility() // Скрываем выбор цвета
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        // UI Элементы
        val rgTextColor = findViewById<RadioGroup>(R.id.rgTextColor)
        val rgBgColor = findViewById<RadioGroup>(R.id.rgBgColor)
        containerBgColor = findViewById(R.id.containerBgColor) // Инициализация

        val btnSelectBgImage = findViewById<Button>(R.id.btnSelectBgImage)
        val btnResetBgImage = findViewById<Button>(R.id.btnResetBgImage)
        val seekBarAlpha = findViewById<SeekBar>(R.id.seekBarAlpha)
        tvAlphaLabel = findViewById(R.id.tvAlphaLabel)

        val switchAutoBrightness = findViewById<Switch>(R.id.switchAutoBrightness)
        val switchAutoLocation = findViewById<Switch>(R.id.switchAutoLocation)
        val switchShowBattery = findViewById<Switch>(R.id.switchShowBattery) // НОВОЕ

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSelectFont = findViewById<Button>(R.id.btnSelectFont)
        val btnResetFont = findViewById<Button>(R.id.btnResetFont)
        tvFontLabel = findViewById(R.id.tvFontLabel)

        val containerThemeColor = findViewById<LinearLayout>(R.id.containerThemeColor)
        val rgThemeColor = findViewById<RadioGroup>(R.id.rgThemeColor)

        val rgDisplayMode = findViewById<RadioGroup>(R.id.rgDisplayMode)
        val containerSplitOptions = findViewById<LinearLayout>(R.id.containerSplitOptions)
        val containerClockOptions = findViewById<LinearLayout>(R.id.containerClockOptions)
        val switchShowDate = findViewById<Switch>(R.id.switchShowDate)

        val rgSideContent = findViewById<RadioGroup>(R.id.rgSideContent)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // === 1. ЦВЕТА И ФОН ===

        val savedTextColor = prefs.getInt("TEXT_COLOR_ID", 0)
        (rgTextColor.getChildAt(savedTextColor) as? RadioButton)?.isChecked = true
        rgTextColor.setOnCheckedChangeListener { _, checkedId ->
            val colorId = rgTextColor.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("TEXT_COLOR_ID", colorId).apply()
        }

        val savedBgColor = prefs.getInt("BG_COLOR_ID", 0)
        (rgBgColor.getChildAt(savedBgColor) as? RadioButton)?.isChecked = true
        rgBgColor.setOnCheckedChangeListener { _, checkedId ->
            val colorId = rgBgColor.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("BG_COLOR_ID", colorId).apply()
        }

        btnSelectBgImage.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }
        btnResetBgImage.setOnClickListener {
            prefs.edit().remove("BG_IMAGE_URI").apply()
            Toast.makeText(this, "Фон сброшен", Toast.LENGTH_SHORT).show()
            updateBgColorVisibility() // Показываем выбор цвета снова
        }

        val savedAlpha = prefs.getInt("BG_IMAGE_ALPHA", 255)
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

        // Установка начальной видимости цветов фона
        updateBgColorVisibility()

        // === 2. ДРУГИЕ НАСТРОЙКИ ===

        switchAutoBrightness.isChecked = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        switchAutoBrightness.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("AUTO_BRIGHTNESS", isChecked).apply() }

        switchAutoLocation.isChecked = prefs.getBoolean("AUTO_LOCATION", false)
        switchAutoLocation.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("AUTO_LOCATION", isChecked).apply() }

        // --- БАТАРЕЯ ---
        switchShowBattery.isChecked = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        switchShowBattery.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_BATTERY_STATUS", isChecked).apply() }

        etApiKey.setText(prefs.getString("YANDEX_API_KEY", ""))
        etApiKey.addTextChangedListener { prefs.edit().putString("YANDEX_API_KEY", it.toString().trim()).apply() }

        val savedFontName = prefs.getString("CUSTOM_FONT_NAME", "Стандартный")
        tvFontLabel.text = "Шрифт: $savedFontName"
        btnSelectFont.setOnClickListener { fontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream")) }
        btnResetFont.setOnClickListener {
            prefs.edit().remove("CUSTOM_FONT_URI").remove("CUSTOM_FONT_NAME").apply()
            tvFontLabel.text = "Шрифт: Стандартный"
            Toast.makeText(this, "Сброшено", Toast.LENGTH_SHORT).show()
        }

        // === 3. РЕЖИМЫ И ТЕМЫ ===

        fun updateVisibility() {
            val isSplit = prefs.getBoolean("IS_SPLIT_MODE", false)
            val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)
            if (isSplit) {
                containerSplitOptions.visibility = View.VISIBLE
                containerClockOptions.visibility = View.GONE
                containerThemeColor.visibility = if (sideMode == 1) View.VISIBLE else View.GONE
            } else {
                containerSplitOptions.visibility = View.GONE
                containerClockOptions.visibility = View.VISIBLE
                containerThemeColor.visibility = View.GONE
            }
        }

        val isSplit = prefs.getBoolean("IS_SPLIT_MODE", false)
        if (isSplit) rgDisplayMode.check(R.id.rbModeSplit) else rgDisplayMode.check(R.id.rbModeClock)
        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            val splitSelected = (checkedId == R.id.rbModeSplit)
            prefs.edit().putBoolean("IS_SPLIT_MODE", splitSelected).apply()
            updateVisibility()
        }

        switchShowDate.isChecked = prefs.getBoolean("SHOW_CLOCK_DATE", true)
        switchShowDate.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("SHOW_CLOCK_DATE", isChecked).apply() }

        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)
        (rgSideContent.getChildAt(sideMode) as? RadioButton)?.isChecked = true
        rgSideContent.setOnCheckedChangeListener { _, checkedId ->
            val newMode = rgSideContent.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("SIDE_CONTENT_MODE", newMode).apply()
            updateVisibility()
        }

        val savedThemeColor = prefs.getInt("THEME_COLOR_ID", 0)
        (rgThemeColor.getChildAt(savedThemeColor) as? RadioButton)?.isChecked = true
        rgThemeColor.setOnCheckedChangeListener { _, checkedId ->
            val colorId = rgThemeColor.indexOfChild(findViewById(checkedId))
            prefs.edit().putInt("THEME_COLOR_ID", colorId).apply()
        }

        updateVisibility()
        btnBack.setOnClickListener { finish() }
    }

    // Логика скрытия выбора цвета фона
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
        tvAlphaLabel.text = "Прозрачность картинки: $percent%"
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
        return result ?: "Файл"
    }
}