package com.dual_shock.paralax.data.rocketmodels

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "rocket_models")
data class RocketModel(
    @PrimaryKey
    val name: String,
    val dryMass: Double,
    val fuelMass: Double,
    val thrust: Double,
    val burnTime: Double ,
    val dragCoefficient: Double,
    val diameter: Double,
    val launchAngle: Double,
    val direction: Double,
    val firstChuteReleaseHeight: Int = 0,
    val firstChuteTerminalVelocity: Double = 20.0,
    val firstChuteAtApogee: Boolean = true,
    val secondChuteReleaseHeight: Int = 400,
    val secondChuteTerminalVelocity: Double = 5.0,
)

@Dao
interface RocketConfigDao {
    @Query("SELECT * FROM rocket_models")
    fun getAllConfigs(): Flow<List<RocketModel>>

    @Query("SELECT * FROM rocket_models WHERE name = :name")
    fun getConfigByName(name: String): RocketModel
    @Delete
    suspend fun deleteConfig(config: RocketModel) // still works, Room uses the PK

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: RocketModel)

    @Query("DELETE FROM rocket_models WHERE name = :oldName")
    suspend fun deleteByName(oldName: String)
}