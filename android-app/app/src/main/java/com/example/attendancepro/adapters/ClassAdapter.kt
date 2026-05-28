package com.example.attendancepro.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.activities.ClassStudentsActivity
import com.example.attendancepro.models.ClassItem

class ClassAdapter(

    private val context: Context,

    private val list: List<ClassItem>

) : RecyclerView.Adapter<ClassAdapter.ViewHolder>() {

    // =========================
    // 📦 VIEW HOLDER
    // =========================
    class ViewHolder(view: View)

        : RecyclerView.ViewHolder(view) {

        // ✅ CLASS NAME
        val tvClassName: TextView =

            view.findViewById(
                R.id.tvClassName
            )

        // ✅ DEPARTMENT
        val tvDepartment: TextView =

            view.findViewById(
                R.id.tvDepartment
            )

        // ✅ TOTAL STUDENTS
        val tvStudents: TextView =

            view.findViewById(
                R.id.tvStudents
            )

        // ✅ PRESENT COUNT
        val tvPresent: TextView =

            view.findViewById(
                R.id.tvPresent
            )
    }

    // =========================
    // 🏗️ CREATE VIEW
    // =========================
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(
            context
        ).inflate(

            R.layout.item_class,

            parent,

            false
        )

        return ViewHolder(view)
    }

    // =========================
    // 🔄 BIND DATA
    // =========================
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        // ✅ CLASS NAME
        holder.tvClassName.text =

            item.class_name

        // ✅ DEPARTMENT
        holder.tvDepartment.text =

            "${item.department} | " +

                    "Sem ${item.semester}"

        // ✅ TOTAL STUDENTS
        holder.tvStudents.text =

            "Students: ${item.student_count}"

        // ✅ PRESENT COUNT
        holder.tvPresent.text =

            "Present: ${item.present_count}"

        // =========================
        // 👆 CLICK EVENT
        // =========================
        holder.itemView.setOnClickListener {

            val intent = Intent(

                context,

                ClassStudentsActivity::class.java
            )

            // ✅ SEND CLASS ID
            intent.putExtra(

                "CLASS_ID",

                item.class_id
            )

            // ✅ SEND CLASS NAME
            intent.putExtra(

                "CLASS_NAME",

                item.class_name
            )

            context.startActivity(intent)
        }
    }

    // =========================
    // 📊 TOTAL ITEMS
    // =========================
    override fun getItemCount(): Int {

        return list.size
    }
}