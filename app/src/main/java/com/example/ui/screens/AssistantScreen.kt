package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.MixyViewModel
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: MixyViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe State flows from ViewModel
    val chatMessages by viewModel.recentCommands.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val speechText by viewModel.speechText.collectAsState()
    val waveLevels by viewModel.voiceWaveLevels.collectAsState()
    val highThinking by viewModel.highThinkingMode.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var textInput by remember { mutableStateFlowOf("") }
    var showNameDialog by remember { mutableStateOf(false) }

    // Dynamic greeting based on current local hours
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // Double Ring Pulse animation for central reactor
    val infiniteTransition = rememberInfiniteTransition(label = "ReactorPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Load generated logo from asset/drawable safely
    val logoBitmap = remember {
        try {
            val stream: InputStream = context.assets.open("ic_mixy_logo.jpg")
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        } catch (e: Exception) {
            try {
                val id = context.resources.getIdentifier("ic_mixy_logo", "drawable", context.packageName)
                if (id != 0) BitmapFactory.decodeResource(context.resources, id)?.asImageBitmap() else null
            } catch (ex: Exception) {
                null
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            text = "Mixy OS",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        )
                        Text(
                            text = "AI Assistant",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // High thinking toggle chip
                    FilterChip(
                        selected = highThinking,
                        onClick = { viewModel.toggleHighThinking(!highThinking) },
                        label = { Text("High Thinking", color = Color.White, fontSize = 9.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (highThinking) CyberPink else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberPurple.copy(alpha = 0.3f),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "System log configuration",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Central Pulsing Avatar Reactor (Sleek Compact Size)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clickable {
                        if (isListening) viewModel.stopListening() else viewModel.startListening()
                    }
            ) {
                // Outer breathing halo ring
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .scale(if (isListening || isSpeaking) pulseScale else 1.0f)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(CyberCyan, CyberPurple, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Inner soft glowing ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (isListening || isSpeaking) pulseScale * 0.95f else 1.0f)
                        .border(
                            width = 1.dp,
                            color = CyberPink.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .background(SpaceCardBg, CircleShape)
                )

                // The glowing logo inside
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = "Mixy Reactor Core",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Fallback gorgeous geometric logo drawn on canvas
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyberPurple, CyberCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Core unit",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // If loading / thinking, overlay a spinner
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = CyberPink,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Name and Greeting Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showNameDialog = true }
            ) {
                Text(
                    text = "$greeting, ",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = userName,
                    color = CyberCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename user profile",
                    tint = CyberCyan.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(12.dp)
                )
            }

            Text(
                text = if (isListening) "I am listening... speak now." else "How can I help you today?",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text Terminal / Command Search Input
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Command spark icon",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )

                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type a command or ask anything...", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.trim().isNotEmpty()) {
                                    viewModel.handleUserCommand(textInput, isVoice = false)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (isListening) viewModel.stopListening() else viewModel.startListening()
                        },
                        modifier = Modifier.testTag("mic_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice mode",
                            tint = if (isListening) CyberPink else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recent Commands List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Commands",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Purge Cache",
                    color = CyberPink,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { viewModel.clearChatHistory() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Recents Command List
            if (chatMessages.isEmpty()) {
                // Default custom JARVIS starter commands when database is empty
                val starterCommands = listOf(
                    Triple("Call Mom", "Dial default caregiver number", Icons.Outlined.Phone to Color(0xFF4CAF50)),
                    Triple("Open Camera", "Initialize lens hardware", Icons.Default.CameraAlt to Color(0xFFFF9800)),
                    Triple("Set alarm for 6 AM", "Configure local waking clock", Icons.Default.Alarm to Color(0xFFE91E63))
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(starterCommands) { item ->
                        CommandItemCard(
                            title = item.first,
                            subtitle = item.second,
                            icon = item.third.first,
                            iconTint = item.third.second,
                            onClick = { viewModel.handleUserCommand(item.first, isVoice = false) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { chat ->
                        // Automatically map icons based on Action types
                        val iconInfo = remember(chat.actionPlanned) {
                            when (chat.actionPlanned) {
                                "MAKE_CALL", "SEND_SMS" -> Icons.Outlined.Phone to Color(0xFF00E676)
                                "OPEN_APP", "SYSTEM_SETTING" -> Icons.Default.Apps to Color(0xFF00B0FF)
                                "SET_ALARM", "SET_TIMER" -> Icons.Default.Alarm to Color(0xFFFF4081)
                                "LAUNCH_CAMERA" -> Icons.Default.CameraAlt to Color(0xFFFFD600)
                                "FLASHLIGHT" -> Icons.Default.FlashlightOn to Color(0xFF00E5FF)
                                else -> Icons.Outlined.ChatBubbleOutline to Color(0xFFD1C4E9)
                            }
                        }

                        val timeString = remember(chat.timestamp) {
                            try {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(chat.timestamp))
                            } catch (e: Exception) {
                                "Today"
                            }
                        }

                        CommandItemCard(
                            title = chat.query,
                            subtitle = if (chat.response.length > 55) chat.response.take(52) + "..." else chat.response,
                            icon = iconInfo.first,
                            iconTint = iconInfo.second,
                            onClick = { viewModel.handleUserCommand(chat.query, isVoice = false) }
                        )
                    }
                }
            }

            // Real-time Voice waves at the bottom
            if (isListening || isSpeaking || speechText.isNotEmpty()) {
                Text(
                    text = speechText.ifEmpty { "Speech analysis active..." },
                    color = CyberCyan,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                VoiceWaveforms(
                    waveLevels = waveLevels,
                    waveColor = if (isSpeaking) CyberPink else CyberPurple,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Name modification popup dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            containerColor = Color(0xFF131722),
            onDismissRequest = { showNameDialog = false },
            title = { Text("Rename OS Operator", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Operator Name", color = CyberCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.trim().isNotEmpty()) {
                            viewModel.saveUserName(tempName.trim())
                        }
                        showNameDialog = false
                    }
                ) {
                    Text("Confirm", color = CyberCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CommandItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored icon container (Compact)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, iconTint.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Execute again",
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Helper state creator
private fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
