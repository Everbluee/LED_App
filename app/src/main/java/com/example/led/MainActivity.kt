package com.example.led

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.util.UUID

//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothStateReceiver: BluetoothBR
    private lateinit var bluetoothCoroutineScope: CoroutineScope

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothSocket: BluetoothSocket

    private lateinit var sendButton: Button
    private lateinit var colorButton: Button
    private lateinit var animButton: Button
    private lateinit var infoButton: Button
    private lateinit var connectButton: Button

    private lateinit var dataInput: EditText
    private lateinit var macAddress: String
    // UUID do nawiązywania połączenia z mikrokontrolerem
    private val deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("OnCreate", "onCreate Evoked")
        LogStorage.logs.add(resources.getString(R.string.log_first_msg))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothStateReceiver = BluetoothBR()
        bluetoothCoroutineScope = CoroutineScope(Dispatchers.IO)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter  = bluetoothManager.adapter

        dataInput = findViewById(R.id.dataInput)
        var dataToSend: String = dataInput.text.toString()
        //*********** BUTTONS ***************

        // Obsługa przycisku wysyłania danych
        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            sendData(dataToSend)
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
        //Obsługa przycisku infoButton (przeniesienie do aktywności InfoActivity)
        infoButton = findViewById(R.id.infoButton)
        infoButton.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }
        connectButton = findViewById(R.id.connectButton)
        connectButton.setOnClickListener {
            Log.i("BluetoothOP", "Aktywność uruchomiona, łączenie z urządzeniem Bluetooth")
            connectToDevice()
        }

        //*************** BLUETOOTH *******************

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
            || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1)
                Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane")
            }
        else {
            Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywne")
        }

        Log.i("OnCreate", "Poza checkiem z uprawnieniami. Sprawdzam czy adapter jest aktywny")
        val enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth has been enabled, you can proceed with your operations here
                Log.i("Bluetooth", "Bluetooth włączony")
                // doSomeOperations()
                connectButton.isEnabled = true
            } else {
                // Bluetooth enabling was either cancelled or failed
                Log.e("Bluetooth", "Nie można włączyć Bluetooth")
                connectButton.isEnabled = false
            }
        }

        // Sprawdzenie czy Bluetooth jest włączone
        if (bluetoothAdapter?.isEnabled == false) {
            Log.i("BluetoothConnection", "Nawiązuję połączenie z urządzeniem Bluetooth")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            connectButton.isEnabled = true
            Log.i("BluetoothConnection", "Nawiązano połączenie z urządzeniem Bluetooth")
        } else {
            connectButton.isEnabled = true
        }


    }

    fun getBluetoothDevice(): BluetoothDevice? {
        return try {
            if (!::macAddress.isInitialized) {
                throw UninitializedPropertyAccessException("MAC address has not been initialized.")
            }
            bluetoothAdapter?.getRemoteDevice(macAddress)
        } catch (e: UninitializedPropertyAccessException) {
            println("Error: $e")
            null
        }
    }

    // Metoda do nawiązywania połączenia z urządzeniem Bluetooth
    private fun connectToDevice() {
        bluetoothCoroutineScope.launch {
            try {
                val device: BluetoothDevice? = getBluetoothDevice()
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(deviceUUID)
                    ?: throw IOException("Bluetooth socket is null")
                if (ActivityCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    withContext(Dispatchers.Main) {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN
                            ), 1)
                    }
                    return@launch
                }
                bluetoothSocket.connect()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Połączono z urządzeniem Bluetooth", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Błąd połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
            }
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

    fun updateButtonState(isEnabled: Boolean) {
        if (isEnabled) {
            connectButton.isEnabled = true
        } else {
            connectButton.isEnabled = false
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
                bluetoothSocket.close()
                Log.i("BluetoothDestroy", "Bluetooth Socket zamknięta")
                bluetoothCoroutineScope.cancel() // Anuluj korutynę, aby uniknąć wycieków pamięci
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i("BluetoothDestroy", "Bluetooth Socket nie może być zamknięta")
        }
    }
}
