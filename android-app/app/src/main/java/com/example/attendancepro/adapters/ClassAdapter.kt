package com.example.attendancepro.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.activities.ClassStudentsActivity
import com.example.attendancepro.models.ClassItem
import com.google.android.material.card.MaterialCardView

class ClassAdapter(

    private val context: Context,

    private val list: List<ClassItem>,

    private val onClassSelected: (ClassItem) -> Unit

) : RecyclerView.Adapter<ClassAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val tvClassName: TextView =
            view.findViewById(R.id.tvClassName)

        val tvDepartment: TextView =
            view.findViewById(R.id.tvDepartment)

        val tvStudents: TextView =
            view.findViewById(R.id.tvStudents)

        val tvPresent: TextView =
            view.findViewById(R.id.tvPresent)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(context)
            .inflate(
                R.layout.item_class,
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

        holder.tvDepartment.text =
            "${item.department} | Sem ${item.semester}"

        holder.tvStudents.text =
            "Students: ${item.student_count}"

        holder.tvPresent.text =
            "Present: ${item.present_count}"

        val card =
            holder.itemView as MaterialCardView

        // Highlight selected card
        if (position == selectedPosition) {

            card.alpha = 1f

            card.setCardBackgroundColor(
                Color.parseColor("#204D67")
            )

        } else {

            card.alpha = 1f

            card.setCardBackgroundColor(
                Color.parseColor("#163449")
            )
        }

        // Single tap = select class
        holder.itemView.setOnClickListener {

            val oldPos = selectedPosition

            val adapterPosition = holder.bindingAdapterPosition

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return@setOnClickListener
            }

            selectedPosition = adapterPosition

            notifyItemChanged(oldPos)

            notifyItemChanged(selectedPosition)

            onClassSelected(item)
        }

        // Long press = open students
        holder.itemView.setOnLongClickListener {

            val intent = Intent(
                context,
                ClassStudentsActivity::class.java
            )

            intent.putExtra(
                "CLASS_ID",
                item.class_id
            )

            intent.putExtra(
                "CLASS_NAME",
                item.class_name
            )

            context.startActivity(intent)

            true
        }
    }

    override fun getItemCount(): Int {

        return list.size
    }
}
