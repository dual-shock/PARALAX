package com.dual_shock.paralax.data.launchsites

import kotlinx.coroutines.flow.Flow

class LaunchSiteRepository(private val datasource: LaunchSiteDao){
    fun getLaunchSites(): Flow<List<LaunchSite>> = datasource.getLaunchSites()

    fun getLaunchSite(id: Int): Flow<List<LaunchSite>> = datasource.getLaunchSite(id)

    suspend fun insert(site: LaunchSite): Long {
        return datasource.insert(site)
    }
    suspend fun insertInterpol(site: Int, interpol: String){datasource.insertInterpol(site, interpol)}

    suspend fun clearInterpolName(id: Int) { datasource.clearInterpolName(id) }

    suspend fun update(id: Int, name: String, lat: Double, lon: Double, altitude: Int?) { datasource.update(id, name, lat, lon, altitude) }

    suspend fun deleteById(id: Int) { datasource.deleteById(id) }
}