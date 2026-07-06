package com.dual_shock.paralax

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteDao
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test



class FakeLaunchSiteDao : LaunchSiteDao {

    private val sites = mutableListOf<LaunchSite>()

    override fun getLaunchSites() =
        flow { emit(sites.toList()

        ) } //return all sites

    override fun getLaunchSite(id: Int) =
        flow { emit(sites.filter { it.id == id }

        ) } //return one site by id

    override suspend fun insert(site: LaunchSite): Long {
        sites.add(site)
        return site.id.toLong()
    }

    override suspend fun update(id: Int, name: String, lat: Double, lon: Double, altitude: Int?) {
        val index = sites.indexOfFirst { it.id == id }
        if (index >= 0) sites[index] = sites[index].copy(name = name, lat = lat, lon = lon, altitude = altitude)
    }

    override suspend fun clearInterpolName(id: Int) {
        val index = sites.indexOfFirst { it.id == id }
        if (index >= 0) sites[index] = sites[index].copy(interpolName = null)
    }

    override suspend fun insertInterpol(siteId: Int, interpolName: String) {
        val index = sites.indexOfFirst { it.id == siteId }
        if (index >= 0) sites[index] = sites[index].copy(interpolName = interpolName)
    }

    override suspend fun delete(site: LaunchSite) {
        sites.remove(site)
    }

    override suspend fun deleteById(id: Int) {
        sites.removeAll { it.id == id }
    }
}


class LaunchSiteRepositoryTest {

    private lateinit var dao: FakeLaunchSiteDao
    private lateinit var repository: LaunchSiteRepository

    @Before
    fun setup() {
        dao = FakeLaunchSiteDao()
        repository = LaunchSiteRepository(dao)
    }


    @Test
    fun empty() = runBlocking{
        val result = repository.getLaunchSites().first()
        assertTrue(result.isEmpty())
    }


    @Test
    fun insertSite() = runBlocking{
        val site = LaunchSite(name = "Utsira", lon = 4.8, lat = 59.3)
        repository.insert(site)
        val result = repository.getLaunchSites().first()
        assertEquals("Utsira", result[0].name)
    }




    @Test
    fun getSite() = runBlocking {
        val site = LaunchSite (id = 1, name = "Utsira", lon= 4.8, lat= 59.3)
        repository.insert(site)
        val result = repository.getLaunchSite(1).first()
        assertEquals("Utsira", result[0].name)
    }



}


