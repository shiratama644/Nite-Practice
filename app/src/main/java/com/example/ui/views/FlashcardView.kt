package com.example.ui.views

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Star
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
import com.example.data.Clef
import com.example.data.Letter
import com.example.ui.ClefPreference
import com.example.ui.MainViewModel
import com.example.ui.components.StaffCanvas

@Composable
fun FlashcardView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val feedbackText by viewModel.feedbackText.collectAsState()
    val feedbackIsCorrect by viewModel.feedbackIsCorrect.collectAsState()
    val clefPref by viewModel.clefPreference.collectAsState()
    val reviewMode by viewModel.reviewModeEnabled.collectAsState()
    val mistakeLogs by viewModel.mistakeLogs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Selector segment for clefs & Review Mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented-style Chips
            listOf(
                ClefPreference.ALL to "すべて",
                ClefPreference.TREBLE to "ト音記号",
                ClefPreference.BASS to "ヘ音記号"
            ).forEach { (pref, title) ->
                FilterChip(
                    selected = clefPref == pref,
                    onClick = { viewModel.setClefPreference(pref) },
                    label = { Text(title) },
                    modifier = Modifier.weight(1f).testTag("chip_${pref.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Review mode toggle card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (reviewMode) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Review mistakes",
                        tint = if (reviewMode) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "苦手・誤答優先モード",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "未克服の音符: ${mistakeLogs.size}種類",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = reviewMode,
                    onCheckedChange = { viewModel.toggleReviewMode(it) },
                    modifier = Modifier.testTag("review_toggle")
                )
            }
        }

        // 2. Main Music Sheet Card (Canvas holder)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                currentNote?.let { note ->
                    StaffCanvas(note = note)
                } ?: CircularProgressIndicator()
            }
        }

        // 3. Status and Feedback display
        AnimatedVisibility(
            visible = feedbackIsCorrect != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (feedbackIsCorrect) {
                        true -> Color(0xFFE8F5E9)   // soft green
                        false -> Color(0xFFFFEBEE)  // soft red
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (feedbackIsCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "Result feedback",
                            tint = if (feedbackIsCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = feedbackText ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (feedbackIsCorrect == true) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        )
                    }

                    // Ask Gemini Tutor Button (shows when incorrect!)
                    if (feedbackIsCorrect == false) {
                        IconButton(
                            onClick = { viewModel.askTutor() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("ask_tutor_error_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = "Ask AI tutor for Mnemonics"
                            )
                        }
                    }
                }
            }
        }

        // 4. Input Answer Buttons / Next CTA Buttons
        if (feedbackIsCorrect == null) {
            // Note options ド、レ、ミ、ファ、ソ、ラ、シ (all 7 letters!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "音符の名前を選択してください：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Grid: 4 in row 1, 3 in row 2
                val letters = Letter.entries
                val row1 = letters.subList(0, 4)
                val row2 = letters.subList(4, 7)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { letter ->
                        Button(
                            onClick = { viewModel.submitAnswer(letter) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_${letter.name.lowercase()}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(letter.japanese, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text(letter.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.75f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { letter ->
                        Button(
                            onClick = { viewModel.submitAnswer(letter) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_${letter.name.lowercase()}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(letter.japanese, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text(letter.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        } else {
            // Next CTA Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary check AI Tutor Button
                OutlinedButton(
                    onClick = { viewModel.askTutor() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("ask_tutor_success_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI講師の覚え方")
                }

                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("next_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("次の音符へ")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
