package com.jairopaiva.exprlab.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jairopaiva.exprlab.ui.screens.ExpressionLabScreen
import com.jairopaiva.exprlab.ui.screens.HomeScreen
import com.jairopaiva.exprlab.ui.screens.KeyStudioScreen
import com.jairopaiva.exprlab.ui.screens.LabsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Keys("Keys", Icons.Default.Lock),
    Expressions("Expressions", Icons.Default.List),
    Labs("Labs", Icons.Default.Star)
}

@Composable
fun AppRoot() {
    var selected by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selected) {
                Tab.Home -> HomeScreen(
                    onOpenKeys = { selected = Tab.Keys },
                    onOpenExpressions = { selected = Tab.Expressions },
                    onOpenLabs = { selected = Tab.Labs }
                )
                Tab.Keys -> KeyStudioScreen()
                Tab.Expressions -> ExpressionLabScreen()
                Tab.Labs -> LabsScreen()
            }
        }
    }
}
