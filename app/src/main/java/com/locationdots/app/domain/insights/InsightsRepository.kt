package com.vikram.locationdots.domain.insights

interface InsightsRepository {
    suspend fun getSnapshot(): InsightsSnapshot
}
