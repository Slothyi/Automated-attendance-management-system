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

    private val list:
    List<AttendanceReportItem>

) : RecyclerView.Adapter<
        AttendanceReportAdapter.ViewHolder>() {

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val tvName: TextView =
            view.findViewById(R.id.tvName)

        val tvRoll: TextView =
            view.findViewById(R.id.tvRoll)

        val tvStatus: TextView =
            view.findViewById(R.id.tvStatus)

        val tvWeekly: TextView =
            view.findViewById(R.id.tvWeekly)

        val tvMonthly: TextView =
            view.findViewById(R.id.tvMonthly)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(context)
            .inflate(
                R.layout.item_attendance_report,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.tvName.text =
            item.name

        holder.tvRoll.text =
            "Roll: ${item.roll}"

        holder.tvStatus.text =
            item.attendance_status

        when (item.attendance_status) {

            "Present" -> {
                holder.tvStatus.setTextColor(
                    Color.GREEN
                )
            }

            "Absent" -> {
                holder.tvStatus.setTextColor(
                    Color.RED
                )
            }

            else -> {
                holder.tvStatus.setTextColor(
                    Color.GRAY
                )
            }
        }

        holder.tvWeekly.text =
            "Weekly: ${item.weekly_attendance}"

        holder.tvMonthly.text =
            "Monthly: ${item.monthly_attendance}"
    }

    override fun getItemCount(): Int {

        return list.size
    }
}