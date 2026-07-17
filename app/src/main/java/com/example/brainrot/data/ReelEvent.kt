package com.example.brainrot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reel_events")
data class ReelEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String, // "Instagram" or "YouTube"
    val timestamp: Long = System.currentTimeMillis()
)
