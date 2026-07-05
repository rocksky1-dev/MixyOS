package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.LlmMessage
import com.example.ui.viewmodel.MixyViewModel
import com.example.data.gemini.MixyAction
import com.example.data.system.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(1) } // Default to 1: Brain Chat
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Brain Icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MIXY LLM COGNITION",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Glass Tab Row for sub-tabs (Neural Core & Brain Chat)
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = Color(0x11FFFFFF),
                contentColor = CyberCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                        color = CyberCyan
                    )
                }
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    modifier = Modifier.testTag("subtab_core"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeveloperBoard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Neural Core", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    modifier = Modifier.testTag("subtab_chat"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Brain Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            AnimatedContent(
                targetState = activeSubTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "SubtabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> NeuralCoreTab(viewModel = viewModel)
                    1 -> BrainChatTab(viewModel = viewModel)
                }
            }
        }
    }
}

// ======================== TAB 0: NEURAL CORE ========================
@Composable
fun NeuralCoreTab(viewModel: MixyViewModel) {
    val downloadState by viewModel.voiceEngineDownloader.state.collectAsState()
    val nvidiaApiKey by viewModel.nvidiaApiKey.collectAsState()
    val nvidiaConnected by viewModel.nvidiaConnected.collectAsState()
    val isConnectingNvidia by viewModel.isConnectingNvidia.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var keyInput by remember { mutableStateOf(nvidiaApiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            NeuralOrbSection(status = downloadState.status)
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // Core Specification Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nvidia_specs_card"),
                cornerRadius = 20.dp,
                borderColor = Color.White.copy(alpha = 0.15f),
                backgroundColor = Color(0x1A090C15)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧠", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mixy OS Brain",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NVIDIA-powered Neural Subsystem",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    SpecRow(label = "Primary Model", value = "google/diffusiongemma-26b-a4b-it", icon = Icons.Default.SettingsVoice)
                    SpecRow(label = "Provider Core", value = "NVIDIA API", icon = Icons.Default.Cloud)
                    SpecRow(label = "Speech Engine", value = "Kokoro TTS (Offline)", icon = Icons.Default.VolumeUp)
                    SpecRow(
                        label = "NVIDIA Link Status",
                        value = if (nvidiaConnected) "Connected" else "Not Connected",
                        icon = Icons.Default.Link,
                        valueColor = if (nvidiaConnected) CyberCyan else CyberPink
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // NVIDIA API Key Configuration
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nvidia_key_config_card"),
                cornerRadius = 20.dp,
                borderColor = CyberCyan.copy(alpha = 0.2f),
                backgroundColor = Color(0x1F050914)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NVIDIA API Key",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("NVIDIA API Key", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nvidia_key_input"),
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isConnectingNvidia) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (keyInput.trim().isEmpty()) {
                                            Toast.makeText(context, "API Key is empty", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.testNvidiaConnection(keyInput) { success, details ->
                                            Toast.makeText(context, details, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("test_connection_btn")
                                ) {
                                    Text("Test", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveNvidiaApiKey(keyInput)
                                        Toast.makeText(context, "API Key Saved Securely", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_key_btn")
                                ) {
                                    Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (keyInput.trim().isEmpty()) {
                                        Toast.makeText(context, "API Key is empty", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.testNvidiaConnection(keyInput) { success, details ->
                                        if (success) {
                                            viewModel.saveNvidiaApiKey(keyInput)
                                            Toast.makeText(context, "Connected & Saved successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Connection failed: $details", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("connect_btn")
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = SpaceDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect Brain", color = SpaceDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Local Voice Downloader Card
            LocalVoiceEngineDownloaderCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LocalVoiceEngineDownloaderCard(viewModel: MixyViewModel) {
    val downloadState by viewModel.voiceEngineDownloader.state.collectAsState()
    val scope = rememberCoroutineScope()
    var isSynthesizing by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("brain_downloader_card"),
        cornerRadius = 20.dp,
        borderColor = Color.White.copy(alpha = 0.12f),
        backgroundColor = Color(0x1F050914)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SettingsVoice, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kokoro Offline TTS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (downloadState.status) {
                "Not Installed" -> {
                    Text(
                        text = "Download Kokoro TTS (Offline Male Voice) to speak replies direct from this device with ultra-low latency.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.voiceEngineDownloader.startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("download_engine_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Engine (~350MB)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
                "Downloading" -> {
                    Text(
                        text = "Downloading Voice Engine Assets: ${downloadState.currentFile}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = downloadState.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", downloadState.progress * 100)}%",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatBytes(downloadState.downloadedBytes)} / ${formatBytes(downloadState.totalBytes)}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.voiceEngineDownloader.pauseDownload() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Yellow.copy(alpha = 0.1f), contentColor = Color.Yellow),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.voiceEngineDownloader.cancelDownload() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyberPink.copy(alpha = 0.1f), contentColor = CyberPink),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "Paused" -> {
                    Text("Download Paused (${String.format(Locale.US, "%.1f", downloadState.progress * 100)}%)", color = Color.Yellow, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.voiceEngineDownloader.startDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Resume", color = SpaceDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.voiceEngineDownloader.cancelDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "Initializing" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberPink, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Initializing offline model core...", color = CyberPink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "Ready" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local Voice Synthesis Offline Engine Ready", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSynthesizing = true
                                viewModel.speakText("Cognitive synthesis core operational. How may I assist you?")
                                delay(3500)
                                isSynthesizing = false
                            }
                        },
                        enabled = !isSynthesizing,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SpaceDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSynthesizing) "Synthesizing Local Voice..." else "Synthesize Speech Test", color = SpaceDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ======================== TAB 1: BRAIN CHAT ========================
@Composable
fun BrainChatTab(viewModel: MixyViewModel) {
    val llmMessages by viewModel.llmMessages.collectAsState()
    val llmIsTyping by viewModel.llmIsTyping.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }

    // Media Attachments Lists
    val selectedImages = remember { mutableStateListOf<Bitmap>() }
    val selectedFileNames = remember { mutableStateListOf<String>() }
    val selectedFileContents = remember { mutableStateListOf<String>() }

    // Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            readUriBitmap(context, uri)?.let { selectedImages.add(it) }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val parsed = readUriContent(context, uri)
            selectedFileNames.add(parsed.first)
            selectedFileContents.add(parsed.second)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { selectedImages.add(it) }
    }

    // Scroll to bottom on new message
    LaunchedEffect(llmMessages.size, llmIsTyping) {
        if (llmMessages.isNotEmpty()) {
            listState.animateScrollToItem(llmMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (llmMessages.isEmpty()) {
            // Ambient Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("🧠", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Mixy OS Neural Core",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Talk with google/diffusiongemma-26b-a4b-it powered by NVIDIA's local cloud infrastructure. Mixy will answer in a local male offline synthesizer voice.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                items(llmMessages) { msg ->
                    LlmChatBubble(
                        message = msg,
                        onExecuteAction = { action -> viewModel.executePendingLlmAction(msg.id, action) },
                        onRejectAction = { viewModel.rejectPendingLlmAction(msg.id) }
                    )
                }

                if (llmIsTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TypingIndicator()
                        }
                    }
                }
            }
        }

        // Selected Media Previews Panel
        if (selectedImages.isNotEmpty() || selectedFileNames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedImages.forEachIndexed { index, bitmap ->
                    Box(modifier = Modifier.size(54.dp)) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImages.removeAt(index) },
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }

                selectedFileNames.forEachIndexed { index, name ->
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .widthIn(max = 120.dp)
                            .background(CyberPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyberPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedFileNames.removeAt(index)
                                selectedFileContents.removeAt(index)
                            },
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }

        // Bottom Input Toolbar
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            cornerRadius = 16.dp,
            borderColor = Color.White.copy(alpha = 0.12f),
            backgroundColor = Color(0x22050914)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Media Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Photo, contentDescription = "Gallery", tint = CyberCyan)
                        }
                        IconButton(onClick = { cameraLauncher.launch(null) }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = CyberCyan)
                        }
                        IconButton(onClick = { fileLauncher.launch("*/*") }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = CyberCyan)
                        }
                    }

                    // Voice Dictation
                    val voiceTint = if (isListening) CyberPink else Color.White
                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = voiceTint
                        )
                    }
                }

                // Voice Waveform Overlay during active dictation
                if (isListening) {
                    val levels by viewModel.voiceWaveLevels.collectAsState()
                    val queryText by viewModel.speechText.collectAsState()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        VoiceWaveforms(
                            waveLevels = levels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            waveColor = CyberPink
                        )
                        if (queryText.isNotEmpty()) {
                            Text(
                                text = queryText,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // TextInput Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Command Mixy OS Brain...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mind_chat_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.trim().isNotEmpty() || selectedImages.isNotEmpty()) {
                                viewModel.sendLlmMessage(
                                    prompt = inputText,
                                    imageBitmaps = selectedImages.toList(),
                                    fileNames = selectedFileNames.toList(),
                                    fileContents = selectedFileContents.toList()
                                )
                                inputText = ""
                                selectedImages.clear()
                                selectedFileNames.clear()
                                selectedFileContents.clear()
                                focusManager.clearFocus()
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty() || selectedImages.isNotEmpty()) {
                                viewModel.sendLlmMessage(
                                    prompt = inputText,
                                    imageBitmaps = selectedImages.toList(),
                                    fileNames = selectedFileNames.toList(),
                                    fileContents = selectedFileContents.toList()
                                )
                                inputText = ""
                                selectedImages.clear()
                                selectedFileNames.clear()
                                selectedFileContents.clear()
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(CyberCyan, CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = SpaceDark, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LlmChatBubble(
    message: LlmMessage,
    onExecuteAction: (MixyAction) -> Unit,
    onRejectAction: () -> Unit
) {
    val containerColor = if (message.isUser) CyberPurple.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.08f)
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    val borderColor = if (message.isUser) CyberPurple.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.25f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    borderColor = borderColor,
                    backgroundColor = containerColor
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Render Previews inside bubble if user sent media
                        if (message.imageBitmaps.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.imageBitmaps.forEach { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        if (message.fileNames.isNotEmpty()) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                message.fileNames.forEach { name ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(name, color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        // Message Text
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Confirmation Action Panel under bubble if AI returned a supported system action
                if (!message.isUser && message.pendingAction != null && !message.actionExecuted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan, RoundedCornerShape(14.dp)),
                        cornerRadius = 14.dp,
                        borderColor = CyberCyan,
                        backgroundColor = Color(0x3300F0FF)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTION REQUESTED",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mixy detected a local system command:\nAction: ${message.pendingAction.action}\nDetails: ${message.pendingAction.appName ?: message.pendingAction.settingType ?: "Trigger Device Core"}",
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onRejectAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text("Dismiss", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onExecuteAction(message.pendingAction) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(34.dp)
                                ) {
                                    Text("Confirm Exec", color = SpaceDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
fun readUriContent(context: Context, uri: Uri): Pair<String, String> {
    val contentResolver = context.contentResolver
    val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else null
    } ?: "document.txt"

    val content = try {
        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (e: Exception) {
        ""
    }
    return fileName to content
}

fun readUriBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "Typing")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 0), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot1Scale; scaleY = dot1Scale }.background(CyberCyan, CircleShape))
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot2Scale; scaleY = dot2Scale }.background(CyberCyan, CircleShape))
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot3Scale; scaleY = dot3Scale }.background(CyberCyan, CircleShape))
    }
}

@Composable
fun NeuralOrbSection(status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowColor = when (status) {
        "Ready" -> CyberCyan
        "Downloading" -> CyberPurple
        "Initializing" -> CyberPink
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .size(130.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
                        center = center,
                        radius = size.width * 0.7f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(glowColor, glowColor.copy(alpha = 0.3f))
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = SpaceDark,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun SpecRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberCyan.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
