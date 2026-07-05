package com.example.ui.screens

import androidx.compose.animation.core.*
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
fun AccessibilityScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.systemLogs.collectAsState()
    var isEnabled by remember { mutableStateOf(true) }
    var simulatorText by remember { mutableStateOf("Ready to capture target screen metrics.") }
    var screenAnalysisActive by remember { mutableStateOf(false) }

    // Mock elements scanned from active simulator screen
    val scannedElements = listOf(
        ScreenElement("Instagram Icon", 120, 430, "App icon, package: com.instagram.android"),
        ScreenElement("Like Button", 450, 890, "Interactive Button"),
        ScreenElement("Comments text field", 320, 1020, "Input target, focused: false"),
        ScreenElement("Confirm transaction box", 540, 1150, "Muted alert dialog")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Accessibility AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Autonomous Operator Service", color = CyberCyan, fontSize = 12.sp)
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

            // Service status tile
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isEnabled) Color(0xFF00FF88).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, if (isEnabled) Color(0xFF00FF88).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = if (isEnabled) Color(0xFF00FF88) else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Accessibility Daemon", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isEnabled) "ACTIVE - Listening for OS overlays" else "INACTIVE - Tap to grant settings", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            viewModel.speakText("Accessibility service set to $it.")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FF88),
                            checkedTrackColor = CyberPurple.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Screen Context Simulator
            Text("Simulated Screen Viewport Scanner", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mock OS Frame Capture", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (screenAnalysisActive) CyberPink else CyberCyan, CircleShape)
                            )
                        }
                        
                        Text(
                            text = if (screenAnalysisActive) "Running OCR scans & neural layouts..." else simulatorText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CyberButton(
                            text = "Analyze Context",
                            onClick = {
                                screenAnalysisActive = true
                                viewModel.speakText("Capturing and analyzing screen structure.")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    screenAnalysisActive = false
                                    simulatorText = "Neural Sweep Complete! Captured 4 interactive nodes, active app detected as Instagram."
                                }, 2200)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        CyberButton(
                            text = "Dispatched Tap Simulation",
                            onClick = {
                                simulatorText = "Emulated tap at grid matrix X: 450, Y: 890 (Like Button executed)."
                                viewModel.speakText("Dispatching touch simulation on target node.")
                            },
                            glowColor = CyberPink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OCR Elements Scanned List
            Text("OCR Coordinate Overlays", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scannedElements) { element ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                simulatorText = "Dispatched tap coordinates [X: ${element.x}, Y: ${element.y}] targetting ${element.label}."
                                viewModel.speakText("Tapping ${element.label}.")
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(element.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(element.details, color = Color.Gray, fontSize = 10.sp)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .background(CyberCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("[${element.x}, ${element.y}]", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ScreenElement(val label: String, val x: Int, val y: Int, val details: String)
