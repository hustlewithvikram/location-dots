package com.locationdots.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected as semanticsSelected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class AppTab(val label: String) {
    TIMELINE("Timeline"),
    MAP("Places"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

@Composable
fun AppBottomBar(selected: AppTab, onSelected: (AppTab) -> Unit) {
    val tabs = remember {
        listOf(
            AppTab.TIMELINE to Icons.Default.Timeline,
            AppTab.MAP to Icons.Default.Map,
            AppTab.INSIGHTS to Icons.Default.Analytics,
            AppTab.SETTINGS to Icons.Default.Settings
        )
    }
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 0.dp
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
                val itemWidth = maxWidth / tabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 700f,
                        visibilityThreshold = 0.5.dp
                    ),
                    label = "navigation-pill"
                )

                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                Row(Modifier.fillMaxSize()) {
                    tabs.forEach { (tab, icon) ->
                        val isSelected = tab == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable { onSelected(tab) }
                                .semantics {
                                    semanticsSelected = isSelected
                                    role = Role.Tab
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(25.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}