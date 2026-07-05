package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.MixyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: MixyViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var logMsg by remember { mutableStateOf("Ready to scan storage directories.") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val folders = listOf(
        FolderItem("Downloads", "42 items • 1.2 GB", Icons.Default.Download, Color(0xFF00E676)),
        FolderItem("Documents", "12 items • 15.4 MB", Icons.Default.Description, CyberCyan),
        FolderItem("Camera Roll", "154 items • 4.8 GB", Icons.Default.Image, CyberPink),
        FolderItem("Audio Notes", "8 items • 120.5 MB", Icons.Default.AudioFile, CyberPurple)
    )

    val duplicatedFiles = listOf(
        FileMetadata("screenshot_2026_07.png", "Downloads", "1.8 MB", "Duplicate of /Pictures/screen1.png"),
        FileMetadata("lecture_notes.pdf", "Documents", "4.2 MB", "Duplicate of /Downloads/lecture_notes_copy.pdf"),
        FileMetadata("temp_audio_file.wav", "Audio Notes", "12.0 MB", "Duplicate of /Audio/temp_record.wav")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Mixy File Matrix", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Cybernetic File Directory", color = CyberCyan, fontSize = 12.sp)
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

            // Glossy File Search Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search system directories...", color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Folders Grid Layout
            Text("Directories", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(180.dp)
            ) {
                items(folders) { folder ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable {
                                searchQuery = folder.name
                                logMsg = "Scanning directory '${folder.name}'... Matches located."
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(folder.color.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, folder.color.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = folder.icon, contentDescription = null, tint = folder.color, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(folder.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(folder.details, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive action bay
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Matrix Storage Cleaner", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(logMsg, color = CyberCyan, fontSize = 11.sp, maxLines = 2)
                    }

                    CyberButton(
                        text = if (isAnalyzing) "Scanning..." else "Analyze duplicates",
                        onClick = {
                            isAnalyzing = true
                            logMsg = "De-duplication sweep running..."
                            viewModel.speakText("Analyzing file structures for duplicated copies.")
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isAnalyzing = false
                                logMsg = "Sweep complete! Safely located 3 duplicates (18.0 MB space recoverable)."
                            }, 2500)
                        },
                        glowColor = CyberPink
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Duplicate Files Found List
            Text("Recoverable Cache Duplicates", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(duplicatedFiles) { file ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(CyberPink.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.FileCopy, contentDescription = null, tint = CyberPink, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(file.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(file.reason, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(file.size, color = CyberPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                        .clickable {
                                            logMsg = "Purged '${file.name}' from storage cache."
                                            viewModel.speakText("Cleaned ${file.name}.")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Purge file", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FolderItem(val name: String, val details: String, val icon: ImageVector, val color: Color)
data class FileMetadata(val name: String, val folderName: String, val size: String, val reason: String)
