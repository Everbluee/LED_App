package com.example.led

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MotionEvent
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

        colorWheel.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {

                val imageViewCoordinates = IntArray(2)
                colorWheel.getLocationOnScreen(imageViewCoordinates)
                val x = event.rawX - imageViewCoordinates[0]
                val y = event.rawY - imageViewCoordinates[1]

                if (x >= 0 && y >= 0 && x < colorWheel.width && y < colorWheel.height) {
                    val bitmap = (colorWheel.drawable as BitmapDrawable).bitmap
                    val scaledX = (x * (bitmap.width.toFloat() / colorWheel.width)).toInt()
                    val scaledY = (y * (bitmap.height.toFloat() / colorWheel.height)).toInt()

                    try {
                        val pixel = bitmap.getPixel(scaledX, scaledY)
                        val colorString = String.format("#%06X", 0xFFFFFF and pixel)
                        selectedColorTextView.text = colorString
                        colorDisplay.setBackgroundColor(pixel)
                        colorIndicator.x = x - colorIndicator.width / 2
                        colorIndicator.y = y - colorIndicator.height / 2
                    } catch (e: IllegalArgumentException) {

                    }
                }
            }
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
