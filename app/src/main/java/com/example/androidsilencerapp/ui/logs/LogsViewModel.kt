package com.example.androidsilencerapp.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.androidsilencerapp.data.local.AutomationLogDao
import com.example.androidsilencerapp.data.model.AutomationLog

class LogsViewModel(private val logDao: AutomationLogDao) : ViewModel() {
    fun getLogs(userId: String): LiveData<List<AutomationLog>> {
        return logDao.getLogsForUser(userId)
    }
}
