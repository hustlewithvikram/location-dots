package com.locationdots.app.domain.insights

interface InsightsRepository {
    suspend fun getSnapshot(): InsightsSnapshot
}
