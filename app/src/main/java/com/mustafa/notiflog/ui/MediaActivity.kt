package com.mustafa.notiflog.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.mustafa.notiflog.data.AppDatabase
import com.mustafa.notiflog.data.MediaFileEntity
import com.mustafa.notiflog.databinding.ActivityMediaBinding
import kotlinx.coroutines.launch
import java.io.File

class MediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaBinding
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(applicationContext).mediaDao()

        adapter = MediaAdapter { item -> openMedia(item) }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter

        dao.getAll().observe(this) { list ->
            adapter.submitList(list)
            binding.emptyView.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.btnClearMedia.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tüm medyayı sil")
                .setMessage("Yedeklenen tüm fotoğraf/video kayıtları silinecek. Emin misin?")
                .setPositiveButton("Sil") { _, _ ->
                    lifecycleScope.launch { dao.clearAll() }
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }
    }

    private fun openMedia(item: MediaFileEntity) {
        val file = File(item.localBackupPath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val mime = if (item.mediaType == "video") "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Aç"))
    }
}
