package com.mustafa.notiflog.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.mustafa.notiflog.data.AppDatabase
import com.mustafa.notiflog.data.MediaFileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * MediaStore'u (Images/Video) izleyip WhatsApp klasörüne düşen yeni
 * dosyaları anında app'in kendi private klasörüne kopyalar.
 * WhatsApp/Android 11+ üzerinde medya dosyaları
 * Android/media/com.whatsapp/WhatsApp/Media/... altında tutulur ve bu
 * klasör sisteme (MediaStore) otomatik taranır; bu yüzden özel bir
 * dosya-sistemi izni gerekmeden, sadece medya okuma izniyle yakalanabilir.
 */
class MediaWatcherService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val imageObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scanForNewMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image")
        }
    }

    private val videoObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scanForNewMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video")
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imageObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver
        )
        // İlk kurulumda mevcut medyayı da bir kez tara
        scanForNewMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image")
        scanForNewMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(imageObserver)
        contentResolver.unregisterContentObserver(videoObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scanForNewMedia(collection: Uri, type: String) {
        scope.launch {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE
            )
            // Sadece WhatsApp klasörlerindeki dosyalarla ilgileniyoruz
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%WhatsApp%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT 50"

            val dao = AppDatabase.getInstance(applicationContext).mediaDao()
            val backupDir = File(filesDir, "media_backup").apply { if (!exists()) mkdirs() }

            try {
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = Uri.withAppendedPath(collection, id.toString())
                        val uriString = contentUri.toString()

                        if (dao.exists(uriString) > 0) continue

                        val displayName = cursor.getString(nameCol) ?: "dosya_$id"
                        val dateAdded = cursor.getLong(dateCol) * 1000L
                        val size = cursor.getLong(sizeCol)

                        val destFile = File(backupDir, "${id}_$displayName")
                        try {
                            contentResolver.openInputStream(contentUri)?.use { input ->
                                FileOutputStream(destFile).use { output -> input.copyTo(output) }
                            }

                            dao.insert(
                                MediaFileEntity(
                                    originalUri = uriString,
                                    localBackupPath = destFile.absolutePath,
                                    mediaType = type,
                                    fileName = displayName,
                                    dateAddedMillis = dateAdded,
                                    sizeBytes = size
                                )
                            )
                        } catch (e: Exception) {
                            // Dosya henüz tam yazılmamış olabilir; bir sonraki
                            // onChange tetiklemesinde tekrar denenecek çünkü
                            // kayıt DB'ye eklenmediği için "exists" kontrolü geçmeyecek.
                        }
                    }
                }
            } catch (e: Exception) {
                // İzin verilmemişse ya da sorgu başarısızsa sessizce geç
            }
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "media_watcher_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Medya İzleme", NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WA Mesaj Geçmişi")
            .setContentText("WhatsApp medyası arka planda yedekleniyor")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1, notification)
    }
}
