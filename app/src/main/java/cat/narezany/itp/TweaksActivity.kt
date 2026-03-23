package cat.narezany.itp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class TweaksActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ic = WindowCompat.getInsetsController(window, window.decorView)
        ic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ic.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_tweaks)
        prefs = getSharedPreferences("tweaks_prefs", MODE_PRIVATE)

        // ── Navigation ──
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_go_itp).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }); finish()
        }

        // ── Blur ──
        val switchBlur = findViewById<SwitchCompat>(R.id.switch_blur_profanity)
        switchBlur.isChecked = prefs.getBoolean("blur_profanity", false)
        switchBlur.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("blur_profanity", c).apply() }

        // ── Traffic saver ──
        val switchTraffic = findViewById<SwitchCompat>(R.id.switch_traffic_saver)
        switchTraffic.isChecked = prefs.getBoolean("traffic_saver", false)
        switchTraffic.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("traffic_saver", c).apply() }

        // ── Image download ──
        val switchDl = findViewById<SwitchCompat>(R.id.switch_image_download)
        switchDl.isChecked = prefs.getBoolean("image_download", true)
        switchDl.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("image_download", c).apply() }

        // ── Clickable Links ──
        val switchLinks = findViewById<SwitchCompat>(R.id.switch_clickable_links)
        switchLinks.isChecked = prefs.getBoolean("clickable_links", true)
        switchLinks.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("clickable_links", c).apply() }

        // ── Emoji clan filter ──
        val inputEmojis = findViewById<EditText>(R.id.input_blocked_emojis)
        inputEmojis.setText(prefs.getString("blocked_emojis", ""))
        inputEmojis.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                prefs.edit().putString("blocked_emojis", v.text.toString().trim()).apply()
                v.clearFocus()
                true
            } else false
        }

        // ── Material You ──
        val switchMY = findViewById<SwitchCompat>(R.id.switch_material_you)
        switchMY.isChecked = prefs.getBoolean("material_you", false)
        switchMY.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("material_you", c).apply() }

        // ── PC Version ──
        val switchPc = findViewById<SwitchCompat>(R.id.switch_pc_version)
        switchPc.isChecked = prefs.getBoolean("pc_version", false)
        switchPc.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("pc_version", c).apply() }

        // ── Translator ──
        val switchTr = findViewById<SwitchCompat>(R.id.switch_translator)
        val trSettings = findViewById<LinearLayout>(R.id.translator_settings)
        val switchTrAuto = findViewById<SwitchCompat>(R.id.switch_translator_auto)
        val inputSkipLangs = findViewById<EditText>(R.id.input_skip_langs)
        val inputTargetLang = findViewById<EditText>(R.id.input_target_lang)

        switchTr.isChecked = prefs.getBoolean("translator_enabled", false)
        trSettings.alpha = if (switchTr.isChecked) 1f else 0.4f
        setTranslatorSettingsEnabled(trSettings, switchTr.isChecked)

        switchTr.setOnCheckedChangeListener { _, c ->
            prefs.edit().putBoolean("translator_enabled", c).apply()
            trSettings.alpha = if (c) 1f else 0.4f
            setTranslatorSettingsEnabled(trSettings, c)
        }

        inputSkipLangs.setText(prefs.getString("translator_skip_langs", "ru"))
        inputSkipLangs.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                prefs.edit().putString("translator_skip_langs", v.text.toString().trim()).apply()
                v.clearFocus(); true
            } else false
        }

        inputTargetLang.setText(prefs.getString("translator_target_lang", "ru"))
        inputTargetLang.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = v.text.toString().trim()
                prefs.edit().putString("translator_target_lang", text).apply()
                v.clearFocus(); true
            } else false
        }

        switchTrAuto.isChecked = prefs.getBoolean("translator_auto", false)
        switchTrAuto.setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("translator_auto", c).apply() }

        // ── PIN ──
        val switchPin = findViewById<SwitchCompat>(R.id.switch_pin)
        switchPin.isChecked = prefs.getString("pin_code", null) != null
        switchPin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startActivity(Intent(this, PinActivity::class.java).apply { putExtra("setting_pin", true) })
            } else {
                prefs.edit().remove("pin_code").apply()
                MainActivity.pinUnlocked = false
            }
        }

        // ── Analytics ──
        val switchAnalytics = findViewById<SwitchCompat>(R.id.switch_analytics)
        switchAnalytics.isChecked = prefs.getBoolean("analytics_enabled", true)
        switchAnalytics.setOnCheckedChangeListener { _, c ->
            prefs.edit().putBoolean("analytics_enabled", c).apply()
            Firebase.analytics.setAnalyticsCollectionEnabled(c)
        }

        // ── Subscribe ──
        findViewById<Button>(R.id.btn_subscribe_nrz).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_URL, "https://итд.com/@nrz")
            }); finish()
        }

        // ── Language ──
        val langSpinner = findViewById<Spinner>(R.id.spinner_language)
        val languages = arrayOf("System (Системный)", "English", "Русский", "中文", "Español")
        val langCodes = arrayOf("", "en", "ru", "zh", "es")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        langSpinner.adapter = adapter

        val currentLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val currentLang = when {
            currentLocales.startsWith("en") -> "en"
            currentLocales.startsWith("ru") -> "ru"
            else -> ""
        }
        val currentIndex = langCodes.indexOf(currentLang).coerceAtLeast(0)
        
        // Prevent onItemSelected from firing during initialization
        langSpinner.setSelection(currentIndex, false)
        langSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selectedCode = langCodes[pos]
                if (selectedCode != currentLang) {
                    val localeList = if (selectedCode.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(selectedCode)
                    }
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync PIN toggle
        val switchPin = findViewById<SwitchCompat>(R.id.switch_pin)
        switchPin.isChecked = prefs.getString("pin_code", null) != null
    }

    override fun onPause() {
        super.onPause()
        // Save emoji field on leave
        val inputEmojis = findViewById<EditText>(R.id.input_blocked_emojis)
        prefs.edit().putString("blocked_emojis", inputEmojis.text.toString().trim()).apply()
        // Save translator fields
        val inputSkipLangs = findViewById<EditText>(R.id.input_skip_langs)
        val inputTargetLang = findViewById<EditText>(R.id.input_target_lang)
        prefs.edit()
            .putString("translator_skip_langs", inputSkipLangs.text.toString().trim())
            .putString("translator_target_lang", inputTargetLang.text.toString().trim())
            .apply()
    }

    private fun setTranslatorSettingsEnabled(container: LinearLayout, enabled: Boolean) {
        for (i in 0 until container.childCount) {
            container.getChildAt(i).isEnabled = enabled
            if (container.getChildAt(i) is LinearLayout) {
                val inner = container.getChildAt(i) as LinearLayout
                for (j in 0 until inner.childCount) {
                    inner.getChildAt(j).isEnabled = enabled
                }
            }
        }
    }
}
