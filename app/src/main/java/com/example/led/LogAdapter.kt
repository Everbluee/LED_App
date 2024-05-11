package com.example.led

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class LogAdapter (private val logs: MutableList<String>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logText: TextView = itemView.findViewById(R.id.logTextView)
        val logIDText: TextView = itemView.findViewById(R.id.logIDTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        val id = "[$position]"
        holder.logIDText.text = id
        holder.logText.text = log
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    fun addLog(log: String) {
        logs.add(log)
        notifyItemInserted(logs.lastIndex - 1)
    }
}