package com.mustafa.notiflog.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mustafa.notiflog.data.MessageEntity
import com.mustafa.notiflog.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : ListAdapter<MessageEntity, MessageAdapter.VH>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            textConversation.text = item.conversationTitle
            textSender.text = if (item.isGroup) item.sender else ""
            textSender.visibility = if (item.isGroup) android.view.View.VISIBLE else android.view.View.GONE
            textMessage.text = item.text
            textTime.text = dateFormat.format(Date(item.timestamp))
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MessageEntity>() {
            override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
                oldItem == newItem
        }
    }
}
