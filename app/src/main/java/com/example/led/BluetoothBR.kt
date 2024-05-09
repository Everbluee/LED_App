package com.example.led

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothBR : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val isEnabled = bluetoothState == BluetoothAdapter.STATE_ON
            // Tutaj możesz zaktualizować stan przycisku
            (context as MainActivity).updateButtonState(isEnabled)
        }
    }
}