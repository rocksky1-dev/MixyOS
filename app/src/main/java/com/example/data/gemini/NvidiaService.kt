package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Structured Action Schema ---
data class MixyAction(
    val action: String, // OPEN_APP, MAKE_CALL, SEND_SMS, SET_ALARM, SET_TIMER, FLASHLIGHT, etc.
    val appName: String? = null,
    val phoneNumber: String? = null,
    val contactName: String? = null,
    val message: String? = null,
    val time: String? = null,
    val label: String? = null,
    val seconds: Int? = null,
    val state: String? = null, // ON, OFF, TOGGLE
    val level: Int? = null, // 0 to 100
    val query: String? = null,
    val subAction: String? = null, // COPY, READ, SEARCH, DELETE_DUPLICATES, COMPRESS
    val text: String? = null,
    val settingType: String? = null, // WIFI, BLUETOOTH, DATA, HOTSPOT, NFC, DISPLAY, SETTINGS
    val gestureType: String? = null, // TAP, SCROLL, TYPE, READ_SCREEN
    val target: String? = null,
    val reply: String? = null // AI pre-filled operational message
)

object NvidiaService {
    private const val TAG = "NvidiaService"
    private const val BASE_URL = "https://integrate.api.nvidia.com/v1/chat/completions"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Parses the custom structured LLM action tag format into a typed MixyAction.
     * Support format: [ACTION:FLASHLIGHT:state=ON] or [ACTION:LAUNCH_CAMERA] or [ACTION:SET_VOLUME:level=80]
     */
    fun parseLlmActionTag(tagContent: String): MixyAction? {
        try {
            val parts = tagContent.split(":")
            if (parts.isEmpty()) return null
            val actionType = parts[0].trim().uppercase()
            
            val params = mutableMapOf<String, String>()
            if (parts.size > 1) {
                val paramStr = parts.subList(1, parts.size).joinToString(":")
                val pairs = paramStr.split(",")
                for (pair in pairs) {
                    val kv = pair.split("=")
                    if (kv.size == 2) {
                        params[kv[0].trim()] = kv[1].trim()
                    } else if (kv.size == 1 && kv[0].isNotEmpty()) {
                        params["value"] = kv[0].trim()
                    }
                }
            }
            
            return MixyAction(
                action = actionType,
                state = params["state"] ?: params["value"],
                appName = params["appName"] ?: params["value"],
                phoneNumber = params["phoneNumber"] ?: params["value"],
                contactName = params["contactName"],
                message = params["message"],
                time = params["time"] ?: params["value"],
                seconds = params["seconds"]?.toIntOrNull() ?: params["value"]?.toIntOrNull(),
                level = params["level"]?.toIntOrNull() ?: params["value"]?.toIntOrNull(),
                query = params["query"] ?: params["value"],
                settingType = params["settingType"] ?: params["value"]
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing action tag: $tagContent", e)
            return null
        }
    }

    /**
     * Highly robust offline parser that translates natural commands into system actions.
     * Works instantly without API keys or active internet connections.
     */
    fun localParseCommand(query: String): MixyAction {
        val clean = query.trim().lowercase(Locale.getDefault())
        
        return when {
            // 1. Flashlight Commands
            (clean.contains("flashlight") || clean.contains("torch")) && (clean.contains("on") || clean.contains("start") || clean.contains("enable") || clean.contains("activate") || clean.contains("chala") || clean.contains("jalao")) -> {
                MixyAction(action = "FLASHLIGHT", state = "ON", reply = "Activating device flashlight lens.")
            }
            (clean.contains("flashlight") || clean.contains("torch")) && (clean.contains("off") || clean.contains("stop") || clean.contains("disable") || clean.contains("deactivate") || clean.contains("band")) -> {
                MixyAction(action = "FLASHLIGHT", state = "OFF", reply = "Deactivating device flashlight lens.")
            }
            
            // 2. Camera Commands
            clean.contains("camera") || clean.contains("take a picture") || clean.contains("take photo") || clean.contains("capture image") || clean.contains("click photo") || clean.contains("photo khicho") -> {
                MixyAction(action = "LAUNCH_CAMERA", reply = "Initializing camera sensor.")
            }
            
            // 3. Gallery Commands
            clean.contains("gallery") || clean.contains("photos") || clean.contains("show photos") || clean.contains("show my photos") || clean.contains("images") || clean.contains("album") -> {
                MixyAction(action = "OPEN_GALLERY", reply = "Opening photo gallery directory.")
            }
            
            // 4. Volume Commands
            clean.contains("volume") || clean.contains("mute") || clean.contains("silent") || clean.contains("sound") -> {
                val level = extractNumber(clean) ?: 50
                val replyText = if (clean.contains("mute") || clean.contains("silent") || level == 0) "Muting media volume." else "Setting media volume levels to $level%."
                MixyAction(action = "SET_VOLUME", level = if (clean.contains("mute") || clean.contains("silent")) 0 else level, reply = replyText)
            }
            
            // 5. Brightness Commands
            clean.contains("brightness") || clean.contains("screen bright") || clean.contains("screen dim") || clean.contains("light") -> {
                val level = extractNumber(clean) ?: 40
                MixyAction(action = "SET_BRIGHTNESS", level = level, reply = "Configuring display brightness level to $level%.")
            }
            
            // 6. Settings and Wireless Commands
            clean.contains("bluetooth settings") || (clean.contains("bluetooth") && clean.contains("settings")) -> {
                MixyAction(action = "SYSTEM_SETTING", settingType = "BLUETOOTH", reply = "Loading Bluetooth control deck.")
            }
            clean.contains("wifi settings") || (clean.contains("wifi") && clean.contains("settings")) || clean.contains("wi-fi") -> {
                MixyAction(action = "SYSTEM_SETTING", settingType = "WIFI", reply = "Loading WiFi configuration panel.")
            }
            clean.contains("data settings") || clean.contains("cellular settings") -> {
                MixyAction(action = "SYSTEM_SETTING", settingType = "DATA", reply = "Loading mobile data settings.")
            }
            clean.contains("display settings") || clean.contains("screen settings") -> {
                MixyAction(action = "SYSTEM_SETTING", settingType = "DISPLAY", reply = "Loading display control settings.")
            }
            clean.contains("setting") || clean.contains("settings") -> {
                MixyAction(action = "SYSTEM_SETTING", settingType = "SETTINGS", reply = "Opening system Settings dashboard.")
            }
            
            // 7. Timer Commands
            clean.contains("timer") || clean.contains("stopwatch") -> {
                val mins = extractNumber(clean) ?: 5
                val seconds = mins * 60
                MixyAction(action = "SET_TIMER", seconds = seconds, label = "Mixy Timer", reply = "Launching system timer for $mins minutes.")
            }
            
            // 8. Alarm Commands
            clean.contains("alarm") || clean.contains("wake me up") -> {
                val timeString = extractTime(clean) ?: "06:00"
                MixyAction(action = "SET_ALARM", time = timeString, label = "Mixy Alarm", reply = "Configuring alarm for $timeString on your system clock.")
            }
            
            // 9. Call / Dial Commands
            clean.startsWith("call ") || clean.contains("dial ") -> {
                val rawName = query.substringAfter("call ", "").substringAfter("dial ", "").trim()
                val phoneNum = extractPhoneNumber(rawName)
                val contactName = if (phoneNum != null) null else rawName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Contact"
                MixyAction(action = "MAKE_CALL", phoneNumber = phoneNum, contactName = contactName, reply = "Initiating dialer sequence for ${contactName ?: phoneNum}.")
            }
            
            // 10. SMS / Messaging Commands
            clean.startsWith("send message") || clean.startsWith("text ") || clean.contains("message to ") -> {
                val rawName = query.substringAfter("message to ", "").substringAfter("text ", "").trim()
                val contactName = rawName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Contact"
                val messageText = query.substringAfter("saying ", "").substringAfter("text ", "").substringAfter(contactName, "").trim()
                MixyAction(action = "SEND_SMS", contactName = contactName, message = messageText.ifEmpty { "Hello!" }, reply = "Drafting SMS message.")
            }
            
            // 11. Maps and Navigation Commands
            clean.contains("map") || clean.contains("navigate") || clean.contains("location") || clean.contains("where is") || clean.contains("directions to") -> {
                val destination = query.substringAfter("navigate to ", "").substringAfter("where is ", "").substringAfter("directions to ", "").trim()
                val mapQuery = if (destination.isEmpty() || destination == query) "current location" else destination
                MixyAction(action = "OPEN_MAPS", query = mapQuery, reply = "Plotting trajectory on map to $mapQuery.")
            }
            
            // 12. Open App Commands
            clean.startsWith("open ") || clean.startsWith("launch ") -> {
                val app = query.substringAfter("open ", "").substringAfter("launch ", "").trim()
                MixyAction(action = "OPEN_APP", appName = app, reply = "Launching system application: $app.")
            }
            
            // 13. File action Commands
            clean.contains("file") || clean.contains("files") || clean.contains("compress") || clean.contains("duplicates") -> {
                MixyAction(action = "FILE_ACTION", subAction = "SEARCH", reply = "Accessing standard file manager directory indices.")
            }
            
            // 14. Notification summary Commands
            clean.contains("notification") || clean.contains("notifications") || clean.contains("unread") -> {
                MixyAction(action = "NOTIFICATION_SUMMARY", reply = "Summarizing device notifications.")
            }
            
            // 15. Automations Commands
            clean.contains("automation") || clean.contains("automations") || clean.contains("rules") -> {
                MixyAction(action = "CHAT", reply = "Navigating to Automation deck. You can configure automatic triggers based on Time, Battery, or Location.")
            }
            
            // Default Chat Action
            else -> {
                MixyAction(action = "CHAT", reply = "Cognitive synthesis core operational. How may I assist you?")
            }
        }
    }

    private fun extractNumber(text: String): Int? {
        val numbers = Regex("\\d+").find(text)
        return numbers?.value?.toIntOrNull()
    }

    private fun extractTime(text: String): String? {
        val timePattern = Regex("(\\d{1,2}):(\\d{2})")
        val match = timePattern.find(text)
        if (match != null) {
            val hour = match.groupValues[1].toInt()
            val min = match.groupValues[2].toInt()
            return String.format(Locale.US, "%02d:%02d", hour, min)
        }
        val singleDigit = Regex("\\d+").find(text)
        if (singleDigit != null) {
            val hour = singleDigit.value.toInt()
            if (hour in 0..23) {
                return String.format(Locale.US, "%02d:00", hour)
            }
        }
        return null
    }

    private fun extractPhoneNumber(text: String): String? {
        val cleanText = text.replace(Regex("[^0-9+]"), "")
        if (cleanText.length >= 5) {
            return cleanText
        }
        return null
    }

    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        bitmaps: List<Bitmap> = emptyList(),
        history: List<com.example.ui.viewmodel.LlmMessage> = emptyList(),
        systemPrompt: String = "",
        modelName: String = "google/diffusiongemma-26b-a4b-it"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext "NVIDIA API Key is missing. Please configure it in your Mixy OS Brain settings."
        }

