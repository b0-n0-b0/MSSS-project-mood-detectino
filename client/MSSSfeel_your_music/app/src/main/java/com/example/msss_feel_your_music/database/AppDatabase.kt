package com.example.msss_feel_your_music.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.msss_feel_your_music.daos.TrackInfoDao
import com.example.msss_feel_your_music.entities.TrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [TrackInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){
    abstract fun trackInfoDao(): TrackInfoDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            Log.d("AppDatabase","db created")

            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.trackInfoDao())
                }
            }
        }

        suspend fun populateDatabase(trackInfoDao: TrackInfoDao) {
            Log.d("AppDatabase","populate db")

            // Delete all content here.
            trackInfoDao.deleteAll()

            // Add sample tracks.
            trackInfoDao.insert(TrackInfo(tid = "11dFghVXANMlKmJXsNCbNl", valence = 0.428))
            trackInfoDao.insert(TrackInfo(tid = "4VqPOruhp5EdPBeR92t6lQ", valence = 0.411))
            trackInfoDao.insert(TrackInfo(tid = "7ouMYWpwJ422jRcDASZB7P", valence = 0.211))

            // TODO: Add your own tracks!
        }
    }

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