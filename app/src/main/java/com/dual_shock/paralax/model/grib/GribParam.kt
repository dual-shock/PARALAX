package com.dual_shock.paralax.model.grib

enum class GribParam {
    U_WIND, V_WIND, TEMPERATURE, UNKNOWN
}

// https://codes.ecmwf.int/grib/format/grib1/table2/128/
fun grib1ParamToEnum(paramNumber: Int): GribParam = when (paramNumber) {
    33, 131 -> GribParam.U_WIND       // 33=WMO table 2, 131=ECMWF table 128
    34, 132 -> GribParam.V_WIND       // 34=WMO table 2, 132=ECMWF table 128
    11, 130 -> GribParam.TEMPERATURE  // 11=WMO table 2, 130=ECMWF table 128
    else -> GribParam.UNKNOWN
}

// https://codes.ecmwf.int/grib/format/grib2/parameters/
fun grib2ParamToEnum(category: Int, number: Int): GribParam = when {
    category == 2 && number == 2 -> GribParam.U_WIND      // Eastward wind
    category == 2 && number == 3 -> GribParam.V_WIND      // Northward wind
    category == 0 && number == 0 -> GribParam.TEMPERATURE  // Temperature
    else -> GribParam.UNKNOWN
}
