package com.mustafa.notiflog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [MessageEntity::class, MediaFileEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2 -> v3: "silinen mesaj" işaretleme özelliği için isDeleted kolonu
        // eklendi. Mevcut kayıtlı mesajlar kaybolmasın diye gerçek migration
        // yazıyoruz (destructive fallback ile veritabanını silmiyoruz).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notiflog.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // v1 -> v2 geçişi için (medya tablosu eklenmişti) hâlâ
                    // gerçek migration yazılmadı; o eski sürümden gelen
                    // kurulumlar için veri sıfırlanabilir. v2 ve sonrası için
                    // artık gerçek migration kullanılıyor.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
