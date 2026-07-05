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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.MixyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    val ruleList by viewModel.automations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Mixy Automations", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Autonomous Device Operations", color = CyberCyan, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_automation_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add new routine", tint = CyberCyan)
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

            // Informative Hero Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(CyberPurple.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, CyberPurple.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text("Active Daemon Triggers", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("When criteria are reached, Mixy schedules intents, manages configurations, and launches apps automatically.", color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Automations list
            if (ruleList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("No automations active yet.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ruleList) { rule ->
                        AutomationRuleItem(
                            rule = rule,
                            onToggle = { viewModel.toggleAutomationActive(rule) },
                            onDelete = { viewModel.deleteAutomationRule(rule) },
                            onTriggerSimulated = { viewModel.triggerAutomationSimulated(rule) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var triggerType by remember { mutableStateOf("TIME") } // TIME, BATTERY
        var triggerValue by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }

        AlertDialog(
            containerColor = Color(0xFF131722),
            onDismissRequest = { showAddDialog = false },
            title = { Text("Assemble Custom Automation", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Routine Label", color = CyberCyan) },
                        placeholder = { Text("e.g., Morning Wakeup", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Trigger Type chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = triggerType == "TIME",
                            onClick = { triggerType = "TIME" },
                            label = { Text("Time-based Trigger", color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberPurple.copy(alpha = 0.3f))
                        )
                        FilterChip(
                            selected = triggerType == "BATTERY",
                            onClick = { triggerType = "BATTERY" },
                            label = { Text("Battery Level", color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberPurple.copy(alpha = 0.3f))
                        )
                    }

                    OutlinedTextField(
                        value = triggerValue,
                        onValueChange = { triggerValue = it },
                        label = { Text(if (triggerType == "TIME") "Time (HH:MM AM/PM)" else "Battery Threshold (e.g., 20%)", color = CyberCyan) },
                        placeholder = { Text(if (triggerType == "TIME") "08:30 AM" else "20%", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Target Phone Action (Natural Language)", color = CyberCyan) },
                        placeholder = { Text("e.g., Turn on flashlight", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotEmpty() && triggerValue.isNotEmpty() && command.isNotEmpty()) {
                            viewModel.addAutomation(title, triggerType, triggerValue, command)
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("SAVE RULE", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ABORT", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AutomationRuleItem(
    rule: com.example.data.database.AutomationRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTriggerSimulated: () -> Unit
) {
    val icon = if (rule.triggerType == "TIME") Icons.Outlined.Timer else Icons.Default.BatteryChargingFull
    val tintColor = if (rule.isActive) CyberCyan else Color.Gray

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTriggerSimulated, modifier = Modifier.size(24.dp).padding(end = 6.dp)) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Simulate action trigger", tint = Color(0xFF00FF88))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove routine", tint = CyberPink)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberPurple.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            Column {
                Text(
                    text = "TRIGGER: ${rule.triggerType} at '${rule.triggerValue}'",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ACTION: '${rule.actionCommand}'",
                    color = CyberCyan.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
