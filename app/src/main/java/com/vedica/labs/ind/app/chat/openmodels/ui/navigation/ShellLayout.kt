package com.vedica.labs.ind.app.chat.openmodels.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vedica.labs.ind.app.chat.openmodels.ui.chat.ChatScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.dashboard.DashboardScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager.ModelManagerScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.settings.SettingsScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.*

enum class Screen(val label: String, val icon: ImageVector, val accentColor: androidx.compose.ui.graphics.Color) {
    TELEMETRY("Telemetry", Icons.Outlined.Speed, TabCyan),
    REPOSITORY("Repository", Icons.Outlined.CloudDownload, TabIndigo),
    CHAT("Chat", Icons.AutoMirrored.Outlined.Chat, TabGreen),
    SETTINGS("Settings", Icons.Outlined.Tune, TabPurple)
}

@Composable
fun ShellLayout() {
    var selectedScreen by remember { mutableStateOf(Screen.CHAT) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                tint = if (selectedScreen == screen) screen.accentColor
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                color = if (selectedScreen == screen) screen.accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = screen.accentColor.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.TELEMETRY -> DashboardScreen()
                    Screen.REPOSITORY -> ModelManagerScreen()
                    Screen.CHAT -> ChatScreen()
                    Screen.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
