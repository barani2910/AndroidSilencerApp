package com.example.androidsilencerapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_exceptions")
data class CalendarException(
    @PrimaryKey
    val date: Long // Store date as midnight timestamp
)
