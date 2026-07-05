package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_history")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val actionPlanned: String? = null,
    val status: String = "SUCCESS" // SUCCESS, FAILED, CONFIRMATION_REQUIRED
)

@Entity(tableName = "automations")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val triggerType: String, // TIME, BATTERY, LOCATION
    val triggerValue: String, // e.g. "08:00", "20%", "Home"
    val actionCommand: String, // e.g. "Open Spotify", "Enable Battery Saver"
    val isActive: Boolean = true,
    val lastTriggered: Long = 0
)

@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // SYSTEM, SECURITY, AI
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
