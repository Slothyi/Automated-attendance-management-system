package com.example.attendancepro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancepro.R
import com.example.attendancepro.models.HistoryItem

class RecentAttendanceAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<RecentAttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivStatusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.date
        
        // Format time properly if it's ISO
        var displayTime = item.time ?: ""
        if (displayTime.contains("T")) {
            try {
                var timeStr = item.time!!
                if (timeStr.contains(".")) {
                    timeStr = timeStr.substringBefore(".")
                } else if (timeStr.contains("+")) {
                    timeStr = timeStr.substringBefore("+")
                }
                val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                sdfIn.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = sdfIn.parse(timeStr)
                if (date != null) {
                    val sdfOut = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    sdfOut.timeZone = java.util.TimeZone.getDefault()
                    displayTime = sdfOut.format(date)
                }
            } catch (e: Exception) {}
        }
        holder.tvTime.text = displayTime
        
        holder.tvStatus.text = item.status
        
        if (item.status.equals("Absent", ignoreCase = true)) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252"))
            holder.tvStatus.setBackgroundColor(Color.parseColor("#1AFF5252"))
            holder.ivStatusIcon.setImageResource(R.drawable.ic_close)
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#FF5252"))
        } else {
            holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(R.color.present_green))
            holder.tvStatus.setBackgroundColor(Color.parseColor("#1A00C853"))
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
            holder.ivStatusIcon.setColorFilter(holder.itemView.context.resources.getColor(R.color.present_green))
        }
    }

    override fun getItemCount() = items.size
}
