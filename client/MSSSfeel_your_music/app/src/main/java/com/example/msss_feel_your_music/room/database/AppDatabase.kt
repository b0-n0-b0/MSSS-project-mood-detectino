package com.example.msss_feel_your_music.room.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.msss_feel_your_music.room.daos.BlacklistDao
import com.example.msss_feel_your_music.room.entities.Blacklist
import com.example.msss_feel_your_music.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The database contains just one table with blacklist informations
@Database(entities = [Blacklist::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){

    // Reference to the dao to execute the queries
    abstract fun BlacklistDao(): BlacklistDao

    // Callback with coroutine to create the database
    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        // When the database is created
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            Log.d("AppDatabase","db created")

            INSTANCE?.let { database ->
                scope.launch {
                    // populateDatabase(database.BlacklistDao())
                }
            }
        }

        // DEBUG Database population mockup
        suspend fun populateDatabase(blacklistDao: BlacklistDao) {
            Log.d("AppDatabase","populate db")

            // Delete all content here.
            blacklistDao.deleteAll()

            // Add sample tracks in blacklist.
            blacklistDao.insert(Blacklist(uri = "spotify:track:6eiERZXZqMNYk7RjqR9Ucd", skipCount = 1))
            blacklistDao.insert(Blacklist(uri = "spotify:track:641IM7ryimhi1vVfSONkJz", skipCount = 1))
            blacklistDao.insert(Blacklist(uri = "spotify:track:6CHIc7NYBvYPLEUHQxhHKg", skipCount = 1))

        }
    }

    // To ensure a single instance of the database
    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "FeelYourMusicDB"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }


    }
}