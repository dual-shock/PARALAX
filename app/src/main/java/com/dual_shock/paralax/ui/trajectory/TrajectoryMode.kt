package com.dual_shock.paralax.ui.trajectory

sealed class TrajectoryMode {
    data object Grib : TrajectoryMode()
    data object Interpol : TrajectoryMode()
    data object Average : TrajectoryMode()
}
