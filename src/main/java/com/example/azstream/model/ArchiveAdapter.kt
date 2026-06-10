package com.example.azstream.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.azstream.R

class ArchiveAdapter(
    private val items: List<ArchiveItem>,
    private val onTimeClick: (ArchiveItem) -> Unit
): RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archive, parent, false)
        return ArchiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        holder.bind(items[position], onTimeClick)
    }

    override fun getItemCount(): Int = items.size

    class ArchiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.icon)
        private val name: TextView = itemView.findViewById(R.id.name)

        fun bind(item: ArchiveItem, onItemClick: (ArchiveItem) -> Unit) {
            when (item) {
                is ArchiveItem.Folder -> {
                    icon.text = "📁"
                    name.text = item.name
                }
                is ArchiveItem.Video -> {
                    icon.text = "🎬"
                    name.text = item.name
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}