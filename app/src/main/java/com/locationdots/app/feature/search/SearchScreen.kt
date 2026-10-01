package com.locationdots.app.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.search.SearchResult
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveListRow
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
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        query, onQueryChange, Modifier.fillMaxWidth(),
                        singleLine = true, placeholder = { Text("Search places and journeys") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                    )
                },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        when {
            isSearching -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            query.trim().length < 2 -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Search your history", style = MaterialTheme.typography.headlineSmall)
                    Text("Try a place name or movement type.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            results.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                ExpressiveCard { Text("No matches found", Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge) }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = {
                    when (it) { is SearchResult.PlaceResult -> "p-" + it.place.id; is SearchResult.VisitResult -> "v-" + it.event.id; is SearchResult.JourneyResult -> "j-" + it.event.id }
                }) { result ->
                    when (result) {
                        is SearchResult.PlaceResult -> ExpressiveListRow(result.place.name ?: "Unnamed place", "Saved place", { Icon(Icons.Default.Place, null) }, onClick = { onPlaceClick(result.place.id) })
                        is SearchResult.VisitResult -> ExpressiveListRow(result.event.place.name ?: "Unnamed place", "Visited · " + formatter.format(result.event.arrival), { Icon(Icons.Default.LocationOn, null) }, onClick = { onPlaceClick(result.event.place.id) })
                        is SearchResult.JourneyResult -> ExpressiveListRow("Journey to " + (result.event.endPlace?.name ?: "Unknown"), result.event.mode.label() + " · " + formatter.format(result.event.timestamp), { Icon(modeIcon(result.event.mode), null) }, onClick = { onJourneyClick(result.event.id) })
                    }
                }
            }
        }
    }
}

private fun JourneyMode.label(): String = when (this) { JourneyMode.WALKING -> "Walking"; JourneyMode.CYCLING -> "Cycling"; JourneyMode.VEHICLE -> "Driving"; JourneyMode.UNKNOWN -> "Movement" }
private fun modeIcon(mode: JourneyMode) = when (mode) { JourneyMode.WALKING -> Icons.Default.DirectionsWalk; JourneyMode.CYCLING -> Icons.Default.PedalBike; JourneyMode.VEHICLE -> Icons.Default.DirectionsCar; JourneyMode.UNKNOWN -> Icons.Default.Route }
