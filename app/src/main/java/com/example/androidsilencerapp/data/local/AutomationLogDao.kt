package com.example.androidsilencerapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.androidsilencerapp.data.model.AutomationLog

@Dao
interface AutomationLogDao {
    @Query("SELECT * FROM automation_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getLogsForUser(userId: String): LiveData<List<AutomationLog>>

    @Insert
    suspend fun insertLog(log: AutomationLog)

    @Query("DELETE FROM automation_logs WHERE userId = :userId")
    suspend fun deleteLogsForUser(userId: String)
}
