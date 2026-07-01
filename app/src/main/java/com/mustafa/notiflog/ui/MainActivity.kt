package com.mustafa.notiflog.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustafa.notiflog.data.AppDatabase
import com.mustafa.notiflog.databinding.ActivityMainBinding
import com.mustafa.notiflog.media.MediaWatcherService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var dao: com.mustafa.notiflog.data.MessageDao

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            startMediaWatcherService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dao = AppDatabase.getInstance(applicationContext).messageDao()

        adapter = MessageAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        dao.getAll().observe(this) { list ->
            adapter.submitList(list)
            binding.emptyView.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (query.isBlank()) {
                    dao.getAll().observe(this@MainActivity) { list -> adapter.submitList(list) }
                } else {
                    dao.search(query).observe(this@MainActivity) { list -> adapter.submitList(list) }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tüm kayıtları sil")
                .setMessage("Kaydedilen tüm mesaj geçmişi silinecek. Emin misin?")
                .setPositiveButton("Sil") { _, _ ->
                    lifecycleScope.launch { dao.clearAll() }
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }

        binding.btnMediaGallery.setOnClickListener {
            startActivity(Intent(this, MediaActivity::class.java))
        }

        requestMediaPermissionsIfNeeded()
        checkPermissionAndWarn()
    }

    private fun requestMediaPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        mediaPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startMediaWatcherService() {
        val intent = Intent(this, MediaWatcherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndWarn()
    }

    private fun checkPermissionAndWarn() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(packageName) == true
        binding.warningBanner.visibility = if (isEnabled) android.view.View.GONE else android.view.View.VISIBLE
    }
}
