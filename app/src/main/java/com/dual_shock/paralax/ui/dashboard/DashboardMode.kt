package com.dual_shock.paralax.ui.dashboard

sealed class DashboardMode {
    data object Upcoming : DashboardMode()
    data object Best : DashboardMode()
    data object Watched : DashboardMode()

    companion object {
        val entries = listOf(Upcoming, Best, Watched)

        fun fromPageIndex(index: Int): DashboardMode = when (index) {
            0 -> Upcoming
            1 -> Best
            2 -> Watched
            else -> Upcoming
        }
    }

    val pageIndex: Int get() = when (this) {
        Upcoming -> 0
        Best -> 1
        Watched -> 2
    }
}