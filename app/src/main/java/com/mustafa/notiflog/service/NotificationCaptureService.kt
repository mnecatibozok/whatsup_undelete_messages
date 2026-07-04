package com.mustafa.notiflog.service

import android.app.Notification
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mustafa.notiflog.data.AppDatabase
import com.mustafa.notiflog.data.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WhatsApp bildirimlerini dinler ve içeriklerini local veritabanına kaydeder.
 * Mesaj sohbette daha sonra silinse bile, buraya kaydedilen kopya kalıcı olarak durur.
 *
 * Ayarlar -> Bildirimler -> Özel erişim -> Bildirim erişimi kısmından
 * bu servise izin verilmesi gerekiyor.
 */
class NotificationCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val watchedPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b" // WhatsApp Business
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watchedPackages) return

        val extras = sbn.notification.extras ?: return
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: return

        // WhatsApp, MessagingStyle bildirimleri kullanır; EXTRA_MESSAGES içinde
        // o ana kadarki tüm mesajlar (sender + text + time) bulunur.
        val messagesArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)

        if (messagesArray != null && messagesArray.isNotEmpty()) {
            for (item in messagesArray) {
                val bundle = (item as? Parcelable)?.let { it as? android.os.Bundle } ?: continue
                val text = bundle.getCharSequence("text")?.toString() ?: continue
                val sender = bundle.getCharSequence("sender")?.toString() ?: conversationTitle
                val time = bundle.getLong("time", System.currentTimeMillis())
                val isGroup = sender != conversationTitle

                saveMessage(sbn, conversationTitle, sender, text, isGroup, time)
            }
        } else {
            // Fallback: basit bildirimler (EXTRA_MESSAGES yoksa)
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
            saveMessage(
                sbn,
                conversationTitle,
                conversationTitle,
                text,
                isGroup = false,
                time = sbn.postTime
            )
        }
    }

    private fun saveMessage(
        sbn: StatusBarNotification,
        conversationTitle: String,
        sender: String,
        text: String,
        isGroup: Boolean,
        time: Long
    ) {
        scope.launch {
            val dao = AppDatabase.getInstance(applicationContext).messageDao()
            val alreadyExists = dao.existsExact(conversationTitle, sender, text, time) > 0
            if (!alreadyExists) {
                dao.insert(
                    MessageEntity(
                        packageName = sbn.packageName,
                        conversationTitle = conversationTitle,
                        sender = sender,
                        text = text,
                        isGroup = isGroup,
                        timestamp = time,
                        notificationKey = sbn.key ?: "$conversationTitle-$time"
                    )
                )
            }
        }
    }

    // Bildirim kaldırılsa/temizlense bile veritabanındaki kayıt kalır;
    // burada özel bir işlem yapmıyoruz, kayıt zaten postNotification anında alınıyor.
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
