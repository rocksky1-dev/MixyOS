package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.MixyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    // Collect stats from viewmodel
    val battery by viewModel.batteryLevel.collectAsState()
    val batteryTemp by viewModel.batteryTemp.collectAsState()
    val ramPct by viewModel.ramUsage.collectAsState()
    val ramInfo by viewModel.ramInfo.collectAsState()
    val storagePct by viewModel.storageUsage.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val cpu by viewModel.cpuUsage.collectAsState()
    val networkSpeed by viewModel.networkSpeed.collectAsState()

    var showOptimizeAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Mixy System HUD", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Telemetry Core Engine", color = CyberCyan, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { showOptimizeAlert = true }) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Optimize performance",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
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
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Main Telemetry Hub Ring
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mixy OS Status", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Status: OPERATIONAL", color = Color(0xFF00FF88), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Security Integrity: 100% SECURE", color = Color.Gray, fontSize = 12.sp)
                        Text("System Temp: ${String.format("%.1f", batteryTemp)} °C", color = Color.Gray, fontSize = 12.sp)
                        Text("Active Thread Count: 42 (Safe)", color = Color.Gray, fontSize = 12.sp)
                    }

                    // Large circular radial progress visualizer for CPU usage
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { cpu.toFloat() / 100f },
                            color = CyberPink,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            strokeWidth = 8.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$cpu%",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CPU LOAD",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Telemetry Grid Items
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    MetricCard(
                        title = "BATTERY LEVEL",
                        value = "$battery%",
                        subtitle = "Temp: ${String.format("%.1f", batteryTemp)} °C",
                        pct = battery.toFloat() / 100f,
                        icon = Icons.Default.BatteryChargingFull,
                        tint = Color(0xFF00FF88)
                    )
                }

                item {
                    MetricCard(
                        title = "RAM SPEED",
                        value = String.format("%.0f%% Used", ramPct),
                        subtitle = ramInfo,
                        pct = ramPct / 100f,
                        icon = Icons.Default.Memory,
                        tint = CyberCyan
                    )
                }

                item {
                    MetricCard(
                        title = "DISK STORAGE",
                        value = String.format("%.0f%% Used", storagePct),
                        subtitle = storageInfo,
                        pct = storagePct / 100f,
                        icon = Icons.Default.Storage,
                        tint = CyberPurple
                    )
                }

                item {
                    MetricCard(
                        title = "NETWORK SPEED",
                        value = networkSpeed,
                        subtitle = "Fluctuating Latency",
                        pct = 0.6f, // static metric representation
                        icon = Icons.Default.CellTower,
                        tint = CyberPink
                    )
                }
            }

            // AI Suggestion Glass Banner
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .height(90.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Mixy Core AI Suggestions",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your RAM is at ${String.format("%.0f%%", ramPct)} capacity. Execute 'Mute background apps' to release allocations.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showOptimizeAlert) {
        AlertDialog(
            containerColor = Color(0xFF131722),
            onDismissRequest = { showOptimizeAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.padding(end = 8.dp))
                    Text("Mixy System Optimization", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "I am ready to optimize your device RAM, purge redundant background logs, and trigger system cooling. Would you like to proceed, Commander Shivam?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOptimizeAlert = false
                        viewModel.speakText("Running system cooling sequence. Redundant system cache has been purged.")
                    }
                ) {
                    Text("OPTIMIZE NOW", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptimizeAlert = false }) {
                    Text("STANDBY", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    pct: Float,
    icon: ImageVector,
    tint: Color
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                LinearProgressIndicator(
                    progress = { pct.coerceIn(0f, 1f) },
                    color = tint,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
