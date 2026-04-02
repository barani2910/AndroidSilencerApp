package com.example.androidsilencerapp.data.local

import androidx.room.*
import com.example.androidsilencerapp.data.model.CalendarException

@Dao
interface CalendarExceptionDao {
    @Query("SELECT * FROM calendar_exceptions")
    suspend fun getAllExceptions(): List<CalendarException>

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_exceptions WHERE date = :date)")
    suspend fun isDateExceptional(date: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addException(exception: CalendarException)

    @Delete
    suspend fun removeException(exception: CalendarException)
}
