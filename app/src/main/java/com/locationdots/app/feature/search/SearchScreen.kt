package com.locationdots.app.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.search.SearchResult
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search places and journeys") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            }
        )

        when {
            isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            query.trim().length < 2 -> SearchHint()

            results.isEmpty() -> SearchEmpty()

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { result ->
                    when (result) {
                        is SearchResult.PlaceResult -> "place:" + result.place.id
                        is SearchResult.VisitResult -> "visit:" + result.event.id
                        is SearchResult.JourneyResult -> "journey:" + result.event.id
                    }
                }) { result ->
                    SearchResultRow(result, formatter, onPlaceClick, onJourneyClick)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    formatter: DateTimeFormatter,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    val title: String
    val subtitle: String
    val icon = when (result) {
        is SearchResult.PlaceResult -> Icons.Default.Place
        is SearchResult.VisitResult -> Icons.Default.LocationOn
        is SearchResult.JourneyResult -> when (result.event.mode) {
            JourneyMode.WALKING -> Icons.Default.DirectionsWalk
            JourneyMode.CYCLING -> Icons.Default.PedalBike
            JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
            JourneyMode.UNKNOWN -> Icons.Default.LocationOn
        }
    }

    val click: () -> Unit

    when (result) {
        is SearchResult.PlaceResult -> {
            title = result.place.name ?: "Unnamed place"
            subtitle = "%.5f, %.5f".format(Locale.US, result.place.latitude, result.place.longitude)
            click = { onPlaceClick(result.place.id) }
        }
        is SearchResult.VisitResult -> {
            title = result.event.place.name ?: "Unnamed place"
            subtitle = "Visited · " + formatter.format(result.event.arrival)
            click = { onPlaceClick(result.event.place.id) }
        }
        is SearchResult.JourneyResult -> {
            title = "Journey to " + (result.event.endPlace?.name ?: "Unnamed place")
            subtitle = result.event.mode.label() + " · " + formatter.format(result.event.timestamp)
            click = { onJourneyClick(result.event.id) }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = click),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchHint() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text("Search your history", style = MaterialTheme.typography.titleLarge)
            Text(
                "Try a place name or movement type.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchEmpty() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No matches", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Nothing in your saved history matches this search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun JourneyMode.label(): String = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Vehicle"
    JourneyMode.UNKNOWN -> "Movement"
}
