package com.dual_shock.paralax.data.launchsites

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "LaunchSites")
data class LaunchSite(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val lon: Double,
    val lat: Double,
    val altitude: Int? = null,
    val interpolName: String? = null,
    val isDefault: Boolean = false
)

@Dao
interface LaunchSiteDao {
    @Query("SELECT * FROM LaunchSites")
    fun getLaunchSites(): Flow<List<LaunchSite>>

    @Query("SELECT * FROM LaunchSites WHERE id = :id")
    fun getLaunchSite(id: Int): Flow<List<LaunchSite>>

    @Insert
    suspend fun insert(site: LaunchSite): Long

    @Query("UPDATE LaunchSites SET name = :name, lat = :lat, lon = :lon, altitude = :altitude WHERE id = :id")
    suspend fun update(id: Int, name: String, lat: Double, lon: Double, altitude: Int?)

    @Query("UPDATE LaunchSites SET interpolName = :interpolName WHERE id = :siteId")
    suspend fun insertInterpol(siteId: Int, interpolName: String)

    @Query("UPDATE LaunchSites SET interpolName = NULL WHERE id = :id")
    suspend fun clearInterpolName(id: Int)

    @Delete
    suspend fun delete(location: LaunchSite)

    @Query("DELETE FROM LaunchSites WHERE id = :id")
    suspend fun deleteById(id: Int)
}