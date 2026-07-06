package com.dual_shock.paralax.data.interpol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "Interpol_locations")
data class InterpolLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val lon: Double,
    val lat: Double
)

@Dao
interface InterpolDao {
    @Query("SELECT * FROM Interpol_locations")
    fun getInterpolLocations(): Flow<List<InterpolLocation>>

    @Query("SELECT * FROM Interpol_locations WHERE id = :id")
    fun getInterpolLocation(id: Int): Flow<List<InterpolLocation>>

    // square euclidean radius (not Haversine equation, doesn't account for earths curvature) uses euclidean distance formula without square root
    @Query(""" 
    SELECT * FROM Interpol_locations
    WHERE lat BETWEEN :lat - :delta AND :lat + :delta
    AND lon BETWEEN :lon - :delta AND :lon + :delta
    ORDER BY (lat - :lat) * (lat - :lat) + (lon - :lon) * (lon - :lon)
    LIMIT 1""")
    suspend fun getClosestInterpolLocation(
        lat: Double,
        lon: Double,
        delta: Double
    ): InterpolLocation?

    @Insert
    suspend fun insertAll(location: List<InterpolLocation>)

    @Delete
    suspend fun deleteAll(location: List<InterpolLocation>)
}