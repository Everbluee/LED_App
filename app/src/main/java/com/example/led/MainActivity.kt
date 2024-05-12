package com.example.led

import android.Manifest
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

class MainActivity : AppCompatActivity() {
    private var bluetoothStateReceiver: BluetoothBR = BluetoothBR()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothCoroutineScope: CoroutineScope
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothSocket: BluetoothSocket

    private lateinit var sendButton: Button
    private lateinit var colourButton: Button
    private lateinit var animButton: Button
    private lateinit var infoButton: Button
    private lateinit var connectButton: Button

    private lateinit var intentColour: Intent
    private lateinit var intentInfo: Intent
    private lateinit var intentAnimation: Intent

    private lateinit var dataInput: EditText
    private lateinit var macAddress: String

    // UUID do nawiązywania połączenia z mikrokontrolerem
    private val deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Inicjalizacja adresu MAC
        //Komp
        //macAddress = "30:C9:AB:E1:40:84"
        //Tel
        //macAddress = "4C:02:20:34:EF:82"

        LogStorage.logs.add("Przejście do ekranu startowego ( $this )")

        bluetoothCoroutineScope = CoroutineScope(Dispatchers.IO)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter  = bluetoothManager.adapter

        //*********** INTENTS ***************
        intentColour = Intent(this, ColoursActivity::class.java)
        intentInfo = Intent(this, InfoActivity::class.java)
        intentAnimation = Intent(this, AnimationActivity::class.java)

