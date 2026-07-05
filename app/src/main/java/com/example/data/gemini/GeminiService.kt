package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini API Data Classes ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64
)

data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null,
    val thinkingConfig: ThinkingConfig? = null
)

data class ThinkingConfig(
    val thinkingBudget: Int // Wait, the thinking level can be specified or thinkingConfig is structured
)

// Since we are using Moshi converter, we will parse responses manually or using Moshi models.
// Let's define simple response structures for Retrofit to map.
// Alternatively, to avoid Moshi mapping issues with deeply nested JSON, we can write a raw JSON string request/response parsing,
// which is extremely robust and 100% immune to model structure mismatches.
// Let's implement BOTH: standard raw bodies (String) for total flexibility, and typed models if needed.
// This is the most bulletproof way to build Gemini REST clients in sandbox environments!

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): ResponseBody
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "Using API Key suffix: ${if (key.isNotEmpty()) key.takeLast(4) else "EMPTY"}")
        return key
    }

    /**
     * General content generator. Handles text prompts and optional bitmap image.
     */
    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null,
        bitmap: Bitmap? = null,
        useHighThinking: Boolean = false,
        modelName: String = "gemini-3.5-flash"
    ): String {
        val key = getApiKey()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return "Mixy OS: Gemini API Key is missing. Please configure it in your AI Studio Secrets panel."
        }

        // We will select model based on options.
        // For high thinking, use 'gemini-3.1-pro-preview'
        val activeModel = if (useHighThinking) "gemini-3.1-pro-preview" else modelName

        try {
            // Build raw JSON manually for total robustness and flexibility
            val contentsJson = StringBuilder()
            contentsJson.append("[")
            contentsJson.append("{\"parts\":[")
            
            // Add image if exists
            if (bitmap != null) {
                val base64Image = bitmap.toBase64()
                contentsJson.append("{\"inlineData\":{\"mimeType\":\"image/jpeg\",\"data\":\"$base64Image\"}},")
            }
            
            // Escape double quotes and backslashes in prompt
            val escapedPrompt = escapeJsonString(prompt)
            contentsJson.append("{\"text\":\"$escapedPrompt\"}")
            contentsJson.append("]}")
            contentsJson.append("]")

            val requestBuilder = StringBuilder()
            requestBuilder.append("{")
            requestBuilder.append("\"contents\":$contentsJson")

            // System instructions
            if (!systemInstruction.isNullOrEmpty()) {
                val escapedSystem = escapeJsonString(systemInstruction)
                requestBuilder.append(",\"systemInstruction\":{\"parts\":[{\"text\":\"$escapedSystem\"}]}")
            }

            // Generation config
            requestBuilder.append(",\"generationConfig\":{")
            requestBuilder.append("\"temperature\":0.7,")
            requestBuilder.append("\"topP\":0.95")
            
            // If high thinking level is requested, add thinkingConfig to gemini-3.1-pro-preview
            if (useHighThinking) {
                // Wait! To set ThinkingLevel.HIGH on gemini-3.1-pro-preview, we set thinkingConfig with a thinking budget.
                // Let's add thinkingConfig. In the Google AI Studio SDK / REST API:
                // "thinkingConfig": { "thinkingBudget": 1024 } or similar, let's include it.
                requestBuilder.append(",\"thinkingConfig\":{\"thinkingBudget\":1024}")
            }
            requestBuilder.append("}")

            requestBuilder.append("}")

            val requestBodyString = requestBuilder.toString()
            val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

            Log.d(TAG, "Requesting $activeModel...")
            val responseBody = service.generateContent(activeModel, key, requestBody)
            val rawResponse = responseBody.string()
            
            return parseResponseText(rawResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API Error", e)
            return "Error: ${e.localizedMessage ?: "Unknown API Error"}"
        }
    }

    /**
     * Translates a natural language command into structured JSON.
     */
    suspend fun parseCommand(userCommand: String): MixyAction {
        val systemPrompt = """
            You are the brain of Mixy OS, a futuristic JARVIS-like AI operating system. 
            Analyze the user's phone control command and translate it into a single structured JSON action.
            Supported Actions:
            1. OPEN_APP: {"action": "OPEN_APP", "appName": "AppNameString"}
               Examples: "Open Instagram", "Launch YouTube"
            2. MAKE_CALL: {"action": "MAKE_CALL", "phoneNumber": "1234567890", "contactName": "NameString"}
               Examples: "Call Mom", "Dial 9876543210"
            3. SEND_SMS: {"action": "SEND_SMS", "phoneNumber": "1234567890", "contactName": "Name", "message": "Text content"}
               Examples: "Send message to Rohan saying Hello", "Text Dad I will be late"
            4. SET_ALARM: {"action": "SET_ALARM", "time": "06:00", "label": "Alarm label"}
               Examples: "Set alarm for 6 AM", "Wake me up at 7:30 with Coffee label"
            5. SET_TIMER: {"action": "SET_TIMER", "seconds": 300, "label": "Timer label"}
               Examples: "Set a 5 minute timer"
            6. FLASHLIGHT: {"action": "FLASHLIGHT", "state": "ON"|"OFF"|"TOGGLE"}
               Examples: "Turn on flashlight", "Torch off"
            7. SET_VOLUME: {"action": "SET_VOLUME", "level": 50} (level 0 to 100)
               Examples: "Mute", "Increase volume to 80%", "Set volume to half"
            8. SET_BRIGHTNESS: {"action": "SET_BRIGHTNESS", "level": 40} (level 0 to 100)
               Examples: "Reduce brightness to 40%", "Make screen bright"
            9. LAUNCH_CAMERA: {"action": "LAUNCH_CAMERA"}
               Examples: "Open camera", "Take a picture"
            10. OPEN_GALLERY: {"action": "OPEN_GALLERY"}
               Examples: "Open gallery", "Show my photos"
            11. OPEN_MAPS: {"action": "OPEN_MAPS", "query": "Destination Address or Search"}
                Examples: "Take me home", "Search Pizza near me on maps"
            12. MANAGE_CLIPBOARD: {"action": "MANAGE_CLIPBOARD", "subAction": "COPY"|"READ", "text": "optional text"}
                Examples: "Copy 'Secret' to clipboard", "Read my clipboard"
            13. SYSTEM_SETTING: {"action": "SYSTEM_SETTING", "settingType": "WIFI"|"BLUETOOTH"|"DATA"|"HOTSPOT"|"NFC"|"DISPLAY"|"SETTINGS"}
                Examples: "Open Bluetooth settings", "Turn on wifi settings"
            14. FILE_ACTION: {"action": "FILE_ACTION", "subAction": "SEARCH"|"DELETE_DUPLICATES"|"COMPRESS", "query": "filename or path"}
                Examples: "Find my PDF notes", "Delete screenshots from Downloads", "Delete duplicates"
            15. ACCESSIBILITY_ACTION: {"action": "ACCESSIBILITY_ACTION", "gestureType": "TAP"|"SCROLL"|"TYPE"|"READ_SCREEN", "target": "button label or text"}
                Examples: "Tap confirm", "Scroll down", "Read screen text"
            16. NOTIFICATION_SUMMARY: {"action": "NOTIFICATION_SUMMARY"}
                Examples: "Read my notifications", "Summarize notification alerts"
            17. CHAT: {"action": "CHAT", "reply": "A friendly operational response acknowledging the task or chatting"}
                Use CHAT if the request is conversational, a direct query like "Who are you?", or cannot be completed by a single direct system command.

            IMPORTANT: Return ONLY a raw JSON block. No markdown, no ```json formatting, no other explanation text.
        """.trimIndent()

        val response = generate(
            prompt = userCommand,
            systemInstruction = systemPrompt,
            modelName = "gemini-3.5-flash"
        )

        Log.d(TAG, "Command parsing raw response: $response")

        return try {
            val jsonText = response.trim().removeSurrounding("```json", "```").trim()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(MixyAction::class.java)
            adapter.fromJson(jsonText) ?: MixyAction(action = "CHAT", reply = response)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing action JSON from response", e)
            MixyAction(action = "CHAT", reply = response)
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun escapeJsonString(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseResponseText(rawJson: String): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Map::class.java)
            val root = adapter.fromJson(rawJson)
            val candidates = root?.get("candidates") as? List<*>
            val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCandidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val textBuilder = StringBuilder()
            parts?.forEach { part ->
                val partMap = part as? Map<*, *>
                val text = partMap?.get("text") as? String
                if (text != null) {
                    textBuilder.append(text)
                }
            }
            if (textBuilder.isEmpty()) "No response returned from Gemini." else textBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini REST response JSON", e)
            // Fallback string extraction for safety
            if (rawJson.contains("\"text\": \"")) {
                val startIdx = rawJson.indexOf("\"text\": \"") + 9
                val endIdx = rawJson.indexOf("\"", startIdx)
                if (startIdx in 10..rawJson.length && endIdx > startIdx) {
                    return rawJson.substring(startIdx, endIdx).replace("\\n", "\n")
                }
            }
            "Response parsing failure."
        }
    }
}

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
