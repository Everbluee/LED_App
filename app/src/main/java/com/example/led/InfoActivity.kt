package com.example.led

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class InfoActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
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
        }

        // Inicjalizacja RecyclerView
        logAdapter = LogAdapter(LogStorage.logs)
        logRecyclerView = findViewById(R.id.logRecyclerView)
        logRecyclerView.adapter = logAdapter
        logRecyclerView.layoutManager = LinearLayoutManager(this)

        // Przewijanie RecyclerView na dół, gdy jest aktualizowany
        logRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            logRecyclerView.post {
                logRecyclerView.smoothScrollToPosition(logAdapter.itemCount - 1)
            }
        }
        // Dodawanie przykładowych logów
        addLogs()
    }

    private fun addLogs() {
        for(i in 1..5) {
            LogStorage.logs.add("Log $i")
        }
    }
}