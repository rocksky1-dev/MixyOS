package com.example.ui.viewmodel

import android.graphics.Bitmap

data class LlmMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val fileNames: List<String> = emptyList(),
    val imageBitmaps: List<Bitmap> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val pendingAction: com.example.data.gemini.MixyAction? = null,
    val actionExecuted: Boolean = false
)
