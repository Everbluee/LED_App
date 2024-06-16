package com.example.led

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.Visibility
import kotlinx.coroutines.*
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var bluetoothStateReceiver: BluetoothBR = BluetoothBR()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothCoroutineScope: CoroutineScope
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothSocket: BluetoothSocket
    lateinit var arrayAdapter: ArrayAdapter<String>
    var devices = mutableListOf<BluetoothDevice>()
    val discoveryFinished = CompletableDeferred<Unit>()

    private lateinit var sendButton: Button
    private lateinit var colourButton: Button
    private lateinit var animButton: Button
    private lateinit var infoButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button

    private lateinit var intentColour: Intent
    private lateinit var intentInfo: Intent
    private lateinit var intentAnimation: Intent

    private lateinit var dataInput: EditText
    private lateinit var macAddress: String

    // UUID do nawiązywania połączenia z mikrokontrolerem
    private val deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Create a BroadcastReceiver for ACTION_FOUND
    private val receiver: BTDeviceBR = BTDeviceBR()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            dataInput = findViewById(R.id.dataInput)
            val dataToSend: String = dataInput.text.toString() + "\n"
            sendData(dataToSend)
            //bluetoothService?.sendData(dataToSend)
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
            if (bluetoothSocket.isConnected) {
                intentInfo.putExtra("bt_mac", macAddress)
                intentInfo.putExtra("bt_conn", "Połączono")
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

            showBluetoothDevicesDialog()
            if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
                bluetoothSocket.close()
                bluetoothCoroutineScope.cancel()
                connectButton.text = getString(R.string.connect_device)
            }
        }
        disconnectButton = findViewById(R.id.disconnectButton)
        disconnectButton.setOnClickListener {
            if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
                bluetoothSocket.close()
                bluetoothCoroutineScope.cancel()
                disconnectButton.visibility = View.INVISIBLE
            }
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
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION), 1)
                Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane")
                LogStorage.logs.add("Uprawnienia Bluetooth są aktywowane... ( $this )")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN), 1)
                    Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywowane")
                    LogStorage.logs.add("Uprawnienia Bluetooth są aktywowane... ( $this )")
                } else {
                    Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywne")
                    LogStorage.logs.add("Uprawnienia Bluetooth są aktywne ( $this )")
                }
            }
        else {
            Log.i("BluetoothPermission", "Uprawnienia Bluetooth są aktywne")
            LogStorage.logs.add("Uprawnienia Bluetooth są aktywne ( $this )")
        }

        Log.i("OnCreate", "Poza checkiem z uprawnieniami. Sprawdzam czy adapter jest aktywny")
        val enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.i("Bluetooth", "Bluetooth włączony")
                LogStorage.logs.add("Bluetooth został uruchomiony ( $this )")
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

        // Register for broadcasts when a device is discovered
        Log.i("DevicesReceiver", "Registering a receiver")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
        }
    }


    private suspend fun discoverDevices(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        arrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, mutableListOf<String>())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
        }
        bluetoothAdapter?.startDiscovery()

        discoveryFinished.await() // Czekamy na zakończenie wyszukiwania
        devices
    }


    private fun showBluetoothDevicesDialog() {
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        coroutineScope.launch {
            val foundDevices = discoverDevices()
            devices.clear()
            devices.addAll(foundDevices)

//            if (foundDevices.isEmpty()) {
//                Toast.makeText(
//                    this@MainActivity,
//                    "Nie znaleziono urządzeń Bluetooth",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return@launch
//            }

            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Wybierz urządzenie Bluetooth")
            builder.setAdapter(arrayAdapter) { _, which ->
                val device = devices[which]
                Log.i("setAdapter", "deviceName: $device")
                macAddress = device.address
                coroutineScope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
                    }
                    bluetoothAdapter?.cancelDiscovery()
                    connectToDevice(device)
                }
            }
            builder.setNegativeButton("Anuluj", null)
            val dialog = builder.create()
            dialog.show()
        }
    }

    private suspend fun connectToDevice(device: BluetoothDevice) {
        withContext(Dispatchers.IO) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID)
                    ?: throw IOException("Bluetooth socket nie istnieje")
                Log.i("bt_socket", bluetoothSocket.toString())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    withContext(Dispatchers.Main) {
                        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                    }
                    return@withContext
                }
                try {
                    bluetoothSocket.connect()
                    Log.i("connectToDevice", "Połączenie z socketem")
                    withContext(Dispatchers.Main) {
                        LogStorage.logs.add("Połączono z urządzeniem Bluetooth ( $this )")
                        Toast.makeText(
                            this@MainActivity,
                            "Połączono z urządzeniem Bluetooth", Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: IOException) {
                    Log.e("bt_socket", e.message.toString())
                    e.printStackTrace()
                    try {
                        Log.i("bt_socket", "trying fallback...")
                        bluetoothSocket =
                            (device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                                .invoke(device, 1) as BluetoothSocket)
                        bluetoothSocket.connect()
                        Log.i("connectToDevice", "Połączenie z socketem")
                        withContext(Dispatchers.Main) {
                            LogStorage.logs.add("Połączono z urządzeniem Bluetooth: ${device.name} " +
                                    "($macAddress) ( $this )")
                            Toast.makeText(
                                this@MainActivity,
                                "Połączono z urządzeniem Bluetooth", Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("bt_socket", "Nie można nawiązać połączenia: ${e.message.toString()}")
                        e.printStackTrace()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    LogStorage.logs.add("Błąd połączenia z urządzeniem Bluetooth ( $this )")
                    Toast.makeText(
                        this@MainActivity,
                        "Błąd połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
            }
        }
        disconnectButton.visibility = View.VISIBLE
        connectButton.text = getString(R.string.disconnect_device)
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

    // Metoda do wysyłania danych przez Bluetooth
    private fun readData() : String {
        if (::bluetoothSocket.isInitialized && bluetoothSocket.isConnected) {
            try {
                val buffer = ByteArray(1024)
                val data = bluetoothSocket.inputStream.read(buffer)
                if (data != -1) {
                    val receivedData = buffer.copyOf(data).toString()
                    LogStorage.logs.add("Dane odebrane: $receivedData ( $this )")
                    Toast.makeText(this, "Dane odebrane: $receivedData", Toast.LENGTH_SHORT).show()
                    return receivedData
                } else {
                    Log.e("BufferData", "Koniec strumienia danych")
                }
            } catch (e: IOException) {
                LogStorage.logs.add("Błąd podczas odbierania danych ( $this )")
                Toast.makeText(this, "Błąd podczas odbierania danych", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            LogStorage.logs.add("Nie można odebrać danych, brak połącznia Bluetooth ( $this )")
            Toast.makeText(this, "Nie można odebrać danych, brak połączenia Bluetooth", Toast.LENGTH_SHORT).show()
        }
        return "-"
    }

    // Aktualizacja stanu przycisku do łączenia się z urządzeniem
    fun updateButtonState(isEnabled: Boolean) {
        connectButton.isEnabled = isEnabled
    }

    override fun onResume() {
        super.onResume()
        val filter2 = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter2)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(receiver)
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
