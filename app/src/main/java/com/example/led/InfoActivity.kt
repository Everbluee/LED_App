package com.example.led

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class InfoActivity : AppCompatActivity(){
    private var bluetoothStateReceiver: BluetoothBR = BluetoothBR()

    private lateinit var backButton: ImageButton
    private lateinit var statusTextView: TextView
    private lateinit var macTextView: TextView
    private lateinit var logAdapter: LogAdapter
    private lateinit var logRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        //Obsługa przycisku backButton
        backButton = findViewById(R.id.backButtonInfo)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            //finish()
        }

        // Inicjalizacja RecyclerView
        logAdapter = LogAdapter(LogStorage.logs)
        logRecyclerView = findViewById(R.id.logRecyclerView)
        logRecyclerView.adapter = logAdapter
        logRecyclerView.layoutManager = LinearLayoutManager(this)

        logAdapter.addLog("Przejście do ekranu informacji ( $this )")
        // Przewijanie RecyclerView na dół, gdy jest aktualizowany
        logRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            logRecyclerView.post {
                logRecyclerView.smoothScrollToPosition(logAdapter.itemCount - 1)
            }
        }

        val intent: Intent = intent
        macTextView = findViewById(R.id.macTV)
        macTextView.text = buildString {
            append(resources.getString(R.string.adres_mac))
            append(' ')
            append(intent.getStringExtra("bt_mac"))
        }
        statusTextView = findViewById(R.id.statusTV)
        statusTextView.text = buildString {
            append(resources.getString(R.string.status))
            append(' ')
            append(intent.getStringExtra("bt_conn"))
        }

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