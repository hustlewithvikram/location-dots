package com.locationdots.app.domain.insights

import com.locationdots.app.domain.model.JourneyMode

data class InsightsSnapshot(
    val totalPlaces: Int = 0,
    val totalVisits: Int = 0,
    val totalTimeMinutes: Long = 0,
    val totalDistanceMeters: Double = 0.0,
    val journeyCount: Int = 0,
    val modeBreakdown: Map<JourneyMode, Int> = emptyMap(),
    val topPlaces: List<PlaceInsight> = emptyList(),
    val dailyVisits: List<DailyInsight> = emptyList()
)

data class PlaceInsight(
    val placeId: String,
    val name: String,
    val visits: Int,
    val timeMinutes: Long
)

data class DailyInsight(
    val label: String,
    val visits: Int,
    val journeys: Int
)
