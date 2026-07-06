package com.example.androidsilencerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.androidsilencerapp.data.model.AutomationLog
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.model.CalendarException

@Database(entities = [Profile::class, AutomationLog::class, CalendarException::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun automationLogDao(): AutomationLogDao
    abstract fun calendarExceptionDao(): CalendarExceptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "silencer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
