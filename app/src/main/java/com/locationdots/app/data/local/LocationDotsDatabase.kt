package com.locationdots.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDotsDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun placeDao(): PlaceDao
}
