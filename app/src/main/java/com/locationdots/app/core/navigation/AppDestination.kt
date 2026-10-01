package com.locationdots.app.core.navigation

sealed interface AppDestination {
    val route: String
    data object Timeline : AppDestination { override val route = "timeline" }
    data object Onboarding : AppDestination { override val route = "onboarding" }
    data object Settings : AppDestination { override val route = "settings" }
    data object Place : AppDestination { override val route = "place/{placeId}" }
}
