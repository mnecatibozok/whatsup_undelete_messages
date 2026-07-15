package com.mustafa.notiflog.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mustafa.notiflog.data.MediaFileEntity
import com.mustafa.notiflog.databinding.ItemMediaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaAdapter(
    private val onClick: (MediaFileEntity) -> Unit
) : ListAdapter<MediaFileEntity, MediaAdapter.VH>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))
    private val scope = CoroutineScope(Dispatchers.Main)

    inner class VH(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        var loadJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.loadJob?.cancel()
        holder.binding.apply {
            root.setOnClickListener { onClick(item) }
            textFileName.text = item.fileName
            textDate.text = dateFormat.format(Date(item.dateAddedMillis))
            videoBadge.visibility = if (item.mediaType == "video") android.view.View.VISIBLE else android.view.View.GONE
            imageThumbnail.setImageBitmap(null)
        }
        holder.loadJob = scope.launch {
            val bmp = withContext(Dispatchers.IO) { loadThumbnail(item) }
            holder.binding.imageThumbnail.setImageBitmap(bmp)
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.loadJob?.cancel()
    }

    private fun loadThumbnail(item: MediaFileEntity): Bitmap? {
        return try {
            if (item.mediaType == "image") {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(item.localBackupPath, opts)
            } else {
                ThumbnailUtils.createVideoThumbnail(
                    item.localBackupPath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MediaFileEntity>() {
            override fun areItemsTheSame(oldItem: MediaFileEntity, newItem: MediaFileEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaFileEntity, newItem: MediaFileEntity) =
                oldItem == newItem
        }
    }
}
