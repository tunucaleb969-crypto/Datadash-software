package com.kwame.datadash.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // sqlcipher-android (unlike the older, deprecated android-database-sqlcipher)
        // does not load its native library automatically — it must be loaded
        // explicitly before any SQLCipher class is used.
        init {
            System.loadLibrary("sqlcipher")
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            val passphrase = SecurePrefs.getOrCreateDbPassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "datadash.db"
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}
