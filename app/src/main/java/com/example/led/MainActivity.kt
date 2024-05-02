package com.example.led

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.util.UUID

//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothSocket: BluetoothSocket

    private lateinit var sendButton: Button
    private lateinit var colorButton: Button
    private lateinit var animButton: Button
    // UUID do nawiązywania połączenia z mikrokontrolerem
    private val deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("OnCreate", "onCreate Evoked")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.i("OnCreate", "View shown")

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter  = bluetoothManager.adapter

        Log.i("OnCreate", "Manager & Adapter Evoked")

        // Sprawdzenie czy urządzenie obsługuje Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Twoje urządzenie nie obsługuje Bluetooth", Toast.LENGTH_SHORT)
                .show()
            finish()
            Log.i("BluetoothAdapter", "Adapter Bluetooth niemożliwy")
            return
        }

        // Sprawdzenie uprawnień do Bluetooth
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1)
                Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane")
            }
        else {
            Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywne")
        }

        Log.i("OnCreate", "Poza checkiem z uprawnieniami. sprawdzam czy adapter jest enabled")
        var enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth has been enabled, you can proceed with your operations here
                doSomeOperations()
                Log.i("Bluetooth", "Bluetooth enabled successfully")
            } else {
                // Bluetooth enabling was either cancelled or failed
                Log.e("Bluetooth", "Failed to enable Bluetooth")
            }
        }

        // Sprawdzenie czy Bluetooth jest włączone
        if (bluetoothAdapter?.isEnabled == false) {
            Log.i("BluetoothConnection", "Nawiązuję połączenie z urządzeniem Bluetooth")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            Log.i("BluetoothConnection", "Nawiązano połączenie z urządzeniem Bluetooth")
        }

        //*********** BUTTONS ***************

        // Obsługa przycisku wysyłania danych
        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            sendData("Twoje dane do wysłania")
        }
        //Obsługa przycisku colorButton (przeniesienie do aktywności ColoursActivity)
        colorButton = findViewById(R.id.colorButton)
        colorButton.setOnClickListener {
            val intent = Intent(this, ColoursActivity::class.java)
            startActivity(intent)
        }
        //Obsługa przycisku animButton (przeniesienie do aktywności AnimationActivity)
        animButton = findViewById(R.id.animButton)
        animButton.setOnClickListener {
            val intent = Intent(this, AnimationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun doSomeOperations() {
        // Tutaj możesz umieścić operacje do wykonania po pomyślnym uruchomieniu aktywności,
        // np. próba nawiązania połączenia Bluetooth
        Log.i("BluetoothOP", "Aktywność uruchomiona, łączenie z urządzeniem Bluetooth")
        connectToDevice()
    }

    // Metoda do nawiązywania połączenia z urządzeniem Bluetooth
    private fun connectToDevice() {
        val device: BluetoothDevice? =
            bluetoothAdapter?.getRemoteDevice("Adres MAC Twojego mikrokontrolera") // Zmień na adres MAC Twojego mikrokontrolera
        try {
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(deviceUUID)
                ?: throw IOException("Bluetooth socket is null")
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission.
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1)
                Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane (2)")
                return
            }
            bluetoothSocket.connect()
            Toast.makeText(this, "Połączono z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Błąd połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Metoda do wysyłania danych przez Bluetooth
    private fun sendData(data: String) {
        if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
            try {
                bluetoothSocket.outputStream.write(data.toByteArray())
                Toast.makeText(this, "Dane wysłane: $data", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Błąd podczas wysyłania danych", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Brak połączenia Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
                bluetoothSocket.close()
                Log.i("BluetoothDestroy", "Bluetooth Socket zamknięta")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i("BluetoothDestroy", "Bluetooth Socket nie może być zamknięta")
        }
    }
}
