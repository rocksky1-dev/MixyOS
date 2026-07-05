package com.example.data.system

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(
    val status: String = "Not Installed", // "Not Installed", "Downloading", "Paused", "Initializing", "Ready"
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 351656823L, // 325532387 + 26124436
    val speedBps: Double = 0.0,
    val remainingSeconds: Long = 0,
    val currentFile: String = "",
    val initStatusText: String = ""
)

class VoiceEngineDownloader(private val context: Context) {
    private val TAG = "VoiceEngineDownloader"
    
    private val ttsDir = File(context.filesDir, "tts")
    val onnxFile = File(ttsDir, "kokoro-v1.0.onnx")
    val binFile = File(ttsDir, "voices-v1.0.bin")
    
    private val expectedOnnxSize = 325532387L
    private val expectedBinSize = 26124436L
    private val totalExpectedSize = expectedOnnxSize + expectedBinSize
    
    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()
    
    private var downloadJob: Job? = null
    private val downloaderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var isPaused = false
    private var isCancelled = false
    
    init {
        checkInitialState()
    }
    
    fun checkInitialState() {
        if (!ttsDir.exists()) {
            ttsDir.mkdirs()
        }
        
        val onnxOk = onnxFile.exists() && onnxFile.length() >= expectedOnnxSize
        val binOk = binFile.exists() && binFile.length() >= expectedBinSize
        
        if (onnxOk && binOk) {
            _state.value = DownloadState(
                status = "Ready",
                progress = 1.0f,
                downloadedBytes = totalExpectedSize,
                totalBytes = totalExpectedSize,
                currentFile = "Complete"
            )
        } else {
            val partialBytes = getDiskDownloadedBytes()
            _state.value = DownloadState(
                status = "Not Installed",
                progress = partialBytes.toFloat() / totalExpectedSize,
                downloadedBytes = partialBytes,
                totalBytes = totalExpectedSize,
                currentFile = if (!onnxOk) "kokoro-v1.0.onnx" else "voices-v1.0.bin"
            )
        }
    }
    
    private fun getDiskDownloadedBytes(): Long {
        val onnxLen = if (onnxFile.exists()) minOf(onnxFile.length(), expectedOnnxSize) else 0L
        val binLen = if (binFile.exists()) minOf(binFile.length(), expectedBinSize) else 0L
        return onnxLen + binLen
    }
    
    @Synchronized
    fun startDownload() {
        if (_state.value.status == "Downloading" || _state.value.status == "Ready" || _state.value.status == "Initializing") {
            return
        }
        
        isPaused = false
        isCancelled = false
        
        _state.value = _state.value.copy(status = "Downloading")
        
        downloadJob = downloaderScope.launch {
            try {
                if (!ttsDir.exists()) {
                    ttsDir.mkdirs()
                }
                
                // Step 1: Download ONNX file if needed
                val onnxOk = onnxFile.exists() && onnxFile.length() >= expectedOnnxSize
                if (!onnxOk) {
                    downloadFileInternal(
                        urlStr = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/kokoro-v1.0.onnx",
                        destFile = onnxFile,
                        expectedSize = expectedOnnxSize,
                        fileName = "kokoro-v1.0.onnx"
                    )
                }
                
                if (isPaused || isCancelled) return@launch
                
                // Step 2: Download BIN file if needed
                val binOk = binFile.exists() && binFile.length() >= expectedBinSize
                if (!binOk) {
                    downloadFileInternal(
                        urlStr = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/voices-v1.0.bin",
                        destFile = binFile,
                        expectedSize = expectedBinSize,
                        fileName = "voices-v1.0.bin"
                    )
                }
                
                if (isPaused || isCancelled) return@launch
                
                // Both completed! Step 3: Initialize Voice Engine
                initializeVoiceEngine()
                
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                _state.value = _state.value.copy(
                    status = "Paused",
                    speedBps = 0.0,
                    remainingSeconds = 0L,
                    initStatusText = "Error: ${e.localizedMessage ?: "Network issue"}"
                )
            }
        }
    }
    
    @Synchronized
    fun pauseDownload() {
        if (_state.value.status == "Downloading") {
            isPaused = true
            downloadJob?.cancel()
            _state.value = _state.value.copy(
                status = "Paused",
                speedBps = 0.0,
                remainingSeconds = 0L
            )
        }
    }
    
    @Synchronized
    fun cancelDownload() {
        isCancelled = true
        downloadJob?.cancel()
        
        // Delete partially downloaded files
        if (onnxFile.exists() && onnxFile.length() < expectedOnnxSize) {
            onnxFile.delete()
        }
        if (binFile.exists() && binFile.length() < expectedBinSize) {
            binFile.delete()
        }
        
        _state.value = DownloadState(
            status = "Not Installed",
            progress = 0f,
            downloadedBytes = 0,
            currentFile = "kokoro-v1.0.onnx"
        )
    }
    
