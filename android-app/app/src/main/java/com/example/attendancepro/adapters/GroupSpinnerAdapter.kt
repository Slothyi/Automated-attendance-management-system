package com.example.attendancepro.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.example.attendancepro.R

class GroupSpinnerAdapter(
    private val context: Context,
    private val items: List<String>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {

        val view = LayoutInflater.from(context)
            .inflate(
                R.layout.item_spinner_group,
                parent,
                false
            )

        val tv =
            view.findViewById<TextView>(
                R.id.tvSpinnerText
            )

        tv.text = items[position]

        return view
    }

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {

        val view = LayoutInflater.from(context)
            .inflate(
                R.layout.item_spinner_dropdown,
                parent,
                false
            )

        val tv =
            view.findViewById<TextView>(
                R.id.tvSpinnerText
            )

        tv.text = items[position]

        val divider =
            view.findViewById<View>(
                R.id.divider
            )

        divider.visibility =
            if (position == items.size - 1)
                View.GONE
            else
                View.VISIBLE

        return view
    }
}