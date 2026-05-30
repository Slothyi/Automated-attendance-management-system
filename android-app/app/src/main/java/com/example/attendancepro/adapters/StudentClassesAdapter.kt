package com.example.attendancepro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancepro.R
import com.example.attendancepro.models.StudentClassItem

class StudentClassesAdapter(
    private var classesList: List<StudentClassItem>
) : RecyclerView.Adapter<StudentClassesAdapter.ClassViewHolder>() {

    class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClassName: TextView = view.findViewById(R.id.tvClassName)
        val tvAttendedCount: TextView = view.findViewById(R.id.tvAttendedCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val item = classesList[position]
        holder.tvClassName.text = item.class_name
        holder.tvAttendedCount.text = "Attended: ${item.attended_count} times"
    }

    override fun getItemCount(): Int = classesList.size

    fun updateData(newList: List<StudentClassItem>) {
        classesList = newList
        notifyDataSetChanged()
    }
}
