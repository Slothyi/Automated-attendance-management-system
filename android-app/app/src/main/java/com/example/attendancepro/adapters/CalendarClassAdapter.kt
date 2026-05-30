package com.example.attendancepro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.models.CalendarClass

class CalendarClassAdapter(

    private val list: List<CalendarClass>,

    private val onClassClick: (CalendarClass) -> Unit

) : RecyclerView.Adapter<CalendarClassAdapter.ViewHolder>() {

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val tvClassName: TextView =
            view.findViewById(R.id.tvClassName)

        val tvDate: TextView =
            view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_calendar_class,
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

        holder.tvClassName.text =
            item.class_name

        holder.tvDate.text =
            item.created_at

        holder.itemView.setOnClickListener {

            onClassClick(item)
        }
    }

    override fun getItemCount(): Int {

        return list.size
    }
}