        //*********** BUTTONS ***************
        // Obsługa przycisku wysyłania danych
        dataInput = findViewById(R.id.dataInput)
        val dataToSend: String = dataInput.text.toString()
        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            sendData(dataToSend)
        }
        //Obsługa przycisku colorButton (przeniesienie do aktywności ColoursActivity)
        colourButton = findViewById(R.id.colorButton)
        colourButton.setOnClickListener {
            startActivity(intentColour)
        }
        //Obsługa przycisku animButton (przeniesienie do aktywności AnimationActivity)
        animButton = findViewById(R.id.animButton)
        animButton.setOnClickListener {
            startActivity(intentAnimation)
        }
        //Obsługa przycisku infoButton (przeniesienie do aktywności InfoActivity)
        infoButton = findViewById(R.id.infoButton)
        infoButton.setOnClickListener {
            if (::bluetoothSocket.isInitialized) {
                intentInfo.putExtra("bt_mac", macAddress)
                if (bluetoothSocket.isConnected) {
                    intentInfo.putExtra("bt_conn", "Połączono")
                } else {
                    intentInfo.putExtra("bt_conn", "Nie połączono")
                }
            } else {
                intentInfo.putExtra("bt_mac", "brak")
                intentInfo.putExtra("bt_conn", "Nie połączono")
            }
            startActivity(intentInfo)
        }
        connectButton = findViewById(R.id.connectButton)
        connectButton.setOnClickListener {
            Log.i("BluetoothOP", "Łączenie z urządzeniem Bluetooth...")
            LogStorage.logs.add("Łączenie z urządzeniem Bluetooth... ( $this )")
            connectToDevice()
        }

        //*************** BLUETOOTH *******************

        // Sprawdzenie czy urządzenie obsługuje Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Twoje urządzenie nie obsługuje Bluetooth", Toast.LENGTH_SHORT)
                .show()
            Log.e("BluetoothAdapter", "Adapter Bluetooth niemożliwy")
            LogStorage.logs.add("Twoje urządzenie nie obsługuje Bluetooth, nie można utworzyć adaptera ( $this )")
            finish()
            return
        }

        // Sprawdzenie uprawnień do Bluetooth
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT), 1)
                Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane")
                LogStorage.logs.add("Uprawnienia Bluetooth są aktywowane... ( $this )")
            }
        else {
            Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywne")
            LogStorage.logs.add("Uprawnienia Bluetooth są aktywne ( $this )")
        }

        Log.i("OnCreate", "Poza checkiem z uprawnieniami. Sprawdzam czy adapter jest aktywny")
        val enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth has been enabled, you can proceed with your operations here
                Log.i("Bluetooth", "Bluetooth włączony")
                LogStorage.logs.add("Bluetooth został uruchomiony ( $this )")
                // doSomeOperations()
                connectButton.isEnabled = true
            } else {
                // Bluetooth enabling was either cancelled or failed
                Log.e("Bluetooth", "Nie można włączyć Bluetooth")
                LogStorage.logs.add("Odmowa uruchomienia Bluetooth ( $this )")
                connectButton.isEnabled = false
            }
        }

        // Sprawdzenie czy Bluetooth jest włączone
        if (bluetoothAdapter?.isEnabled == false) {
            Log.i("BluetoothConnection", "Próba uruchomienia Bluetooth")
            LogStorage.logs.add("Próba uruchomienia Bluetooth... ( $this )")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            connectButton.isEnabled = true
        }
    }

    private fun getBluetoothDevice(): BluetoothDevice? {
        return try {
            if (!::macAddress.isInitialized) {
                throw UninitializedPropertyAccessException("Adres MAC nie został zainicjalizowany.")
            }
            bluetoothAdapter?.getRemoteDevice(macAddress)
        } catch (e: UninitializedPropertyAccessException) {
            println("Error: $e")
            LogStorage.logs.add("Error: $e ( $this )")
            null
        }
    }

    // Metoda do nawiązywania połączenia z urządzeniem Bluetooth
    private fun connectToDevice() {
        bluetoothCoroutineScope.launch {
            try {
                val device: BluetoothDevice? = getBluetoothDevice()
                bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(deviceUUID)
                    ?: throw IOException("Bluetooth socket nie istnieje")
                Log.i("bt_socket", bluetoothSocket.toString())
                if (ActivityCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    withContext(Dispatchers.Main) {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ), 1)
                    }
                    return@launch
                }
                try {
                    bluetoothSocket.connect()
                    Log.i("connectToDevice", "Połączenie z socketem")
                    withContext(Dispatchers.Main) {
                        LogStorage.logs.add("Połączono z urządzeniem Bluetooth ( $this )")
                        Toast.makeText(this@MainActivity,
                            "Połączono z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.e("bt_socket", e.message.toString())
                    try {
                        Log.i("bt_socket", "trying fallback...")
                        bluetoothSocket = (device.javaClass.getMethod("createRfcommSocket", Int::class.java).invoke(device, 1) as BluetoothSocket)
                        bluetoothSocket.connect()
                    } catch (e: Exception) {
                        Log.e("bt_socket", "Nie można nawiązać połączenia")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    LogStorage.logs.add("Błąd połączenia z urządzeniem Bluetooth ( $this )")
                    Toast.makeText(this@MainActivity,
                        "Błąd połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show()
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
                LogStorage.logs.add("Dane wysłane: $data ( $this )")
                Toast.makeText(this, "Dane wysłane: $data", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                LogStorage.logs.add("Błąd podczas wysyłania danych ( $this )")
                Toast.makeText(this, "Błąd podczas wysyłania danych", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            LogStorage.logs.add("Nie można wysłać danych, brak połącznia Bluetooth ( $this )")
            Toast.makeText(this, "Nie można wysłać danych, brak połączenia Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // Aktualizacja stanu przycisku do łączenia się z urządzeniem
    fun updateButtonState(isEnabled: Boolean) {
        connectButton.isEnabled = isEnabled
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
                LogStorage.logs.add("Bluetooth Socket zamknięta ( $this )")
                Log.i("BluetoothDestroy", "Bluetooth Socket zamknięta")
                bluetoothCoroutineScope.cancel() // Anuluj korutynę, aby uniknąć wycieków pamięci
            }
        } catch (e: IOException) {
            e.printStackTrace()
            LogStorage.logs.add("Bluetooth Socket nie może być zamknięta ( $this )")
            Log.i("BluetoothDestroy", "Bluetooth Socket nie może być zamknięta")
        }
    }
}
