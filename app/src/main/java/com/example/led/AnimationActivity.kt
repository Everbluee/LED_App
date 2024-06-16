package com.example.led

import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
//import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.gridlayout.widget.GridLayout
import java.util.Random

class AnimationActivity : AppCompatActivity() {
    private var bluetoothStateReceiver: BluetoothBR = BluetoothBR()
    private lateinit var animFirst: Button
    private lateinit var animSecond: Button
    private lateinit var animThird: Button
    private lateinit var animFourth: Button
    private lateinit var animFifth: Button
    private lateinit var animSixth: Button
    private lateinit var backButton: ImageButton
    private val LED_SIZE_DP = 20
    private lateinit var sharedPref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_animation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LogStorage.logs.add("Przejście do ekranu animacji ( $this )")

        backButton = findViewById(R.id.backButtonAnim)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        animFirst = findViewById(R.id.animFirst)
        animSecond = findViewById(R.id.animSecond)
        animThird = findViewById(R.id.animThird)
        animFourth = findViewById(R.id.animFourth)
        animFifth = findViewById(R.id.animFifth)
        animSixth = findViewById(R.id.animSixth)

        animFirst.setOnClickListener {
            showAnimationDialog(::animateFirst,"Animation 1 data" )
        }
        animSecond.setOnClickListener {
            showAnimationDialog(::animateSecond, "Animation 2 data")
        }
        animThird.setOnClickListener {
            showAnimationDialog(::animateThird, "Animation 3 data")
        }
        animFourth.setOnClickListener {
            showAnimationDialog(::animateFourth, "Animation 4 data")
        }
        animFifth.setOnClickListener {
            showAnimationDialog(::animateFifth, "Animation 5 data")
        }
        animSixth.setOnClickListener {
            showAnimationDialog(::animateSixth, "Animation 6 data")
        }
        sharedPref = getSharedPreferences("LED_PREFERENCES", Context.MODE_PRIVATE)
    }
    private fun handleAnimationButtonClick(animationData: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("animation_data", animationData)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun showAnimationDialog(animation: (List<ImageView>) -> Unit, animationData: String) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.animation_popup, null)
        dialog.setContentView(view)

        // Dodanie rozmycia tła
        val rootView = findViewById<ConstraintLayout>(R.id.main)
        rootView.alpha = 0.5f

        dialog.setOnDismissListener {
            rootView.alpha = 1.0f
        }
        val sendDataButton: Button = view.findViewById(R.id.sendDataButton)

        sendDataButton.setOnClickListener {
            handleAnimationButtonClick(animationData)
        }
        val ledMatrix: GridLayout = view.findViewById(R.id.led_matrix)
        val sizePx = (LED_SIZE_DP * resources.displayMetrics.density).toInt()
        val leds = ArrayList<ImageView>()

        // Dodawanie ImageView do GridLayout
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                val led = ImageView(this)
                val params = GridLayout.LayoutParams()
                params.width = sizePx
                params.height = sizePx
                params.setMargins(1, 1, 1, 1)
                led.layoutParams = params
                led.setBackgroundColor(Color.GRAY)
                ledMatrix.addView(led)
                leds.add(led)
            }
        }

        animation(leds)

        dialog.show()
    }
    private fun animateFirst(leds: List<ImageView>) {
        val handler = android.os.Handler()
        val animationDuration = 500L
        val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE)
        val animateLEDsRunnable = object : Runnable {
            var index = 0

            override fun run() {
                val row = index / 16
                val col = index % 16

                leds[index].setBackgroundColor(if ((row + col) % 2 == 0) selectedColor else Color.GRAY)

                index++
                if (index < leds.size) {
                    handler.postDelayed(this, animationDuration)
                }
            }
        }

        handler.post(animateLEDsRunnable)
    }

    private fun animateSecond(leds: List<ImageView>) {
        val handler = android.os.Handler()
        val blinkInterval = 500L // (w milisekundach)
        val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE)

        var isColorOn = false

        val blinkRunnable = object : Runnable {
            override fun run() {
                isColorOn = !isColorOn
                for (i in 0 until leds.size) {
                    val led = leds[i]
                    val row = i / 16
                    val col = i % 16
                    if ((row + col) % 2 == 0) {
                        led.setBackgroundColor(if (isColorOn) selectedColor else Color.GRAY)
                    } else {
                        led.setBackgroundColor(if (!isColorOn) selectedColor else Color.GRAY)
                    }
                }
                handler.postDelayed(this, blinkInterval)
            }
        }

        handler.post(blinkRunnable)
    }
    private fun animateThird(leds: List<ImageView>) {
        val handler = android.os.Handler()
        val animationDuration = 500L // Czas trwania animacji (w milisekundach)
        val jumpInterval = 100L // Czas trwania jednego przeskoku (w milisekundach)
        val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE)

        var currentIndex = 0

        val animationRunnable = object : Runnable {
            override fun run() {
                val prevIndex = (currentIndex - 1 + leds.size) % leds.size
                leds[prevIndex].setBackgroundColor(Color.GRAY)

                val currentLed = leds[currentIndex]
                currentLed.setBackgroundColor(selectedColor)

                currentIndex = (currentIndex + 1) % leds.size

                handler.postDelayed(this, jumpInterval)
            }
        }

        handler.post(animationRunnable)
    }

