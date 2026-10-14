package com.vikram.locationdots.core.location

import com.vikram.locationdots.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    val locations: Flow<LocationPoint>
    fun start()
    fun stop()
}
