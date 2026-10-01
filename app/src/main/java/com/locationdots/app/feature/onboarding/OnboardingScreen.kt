package com.locationdots.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    hasLocationPermission: Boolean,
    isTracking: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Location Dots",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = if (hasLocationPermission) {
                "Location access is ready."
            } else {
                "Allow location access to build your personal timeline."
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )

        if (!hasLocationPermission) {
            Button(onClick = onRequestLocationPermission) {
                Text("Allow location")
            }
        } else if (isTracking) {
            OutlinedButton(onClick = onStopTracking) {
                Text("Stop tracking")
            }
        } else {
            Button(onClick = onStartTracking) {
                Text("Start tracking")
            }
        }
    }
}
