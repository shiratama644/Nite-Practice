package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.views.FlashcardView
import com.example.ui.views.HistoryView
import com.example.ui.views.SettingsView
import com.example.ui.views.TutorView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: MainViewModel = viewModel()
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                listOf(
                    Triple(AppTab.GAME, Icons.Default.MusicNote, "練習"),
                    Triple(AppTab.PROGRESS, Icons.Default.Analytics, "分析"),
                    Triple(AppTab.TUTOR, Icons.Default.Psychology, "AI解説"),
                    Triple(AppTab.SETTINGS, Icons.Default.Settings, "設定")
                ).forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated transitions between selected sub-views!
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screentransition"
            ) { tab ->
                when (tab) {
                    AppTab.GAME -> FlashcardView(viewModel = viewModel)
                    AppTab.PROGRESS -> HistoryView(viewModel = viewModel)
                    AppTab.TUTOR -> TutorView(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsView(viewModel = viewModel)
                }
            }
        }
    }
}

