package com.dual_shock.paralax

import org.junit.Assert.*
import org.junit.Test
import com.dual_shock.paralax.ui.dashboard.DashboardUIState

class DashboardUIStateTest {

    @Test
    fun effectiveHour_normalHour_returnsModulo() {
        val state = DashboardUIState(
            selectedDay = 1,
            selectedBaseHour = 14
        )
        assertEquals(14, state.effectiveHour)
    }

    @Test
    fun effectiveHour_overflowHour_wrapsAt24() {
        val state = DashboardUIState(
            selectedDay = 1,
            selectedBaseHour = 27
        )
        // 27 % 24 = 3
        assertEquals(3, state.effectiveHour)
    }

    @Test
    fun effectiveDay_noOverflow_returnsSameDay() {
        val state = DashboardUIState(
            selectedDay = 2,
            selectedBaseHour = 15
        )
        // 15 / 24 = 0, so effectiveDay = 2 + 0 = 2
        assertEquals(2, state.effectiveDay)
    }

    @Test
    fun effectiveDay_hourOverflow_incrementsDay() {
        val state = DashboardUIState(
            selectedDay = 1,
            selectedBaseHour = 27
        )
        // 27 / 24 = 1, so effectiveDay = 1 + 1 = 2
        assertEquals(2, state.effectiveDay)
    }
}
