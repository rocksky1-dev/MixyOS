package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.MixyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    var notificationSummary by remember { mutableStateOf("Mixy core ready to compress notification stacks.") }
    var isSummarizing by remember { mutableStateOf(false) }

    val notifications = listOf(
        AppNotification("WhatsApp", "Shivam", "Hey, are we still meeting up for the AI presentation tonight?", "10 mins ago", listOf("Yes, on my way!", "Can we reschedule?", "Send location")),
        AppNotification("Gmail", "Google Workspace", "Weekly Project Integrity report finalized. Review safety standards.", "30 mins ago", emptyList()),
        AppNotification("LinkedIn", "Sanjay Kumar", "View new connections matching your Android development skillsets.", "1 hour ago", listOf("Awesome!", "Will review later")),
        AppNotification("Calendar", "Daily Sync", "Upcoming: Mixy OS feature review meeting starts in 15 mins.", "Just now", emptyList())
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Mixy Notifications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Intelligent Notification Summaries", color = CyberCyan, fontSize = 12.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Summary Action Deck
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Smart Summary", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(notificationSummary, color = CyberCyan, fontSize = 11.sp, maxLines = 3)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    CyberButton(
                        text = if (isSummarizing) "Compiling..." else "Summarize stack",
                        onClick = {
                            isSummarizing = true
                            viewModel.speakText("Compiling notifications summary.")
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isSummarizing = false
                                notificationSummary = "SUMMARY: Shivam is checking on your meeting. Google Workspace finalized projects, and an AI Review starts in 15 mins. No blocking action alerts."
                            }, 2000)
                        },
                        glowColor = CyberPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notification Stack List
            Text("Notification Stream", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItemCard(
                        notification = notification,
                        onSendReply = { reply ->
                            viewModel.speakText("Sending reply: $reply")
                            notificationSummary = "Action dispatched: Sent '$reply' to ${notification.sender} on ${notification.appName}."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onSendReply: (String) -> Unit
) {
    val iconColor = when (notification.appName) {
        "WhatsApp" -> Color(0xFF25D366)
        "Gmail" -> Color(0xFFEA4335)
        "LinkedIn" -> Color(0xFF0A66C2)
        else -> CyberCyan
    }

    val icon = when (notification.appName) {
        "WhatsApp", "LinkedIn" -> Icons.Outlined.ChatBubbleOutline
        "Gmail" -> Icons.Default.Email
        else -> Icons.Default.CalendarToday
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(iconColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, iconColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(notification.appName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(notification.time, color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(notification.sender, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(notification.message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

            // Quick Replies chips if any
            if (notification.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    notification.replies.forEach { reply ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, CyberPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .background(CyberPurple.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSendReply(reply) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(reply, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

data class AppNotification(
    val appName: String,
    val sender: String,
    val message: String,
    val time: String,
    val replies: List<String>
)
