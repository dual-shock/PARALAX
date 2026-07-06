package com.dual_shock.paralax.data.frost

import com.dual_shock.paralax.model.frost.Lightning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FrostRepository {
    private val dataSource = FrostDataSource()



    suspend fun getRecentLightningStrikes(lat: Double, lon: Double, hoursBack: Int = 2): List<Lightning> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val now = Date()
        val from = Date(now.time - hoursBack * 3600L * 1000)
        val reftime = "${sdf.format(from)}/${sdf.format(now)}"
        return dataSource.fetchLightning(reftime, lat, lon)
    }

}
