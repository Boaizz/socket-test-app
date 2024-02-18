package com.example.socketapp
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socketapp.R

class TimeStatusAdapter(private val timeStatusList: List<String>) : RecyclerView.Adapter<TimeStatusAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeStatusText: TextView = view.findViewById(R.id.timeStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.time_status_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.timeStatusText.text = timeStatusList[position]
    }

    override fun getItemCount() = timeStatusList.size
}
