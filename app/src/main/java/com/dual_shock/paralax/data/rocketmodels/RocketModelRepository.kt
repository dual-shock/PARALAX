package com.dual_shock.paralax.data.rocketmodels

import kotlinx.coroutines.flow.Flow

class RocketModelRepository(private val datasource: RocketConfigDao) {

    fun getRocketModels(): Flow<List<RocketModel>> = datasource.getAllConfigs()

    suspend fun getRocketModel(name: String): RocketModel = datasource.getConfigByName(name)

    suspend fun insert(rocketModel: RocketModel) = datasource.insertConfig(rocketModel)

    suspend fun delete(rocketModel: RocketModel) = datasource.deleteConfig(rocketModel)

    suspend fun updateModel(oldName: String, newModel: RocketModel) {
        if (oldName != newModel.name) {
            datasource.deleteByName(oldName)
        }
        datasource.insertConfig(newModel)
    }
}