        try {
            val root = JSONObject()
            root.put("model", modelName)
            root.put("temperature", 0.5)
            root.put("top_p", 0.9)
            root.put("max_tokens", 1024)

            val messages = JSONArray()

            // 1. Add System Instruction Prompt
            if (systemPrompt.isNotEmpty()) {
                val systemMsg = JSONObject()
                systemMsg.put("role", "system")
                systemMsg.put("content", systemPrompt)
                messages.put(systemMsg)
            }

            // 2. Add Chat History
            for (msg in history) {
                val histMsg = JSONObject()
                histMsg.put("role", if (msg.isUser) "user" else "assistant")
                val cleanedText = msg.text
                histMsg.put("content", cleanedText)
                messages.put(histMsg)
            }

            // 3. Add Current Message
            val userMsg = JSONObject()
            userMsg.put("role", "user")

            if (bitmaps.isEmpty()) {
                userMsg.put("content", prompt)
            } else {
                val contentArray = JSONArray()
                
                val textObj = JSONObject()
                textObj.put("type", "text")
                textObj.put("text", prompt)
                contentArray.put(textObj)
                
                for (bitmap in bitmaps) {
                    val imageObj = JSONObject()
                    imageObj.put("type", "image_url")
                    
                    val imageUrlDetails = JSONObject()
                    val base64 = bitmap.toBase64()
                    imageUrlDetails.put("url", "data:image/jpeg;base64,$base64")
                    
                    imageObj.put("image_url", imageUrlDetails)
                    contentArray.put(imageObj)
                }
                userMsg.put("content", contentArray)
            }

            messages.put(userMsg)
            root.put("messages", messages)

            val jsonString = root.toString()
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "NVIDIA API error: $errBody")
                return@withContext "Error: API call failed with code ${response.code}. Details: $errBody"
            }

            val responseBodyString = response.body?.string() ?: return@withContext "Error: Received empty response from NVIDIA API"
            Log.d(TAG, "NVIDIA Response: $responseBodyString")

            val responseJson = JSONObject(responseBodyString)
            val choices = responseJson.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                return@withContext message.getString("content")
            }

            return@withContext "Error: No text content returned in choices."
        } catch (e: java.io.IOException) {
            Log.e(TAG, "NVIDIA network error", e)
            return@withContext "Error: Network connection failed. ${e.localizedMessage ?: "Check your internet connection."}"
        } catch (e: Exception) {
            Log.e(TAG, "NVIDIA API general error", e)
            return@withContext "Error: Exception occurred. ${e.localizedMessage ?: "Unknown failure."}"
        }
    }
}
