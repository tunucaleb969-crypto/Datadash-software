package com.kwame.datadash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kwame.datadash.data.Entry
import com.kwame.datadash.databinding.ItemEntryBinding
import java.util.Locale

class EntryAdapter(
    private val onLongClick: (Entry) -> Boolean
) : ListAdapter<Entry, EntryAdapter.VH>(DIFF) {

    class VH(val binding: ItemEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        holder.binding.textName.text = entry.name
        holder.binding.textSubtitle.text = "${entry.category} • ${entry.phone}"
        holder.binding.textAmount.text = String.format(Locale.getDefault(), "GHS %.2f", entry.amount)
        holder.binding.textDate.text = entry.date
        holder.itemView.setOnLongClickListener { onLongClick(entry) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Entry>() {
            override fun areItemsTheSame(oldItem: Entry, newItem: Entry) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Entry, newItem: Entry) = oldItem == newItem
        }
    }
}
