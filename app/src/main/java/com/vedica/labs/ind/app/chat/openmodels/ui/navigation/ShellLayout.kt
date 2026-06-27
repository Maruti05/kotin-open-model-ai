package com.vedica.labs.ind.app.chat.openmodels.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vedica.labs.ind.app.chat.openmodels.ui.chat.ChatScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.dashboard.DashboardScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager.ModelManagerScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.settings.SettingsScreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.TabCyan
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.TabGreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.TabIndigo
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.TabPurple

enum class Screen(val label: String, val icon: ImageVector, val accentColor: androidx.compose.ui.graphics.Color) {
    TELEMETRY("Telemetry", Icons.Outlined.Speed, TabCyan),
    REPOSITORY("Repository", Icons.Outlined.CloudDownload, TabIndigo),
    CHAT("Chat", Icons.AutoMirrored.Outlined.Chat, TabGreen),
    SETTINGS("Settings", Icons.Outlined.Tune, TabPurple)
}

@Composable
fun ShellLayout() {
    var selectedScreen by remember { mutableStateOf(Screen.CHAT) }

    val contentInsets = WindowInsets.systemBars

    Scaffold(
        contentWindowInsets = contentInsets,
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
                    val direction = targetState.ordinal - initialState.ordinal
                    if (direction > 0) {
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { fullWidth -> -fullWidth }
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { fullWidth -> fullWidth }
                        )
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.TELEMETRY -> DashboardScreen()
                    Screen.REPOSITORY -> ModelManagerScreen()
                    Screen.CHAT -> ChatScreen(
                        onNavigateToRepository = { selectedScreen = Screen.REPOSITORY }
                    )
                    Screen.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
