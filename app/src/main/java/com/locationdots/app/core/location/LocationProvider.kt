package com.locationdots.app.core.location

import com.locationdots.app.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    val locations: Flow<LocationPoint>
    fun start()
    fun stop()
}
