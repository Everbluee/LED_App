package com.example.led

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions

class BTDeviceBR : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("DevicesReceiver", "Received Intent in receiver")
        val action = intent.action
        when (action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                Log.i("DevicesReceiver", "Discovered a device!")
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (context is MainActivity) {
                        context.devices.add(it)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(context, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                            return
                        }
                        if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                            return
                        }
                        Log.i("DevicesReceiver", "Discovered: ${it.name}")
                        context.arrayAdapter.add("${it.name} (${it.address})")
                        context.arrayAdapter.notifyDataSetChanged()
                    }
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.i("DeviceReceiver", "Action Discovery started")
                if (context is MainActivity)
                    context.discoveryFinished.complete(Unit)
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.i("DeviceReceiver", "Action Discovery finished")
                if (context is MainActivity)
                    context.discoveryFinished.complete(Unit)
            }

            else -> {
                Log.i("DeviceReceiver", "Action not found")
            }
        }
    }
}