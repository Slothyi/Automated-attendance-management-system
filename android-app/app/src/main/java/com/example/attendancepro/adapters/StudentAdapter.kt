package com.example.attendancepro.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.models.StudentItem

class StudentAdapter(

    private val context: Context,

    private val list: List<StudentItem>

) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        // 👤 NAME
        val tvName: TextView =
            view.findViewById(R.id.tvName)

        // 🎫 ROLL
        val tvRoll: TextView =
            view.findViewById(R.id.tvRoll)

        // ✅ STATUS
        val tvStatus: TextView =
            view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(context)

            .inflate(

                R.layout.item_student,

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

        // =========================
        // 👤 NAME
        // =========================
        holder.tvName.text =
            item.name

        // =========================
        // 🎫 ROLL
        // =========================
        holder.tvRoll.text =
            item.roll

        // =========================
        // ✅ STATUS
        // =========================
        val status =

            item.attendance_status
                ?: "Absent"

        holder.tvStatus.text =
            status

        // =========================
        // 🎨 STATUS COLOR
        // =========================
        if (

            status.equals(
                "Present",
                ignoreCase = true
            )
        ) {

            holder.tvStatus.setTextColor(
                Color.GREEN
            )

        } else {

            holder.tvStatus.setTextColor(
                Color.RED
            )
        }
    }

    override fun getItemCount(): Int {

        return list.size
    }
}