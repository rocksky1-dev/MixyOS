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
import java.util.concurrent.TimeUnit

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

    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        bitmaps: List<Bitmap> = emptyList(),
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