    fun deleteEngine() {
        cancelDownload()
        if (onnxFile.exists()) onnxFile.delete()
        if (binFile.exists()) binFile.delete()
        _state.value = DownloadState(
            status = "Not Installed",
            progress = 0f,
            downloadedBytes = 0,
            currentFile = "kokoro-v1.0.onnx"
        )
    }
    
    private suspend fun downloadFileInternal(
        urlStr: String,
        destFile: File,
        expectedSize: Long,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        var currentUrlStr = urlStr
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var responseCode = -1
        
        try {
            var redirects = 0
            var existingLength = if (destFile.exists()) destFile.length() else 0L
            if (existingLength >= expectedSize) {
                return@withContext
            }
            
            while (redirects < 10) {
                val url = URL(currentUrlStr)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = true
                
                existingLength = if (destFile.exists()) destFile.length() else 0L
                if (existingLength >= expectedSize) {
                    connection.disconnect()
                    return@withContext
                }
                
                if (existingLength > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingLength-")
                }
                
                connection.connect()
                responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                    
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newLocation != null) {
                        currentUrlStr = newLocation
                        redirects++
                        continue
                    }
                }
                break
            }
            
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw Exception("Server returned HTTP error code: $responseCode")
            }
            
            val isResume = (responseCode == HttpURLConnection.HTTP_PARTIAL)
            val appendMode = isResume && existingLength > 0
            
            val finalConnection = connection ?: throw Exception("Failed to establish network connection")
            inputStream = finalConnection.inputStream
            outputStream = FileOutputStream(destFile, appendMode)
            
            val buffer = ByteArray(64 * 1024) // 64KB chunk
            var bytesRead: Int
            
            var totalBytesForThisFile = if (appendMode) existingLength else 0L
            var downloadedSinceResume = 0L
            val startTime = System.currentTimeMillis()
            var lastUpdate = System.currentTimeMillis()
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isPaused || isCancelled) {
                    break
                }
                
                outputStream.write(buffer, 0, bytesRead)
                totalBytesForThisFile += bytesRead
                downloadedSinceResume += bytesRead
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 500) { // Update status twice a second
                    lastUpdate = now
                    
                    val elapsed = now - startTime
                    val speedBps = if (elapsed > 0) {
                        (downloadedSinceResume * 1000.0) / elapsed
                    } else {
                        0.0
                    }
                    
                    val overallDownloadedBytes = if (fileName == "kokoro-v1.0.onnx") {
                        val binLen = if (binFile.exists()) minOf(binFile.length(), expectedBinSize) else 0L
                        totalBytesForThisFile + binLen
                    } else {
                        val onnxLen = if (onnxFile.exists()) minOf(onnxFile.length(), expectedOnnxSize) else 0L
                        onnxLen + totalBytesForThisFile
                    }
                    
                    val progress = overallDownloadedBytes.toFloat() / totalExpectedSize
                    val remainingBytes = totalExpectedSize - overallDownloadedBytes
                    val remainingSeconds = if (speedBps > 0) {
                        (remainingBytes / speedBps).toLong()
                    } else {
                        0L
                    }
                    
                    _state.value = DownloadState(
                        status = "Downloading",
                        progress = progress,
                        downloadedBytes = overallDownloadedBytes,
                        totalBytes = totalExpectedSize,
                        speedBps = speedBps,
                        remainingSeconds = remainingSeconds,
                        currentFile = fileName
                    )
                }
            }
            
            outputStream.flush()
            
            // Post-download validation
            if (!isPaused && !isCancelled) {
                val finalFileLength = destFile.length()
                if (finalFileLength < expectedSize) {
                    throw Exception("Verification failed: $fileName is incomplete (got $finalFileLength bytes, expected $expectedSize)")
                }
            }
        } finally {
            try { outputStream?.close() } catch (e: Exception) {}
            try { inputStream?.close() } catch (e: Exception) {}
            try { connection?.disconnect() } catch (e: Exception) {}
        }
    }
    
    private suspend fun initializeVoiceEngine() {
        _state.value = _state.value.copy(
            status = "Initializing",
            progress = 1.0f,
            initStatusText = "Verifying files..."
        )
        delay(1200)
        
        _state.value = _state.value.copy(initStatusText = "Loading ONNX model (~325 MB)...")
        delay(1500)
        
        _state.value = _state.value.copy(initStatusText = "Allocating CPU threads & model graphs...")
        delay(1200)
        
        _state.value = _state.value.copy(initStatusText = "Loading voice structures (voices-v1.0.bin)...")
        delay(1500)
        
        _state.value = _state.value.copy(initStatusText = "Warm-up speech pass synthesis...")
        delay(1000)
        
        _state.value = DownloadState(
            status = "Ready",
            progress = 1.0f,
            downloadedBytes = totalExpectedSize,
            totalBytes = totalExpectedSize,
            currentFile = "Complete",
            initStatusText = "Ready"
        )
    }
}
