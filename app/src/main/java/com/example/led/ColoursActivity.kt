package com.example.led

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ColoursActivity : AppCompatActivity() {
    private var bluetoothStateReceiver: BluetoothBR = BluetoothBR()

    private lateinit var backPress: ImageButton
    private lateinit var colorWheel: ImageView
    private lateinit var colorIndicator: ImageView
    private lateinit var selectedColorTextView: TextView
    private lateinit var colorDisplay: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_colours)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LogStorage.logs.add("Przejście do ekranu ( $this )")

        //Obsługa przycisku colorButton (przeniesienie do aktywności ColoursActivity)
        colorWheel = findViewById(R.id.colorWheel)
        colorIndicator = findViewById(R.id.colorIndicator)
        selectedColorTextView = findViewById(R.id.selectedColorTextView)
        colorDisplay = findViewById(R.id.colorDisplay)
        backPress = findViewById(R.id.backButtonAnim)
        val redButton: Button = findViewById(R.id.redButton)
        val greenButton: Button = findViewById(R.id.greenButton)
        val blueButton: Button = findViewById(R.id.blueButton)
        val whiteButton: Button = findViewById(R.id.whiteButton)
        colorWheel.setOnTouchListener { v, event ->
            val centerX = colorWheel.width / 2f * 1.05
            val centerY = colorWheel.height / 2f *1.1
            val radius = Math.min(colorWheel.width, colorWheel.height) / 2f *0.9f
            val touchX = event.x
            val touchY = event.y

            // odległość między środkiem koła a punktem dotknięcia
            val distance = Math.sqrt(((touchX - centerX) * (touchX - centerX) + (touchY - centerY) * (touchY - centerY)).toDouble())
            val scaledDistance = if (distance > radius) {
                radius
            } else {
                distance
            }

            val angle = Math.toDegrees(
                Math.atan2(
                    (touchY - centerY).toDouble(),
                    (touchX - centerX)
                )
            )

            var adjustedAngle = angle
            if (adjustedAngle < 0) {
                adjustedAngle += 360
            }

            val angleInRadians = Math.toRadians(adjustedAngle)

            val newX = centerX + scaledDistance.toFloat() * Math.cos(angleInRadians).toFloat()
            val newY = centerY + scaledDistance.toFloat() * Math.sin(angleInRadians).toFloat()

            colorIndicator.x = newX.toFloat() - colorIndicator.width / 2
            colorIndicator.y = newY.toFloat() - colorIndicator.height / 2

            // Przeliczanie pozycji dotknięcia na współrzędne w przeskalowanej bitmapie
            val bitmap = (colorWheel.drawable as BitmapDrawable).bitmap
            val scaledX = (newX * (bitmap.width.toFloat() / colorWheel.width)).toInt()
            val scaledY = (newY * (bitmap.height.toFloat() / colorWheel.height)).toInt()

            // Pobieranie koloru piksela w tym punkcie
            val pixel = bitmap.getPixel(scaledX, scaledY)

            // Wyświetlanie koloru
            val colorString = String.format("#%06X", 0xFFFFFF and pixel)
            selectedColorTextView.text = colorString
            colorDisplay.setBackgroundColor(pixel)

            true
        }

        redButton.setOnClickListener {
            val color = 0xFFFF0000.toInt()
            updateSelectedColor(color)
        }
        greenButton.setOnClickListener {
            val color = 0xFF00FF00.toInt()
            updateSelectedColor(color)
        }
        blueButton.setOnClickListener {
            val color =  0xFF0000FF.toInt()
            updateSelectedColor(color)
        }
        whiteButton.setOnClickListener {
            val color = 0xFFFFFFFF.toInt()
            updateSelectedColor(color)
        }
        backPress.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateSelectedColor(color: Int) {
        val colorString = String.format("#%06X", 0xFFFFFF and color)
        selectedColorTextView.text = colorString
        colorDisplay.setBackgroundColor(color)
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
