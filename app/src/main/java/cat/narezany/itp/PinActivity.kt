package cat.narezany.itp

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class PinActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var enteredPin = ""
    private var isSettingPin = false
    private var firstPin: String? = null // used during set flow
    private val pinLength = 4
    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ic = WindowCompat.getInsetsController(window, window.decorView)
        ic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ic.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_pin)
        prefs = getSharedPreferences("tweaks_prefs", MODE_PRIVATE)

        isSettingPin = intent.getBooleanExtra("setting_pin", false)
        val title = findViewById<TextView>(R.id.pin_title)
        val subtitle = findViewById<TextView>(R.id.pin_subtitle)

        if (isSettingPin) {
            title.text = "Создайте PIN"
            subtitle.text = "Вводите 4 цифры"
        } else {
            title.text = "Введите PIN"
            subtitle.text = ""
        }

        // Build dots
        val dotsContainer = findViewById<LinearLayout>(R.id.dots_container)
        for (i in 0 until pinLength) {
            val dot = View(this).apply {
                val size = (14 * resources.displayMetrics.density).toInt()
                val margin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                setBackgroundResource(R.drawable.pin_dot_empty)
            }
            dots.add(dot)
            dotsContainer.addView(dot)
        }

        // Wire keypad
        val keys = mapOf(
            R.id.key_0 to "0", R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3",
            R.id.key_4 to "4", R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7",
            R.id.key_8 to "8", R.id.key_9 to "9"
        )
        keys.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { onDigit(digit) }
        }
        findViewById<Button>(R.id.key_del).setOnClickListener { onDelete() }
    }

    private fun onDigit(d: String) {
        if (enteredPin.length >= pinLength) return
        enteredPin += d
        updateDots()
        if (enteredPin.length == pinLength) {
            onPinComplete()
        }
    }

    private fun onDelete() {
        if (enteredPin.isEmpty()) return
        enteredPin = enteredPin.dropLast(1)
        updateDots()
    }

    private fun updateDots() {
        for (i in 0 until pinLength) {
            dots[i].setBackgroundResource(if (i < enteredPin.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty)
        }
    }

    private fun onPinComplete() {
        if (isSettingPin) {
            if (firstPin == null) {
                // First entry
                firstPin = enteredPin
                enteredPin = ""
                updateDots()
                findViewById<TextView>(R.id.pin_title).text = "Повторите PIN"
                findViewById<TextView>(R.id.pin_subtitle).text = "Введите PIN ещё раз"
            } else {
                // Confirm
                if (enteredPin == firstPin) {
                    prefs.edit().putString("pin_code", enteredPin).apply()
                    finish()
                } else {
                    // Mismatch — shake and reset
                    enteredPin = ""
                    firstPin = null
                    updateDots()
                    findViewById<TextView>(R.id.pin_title).text = "Создайте PIN"
                    val sub = findViewById<TextView>(R.id.pin_subtitle)
                    sub.text = "PIN не совпали, попробуйте снова"
                    sub.setTextColor(Color.parseColor("#EF4444"))
                }
            }
        } else {
            // Unlock mode
            val savedPin = prefs.getString("pin_code", null)
            if (enteredPin == savedPin) {
                MainActivity.pinUnlocked = true
                finish()
            } else {
                enteredPin = ""
                updateDots()
                val sub = findViewById<TextView>(R.id.pin_subtitle)
                sub.text = "Неверный PIN"
                sub.setTextColor(Color.parseColor("#EF4444"))
                // Shake animation on dots
                val container = findViewById<LinearLayout>(R.id.dots_container)
                container.animate().translationX(20f).setDuration(50).withEndAction {
                    container.animate().translationX(-20f).setDuration(50).withEndAction {
                        container.animate().translationX(10f).setDuration(50).withEndAction {
                            container.animate().translationX(0f).setDuration(50).start()
                        }.start()
                    }.start()
                }.start()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isSettingPin) {
            super.onBackPressed() // Allow cancel during set
        }
        // Don't allow back during unlock
    }
}
