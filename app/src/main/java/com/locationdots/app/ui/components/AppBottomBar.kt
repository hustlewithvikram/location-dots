package com.locationdots.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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

enum class AppTab(val label: String) { TIMELINE("Timeline"), MAP("Places"), INSIGHTS("Insights"), SETTINGS("Settings") }

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

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = androidx.compose.animation.core.tween(
                    420,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "navigation-indicator"
            )

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset + 8.dp)
                    .width(itemWidth - 16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )

            Row(Modifier.fillMaxWidth()) {
                tabs.forEach { (tab, icon) ->
                    val isSelected = tab == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { onSelected(tab) }
                            .semantics {
                                semanticsSelected = isSelected
                                role = Role.Tab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
