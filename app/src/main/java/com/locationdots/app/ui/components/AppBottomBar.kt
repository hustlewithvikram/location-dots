package com.locationdots.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

enum class AppTab(val label: String) { TIMELINE("Timeline"), MAP("Places"), INSIGHTS("Insights"), SETTINGS("Settings") }

@Composable
fun AppBottomBar(selected: AppTab, onSelected: (AppTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(selected == AppTab.TIMELINE, { onSelected(AppTab.TIMELINE) }, { Icon(Icons.Default.Timeline, null) }, label = { Text("Timeline") })
        NavigationBarItem(selected == AppTab.MAP, { onSelected(AppTab.MAP) }, { Icon(Icons.Default.Map, null) }, label = { Text("Places") })
        NavigationBarItem(selected == AppTab.INSIGHTS, { onSelected(AppTab.INSIGHTS) }, { Icon(Icons.Default.Analytics, null) }, label = { Text("Insights") })
        NavigationBarItem(selected == AppTab.SETTINGS, { onSelected(AppTab.SETTINGS) }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}
