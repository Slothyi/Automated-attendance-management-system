package com.example.attendancepro.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.models.AttendanceReportItem

class AttendanceReportAdapter(
    private val context: Context,
    private val list: List<AttendanceReportItem>
) : RecyclerView.Adapter<AttendanceReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRoll: TextView = view.findViewById(R.id.tvRoll)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.item_attendance_report,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvName.text = item.name
        holder.tvRoll.text = item.roll
        holder.tvTime.text = item.time
        holder.tvStatus.text = item.attendance_status

        when (item.attendance_status) {
            "Present" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#4DFF91"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_present)
            }
            "Absent" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#FF5252"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_absent)
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#7B8D9E"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_other)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}