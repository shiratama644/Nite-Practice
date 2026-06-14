package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotePool
import com.example.ui.MainViewModel
import com.example.ui.components.AnalyticsChart

@Composable
fun HistoryView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessionLogs by viewModel.sessionLogs.collectAsState()
    val mistakeLogs by viewModel.mistakeLogs.collectAsState()
    val dailyProgressPoints by viewModel.dailyProgressPoints.collectAsState()

    var showSpeedMode by remember { mutableStateOf(true) } // true: speed line, false: accuracy line

    // Calculate aggregated stats
    val totalAttempts = sessionLogs.size
    val avgReactionTimeMs = if (totalAttempts > 0) {
        sessionLogs.map { it.responseTimeMs }.average().toInt()
    } else {
        0
    }
    val correctPercentage = if (totalAttempts > 0) {
        (sessionLogs.count { it.isCorrect }.toFloat() / totalAttempts * 100).toInt()
    } else {
        0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Core Summary Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Average speed card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("平均回答時間", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (avgReactionTimeMs > 0) String.format("%.2f 秒", avgReactionTimeMs / 1000f) else "--",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Accuracy Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text("通算正解率", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (totalAttempts > 0) "$correctPercentage%" else "--",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Total runs log
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("総トレーニング回数", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$totalAttempts 回回答",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Custom Line Chart Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "成長グラフ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        // Switch buttons
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp))
                                .padding(2.dp)
                        ) {
                            val activeColor = MaterialTheme.colorScheme.primary
                            val activeTextColor = MaterialTheme.colorScheme.onPrimary
                            val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (showSpeedMode) activeColor else Color.Transparent)
                                    .clickable { showSpeedMode = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("chart_tab_speed"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "速度", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (showSpeedMode) activeTextColor else inactiveTextColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (!showSpeedMode) activeColor else Color.Transparent)
                                    .clickable { showSpeedMode = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("chart_tab_accuracy"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "正解率", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (!showSpeedMode) activeTextColor else inactiveTextColor
                                )
                            }
                        }
                    }

                    // Render Custom Canvas Line Plot
                    AnalyticsChart(
                        dataPoints = dailyProgressPoints,
                        showSpeed = showSpeedMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_chart_canvas")
                    )
                }
            }
        }

        // 3. Weak Notes / Mistake Target List
        item {
            Text(
                text = "現在克服中の音符 (${mistakeLogs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (mistakeLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "苦手な音符はありません！完璧です！",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "間違えた音符は、ここに自動で記録され優先的に出題されます。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(mistakeLogs) { log ->
                val noteObj = NotePool.getNoteById(log.noteKey)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("mistake_item_${log.noteKey}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (log.clef == "TREBLE") {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.clef == "TREBLE") Icons.Default.MusicNote else Icons.Default.MusicVideo,
                                    contentDescription = null,
                                    tint = if (log.clef == "TREBLE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }

                            Column {
                                Text(
                                    text = if (log.clef == "TREBLE") "【ト音】 ${noteObj?.japaneseName ?: ""} (${noteObj?.displayPitch ?: ""})" else "【ヘ音】 ${noteObj?.japaneseName ?: ""} (${noteObj?.displayPitch ?: ""})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "間違えた回数: ${log.mistakeCount}回",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Done / Mastered check button
                        IconButton(
                            onClick = { viewModel.deleteMistake(log.noteKey) },
                            modifier = Modifier.testTag("clear_mistake_${log.noteKey}")
                        ) {
                            // Actually we can master it directly. Let's make sure they can tap to study or delete.
                            // In viewModel, we have decrement or remove. Let's make it a delete button to clear it manually!
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Clear mistake log",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
