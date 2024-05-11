package com.example.led

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView

class BluetoothBR : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val logAdapter = LogAdapter(LogStorage.logs)
            if (context is InfoActivity) {
                val logRecyclerView = context.findViewById<RecyclerView>(R.id.logRecyclerView)
                logRecyclerView.adapter = logAdapter
            } else if (context is MainActivity) {
                context.updateButtonState(bluetoothState == BluetoothAdapter.STATE_ON)
            }
            when (bluetoothState) {
                BluetoothAdapter.STATE_ON -> logAdapter.addLog("Bluetooth włączony ( $context )")
                BluetoothAdapter.STATE_OFF -> logAdapter.addLog("Bluetooth wyłączony ( $context )")
                BluetoothAdapter.STATE_TURNING_ON -> logAdapter.addLog("Bluetooth włącza się... ( $context )")
                BluetoothAdapter.STATE_TURNING_OFF -> logAdapter.addLog("Bluetooth wyłącza się... ( $context )")
                else -> {
                    logAdapter.addLog("Bluetooth wykonuje inną akcję... ( $context )")
                }
            }
        }
    }
}