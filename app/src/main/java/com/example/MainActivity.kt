package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.MixyViewModel
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Outer cyber glow background wrapper
                GlowBackground {
                    MainAppShell()
                }
            }
        }
    }
}

@Composable
fun MainAppShell() {
    val viewModel: MixyViewModel = viewModel()
    var currentTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // High-fidelity custom glassmorphism bottom navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    cornerRadius = 24.dp,
                    borderColor = Color.White.copy(alpha = 0.15f),
                    backgroundColor = Color(0x1F0A0E1A) // Semi-transparent cosmic slate
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabNavItem(
                            icon = Icons.Default.ChatBubble,
                            label = "Assistant",
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            tag = "nav_tab_assistant"
                        )
                        TabNavItem(
                            icon = Icons.Default.Dashboard,
                            label = "Dashboard",
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            tag = "nav_tab_dashboard"
                        )
                        TabNavItem(
                            icon = Icons.Default.AutoMode,
                            label = "Automate",
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            tag = "nav_tab_automate"
                        )
                        TabNavItem(
                            icon = Icons.Default.Folder,
                            label = "Files",
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            tag = "nav_tab_files"
                        )
                        TabNavItem(
                            icon = Icons.Default.Psychology,
                            label = "LLM",
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            tag = "nav_tab_llm"
                        )
                        TabNavItem(
                            icon = Icons.Default.Terminal,
                            label = "Console",
                            selected = currentTab == 5,
                            onClick = { currentTab = 5 },
                            tag = "nav_tab_console"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Fade transition on screen switching
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(250)).togetherWith(fadeOut(tween(250)))
                },
                label = "ScreenSwitch"
            ) { targetTab ->
                when (targetTab) {
                    0 -> AssistantScreen(
                        viewModel = viewModel,
                        onOpenSettings = { currentTab = 5 }
                    )
                    1 -> DashboardScreen(
                        viewModel = viewModel
                    )
                    2 -> AutomationScreen(
                        viewModel = viewModel
                    )
                    3 -> FileManagerScreen(
                        viewModel = viewModel
                    )
                    4 -> BrainScreen(
                        viewModel = viewModel
                    )
                    5 -> OSConsoleScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun TabNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val activeColor = CyberCyan
    val inactiveColor = Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) Color.White else inactiveColor,
            fontSize = 10.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSConsoleScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    var consoleSubTab by remember { mutableStateOf(0) } // 0: Notifications, 1: Accessibility AI

    Column(modifier = modifier.fillMaxSize()) {
        // High fidelity glass tab selectors
        TabRow(
            selectedTabIndex = consoleSubTab,
            containerColor = Color.Transparent,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[consoleSubTab])
                        .height(3.dp)
                        .background(CyberCyan)
                )
            }
        ) {
            Tab(
                selected = consoleSubTab == 0,
                onClick = { consoleSubTab = 0 },
                text = { Text("Smart Notifications", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = consoleSubTab == 1,
                onClick = { consoleSubTab = 1 },
                text = { Text("Accessibility AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (consoleSubTab == 0) {
                NotificationScreen(viewModel)
            } else {
                AccessibilityScreen(viewModel)
            }
        }
    }
}
