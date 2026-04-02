package com.example.androidsilencerapp.engine

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.util.*

class CalendarManager(private val context: Context) {
    private val TAG = "CalendarManager"
    private val EXCEPTION_KEYWORD = "EXCEPTIONAL"

    fun isExceptionActive(): Boolean {
        try {
            val now = System.currentTimeMillis()
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )

            // Query for events happening right now
            val selection = "(${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DTEND} >= ?)"
            val selectionArgs = arrayOf(now.toString(), now.toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(0) ?: ""
                    val description = it.getString(1) ?: ""
                    
                    if (title.contains(EXCEPTION_KEYWORD, ignoreCase = true) || 
                        description.contains(EXCEPTION_KEYWORD, ignoreCase = true)) {
                        Log.d(TAG, "Active Exceptional event found: $title")
                        return true
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Calendar permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking calendar", e)
        }
        return false
    }
}
