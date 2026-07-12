package com.mustafa.notiflog.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mustafa.notiflog.data.ConversationSummary
import com.mustafa.notiflog.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onClick: (ConversationSummary) -> Unit
) : ListAdapter<ConversationSummary, ConversationAdapter.VH>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))

    inner class VH(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            textName.text = item.conversationTitle
            textLastTime.text = dateFormat.format(Date(item.lastTimestamp))
            if (item.deletedCount > 0) {
                textDeletedBadge.visibility = android.view.View.VISIBLE
                textDeletedBadge.text = "${item.deletedCount} silinmiş mesaj"
            } else {
                textDeletedBadge.visibility = android.view.View.GONE
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ConversationSummary>() {
            override fun areItemsTheSame(oldItem: ConversationSummary, newItem: ConversationSummary) =
                oldItem.conversationTitle == newItem.conversationTitle

            override fun areContentsTheSame(oldItem: ConversationSummary, newItem: ConversationSummary) =
                oldItem == newItem
        }
    }
}