private fun animateFourth(leds: List<ImageView>) {
    val handler = android.os.Handler()
    val blinkInterval = 500L
    val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE) // Pobierz wybrany kolor z preferencji

    val random = Random()

    val blinkRunnable = object : Runnable {
        override fun run() {
            // Wylosowanie sześciu różnych indeksów diod
            val randomIndices = mutableSetOf<Int>()
            while (randomIndices.size < 6) {
                val randomIndex = random.nextInt(leds.size)
                randomIndices.add(randomIndex)
            }

            // Zmiana koloru dla wylosowanych diod
            for (index in randomIndices) {
                val led = leds[index]
                val isOn = random.nextBoolean()
                led.setBackgroundColor(if (isOn) selectedColor else Color.GRAY)
            }

            handler.postDelayed(this, blinkInterval)
        }
    }

    handler.post(blinkRunnable)
}


private fun animateFifth(leds: List<ImageView>) {
    val handler = android.os.Handler()
    val animationDuration = 500L
    val jumpInterval = 100L // Czas trwania jednego przeskoku
    val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE)
    val matrixSize =16
    var currentSize = matrixSize
    var startX = 0
    var startY = 0


    val animationRunnable = object : Runnable {
        override fun run() {
            // Wyłącz poprzedni kwadrat
            for (x in startX until startX + currentSize) {
                leds[startY * matrixSize + x].setBackgroundColor(Color.GRAY) // Górny rząd
                leds[(startY + currentSize - 1) * matrixSize + x].setBackgroundColor(Color.GRAY) // Dolny rząd
            }
            for (y in startY + 1 until startY + currentSize - 1) {
                leds[y * matrixSize + startX].setBackgroundColor(Color.GRAY) // Lewy bok
                leds[y * matrixSize + startX + currentSize - 1].setBackgroundColor(Color.GRAY) // Prawy bok
            }

            // Włącz aktualny kwadrat
            for (x in startX until startX + currentSize) {
                leds[startY * matrixSize + x].setBackgroundColor(selectedColor) // Górny rząd
                leds[(startY + currentSize - 1) * matrixSize + x].setBackgroundColor(selectedColor) // Dolny rząd
            }
            for (y in startY + 1 until startY + currentSize - 1) {
                leds[y * matrixSize + startX].setBackgroundColor(selectedColor) // Lewy bok
                leds[y * matrixSize + startX + currentSize - 1].setBackgroundColor(selectedColor) // Prawy bok
            }

            // Przesuń się do następnego kroku
            startX++
            startY++
            currentSize -= 2

            // Sprawdź warunek zakończenia animacji
            if (currentSize > 0) {
                handler.postDelayed(this, jumpInterval)
            } else {
                // Wróć do stanu początkowego
                currentSize = matrixSize
                startX = 0
                startY = 0
                for (led in leds) {
                    led.setBackgroundColor(Color.GRAY)
                }

                handler.postDelayed(this, jumpInterval)
            }
        }

    }
    handler.post(animationRunnable)
}

private fun animateSixth(leds: List<ImageView>) {
    val handler = android.os.Handler()
    val animationDuration = 500L

    var isOn = true

    val animationRunnable = object : Runnable {
        override fun run() {
            if (isOn) {
                for (led in leds) {
                    led.setBackgroundColor(Color.GRAY)
                }
            } else {
                val selectedColor = sharedPref.getInt("SELECTED_COLOR", Color.WHITE)
                for (led in leds) {
                    led.setBackgroundColor(selectedColor)
                }
            }

            isOn = !isOn

            handler.postDelayed(this, animationDuration)
        }
    }

    handler.post(animationRunnable)
}


    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothStateReceiver)
    }
}
