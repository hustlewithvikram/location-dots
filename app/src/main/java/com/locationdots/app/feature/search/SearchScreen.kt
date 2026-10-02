package com.locationdots.app.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.search.SearchResult
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveIconBadge
import com.locationdots.app.ui.components.ExpressiveIconButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchScreen(
    query: String,
    results: List<SearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconButton(
                onClick = onBack,
                icon = { Icon(Icons.Default.ArrowBack, "Back") },
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Search",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            singleLine = true,
            placeholder = { Text("Places or journeys") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(22.dp)
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isSearching -> SearchLoading()

                query.trim().length < 2 -> SearchEmpty(
                    icon = Icons.Default.Search,
                    title = "Search your timeline",
                    message = "Find saved places, visits and journeys."
                )

                results.isEmpty() -> SearchEmpty(
                    icon = Icons.Default.SearchOff,
                    title = "No results",
                    message = "Try a different place or journey."
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        top = 4.dp,
                        end = 18.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        results,
                        key = {
                            when (it) {
                                is SearchResult.PlaceResult -> "p-" + it.place.id
                                is SearchResult.VisitResult -> "v-" + it.event.id
                                is SearchResult.JourneyResult -> "j-" + it.event.id
                            }
                        }
                    ) { result ->
                        when (result) {
                            is SearchResult.PlaceResult -> SearchResultCard(
                                title = result.place.name ?: "Unnamed place",
                                subtitle = "Saved place",
                                icon = Icons.Default.Place,
                                onClick = { onPlaceClick(result.place.id) }
                            )

                            is SearchResult.VisitResult -> SearchResultCard(
                                title = result.event.place.name ?: "Unnamed place",
                                subtitle = "Visited · " + formatter.format(result.event.arrival),
                                icon = Icons.Default.LocationOn,
                                onClick = { onPlaceClick(result.event.place.id) }
                            )

                            is SearchResult.JourneyResult -> SearchResultCard(
                                title = "Journey to " + (result.event.endPlace?.name ?: "Unknown"),
                                subtitle = result.event.mode.label() + " · " + formatter.format(result.event.timestamp),
                                icon = modeIcon(result.event.mode),
                                onClick = { onJourneyClick(result.event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconBadge(
                modifier = Modifier.size(44.dp),
                icon = { Icon(icon, contentDescription = null) }
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchEmpty(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExpressiveIconBadge(
            modifier = Modifier.size(68.dp),
            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp)) }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

private fun JourneyMode.label(): String = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Driving"
    JourneyMode.UNKNOWN -> "Movement"
}

private fun modeIcon(mode: JourneyMode) = when (mode) {
    JourneyMode.WALKING -> Icons.Default.DirectionsWalk
    JourneyMode.CYCLING -> Icons.Default.PedalBike
    JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
    JourneyMode.UNKNOWN -> Icons.Default.Route
}
