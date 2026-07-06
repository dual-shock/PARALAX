package com.dual_shock.paralax.ui.theme

import androidx.compose.ui.graphics.Color


val Cream          = Color(0xFFF2EADB)  // background — the page
val CreamSurface   = Color(0xFFFBF5E8)  // surface    — cards lift off the page
val CreamRecessed  = Color(0xFFE8DCC6)  // surfaceVariant — wells, selected rows

val Orange         = Color(0xFFE58E53)
val DarkText       = Color(0xFF2A211A)  // warm near-black
val WarmMuted      = Color(0xFF7A6B5D)  // warm muted text
val WarmGrayBorder        = Color(0xFFC9B8A0)
val WarmGrayBorderLight   = Color(0xFFDFD0B8)


val WarmDarkBg        = Color(0xFF1A1918)  // page (darkest). Was #1C1816 — pulled chroma down.
val WarmDarkSurface   = Color(0xFF23211F)  // cards (raised, lighter than bg). Was #29221F.
val WarmDarkRaised    = Color(0xFF2E2B28)  // most elevated / selected row. Was #362C28.
val WarmDarkAlt       = Color(0xFF1E1C1A)


val Cyan              = Color(0xFF66B4AC)
//val Cyan              = Color(0xFFC695A5)

val LightText         = Color(0xFFE5DDCE)  // warm cream, but a hair less yellow than before
val WarmGrayText      = Color(0xFF9A8F82)  // muted body — warm but desaturated
val WarmDarkBorder    = Color(0xFF423D38)
val WarmDarkBorderSub = Color(0xFF332F2B)

// Light mode viability ramp (earthier, desaturated - terracotta → moss)
fun viabilityColorLight(score: Int): Color = when (score) {
    in 0..10   -> Color(0xFFB73A2C) // deep terracotta
    in 11..20  -> Color(0xFFC85440) // clay red
    in 21..30  -> Color(0xFFD46A3A) // burnt orange
    in 31..40  -> Color(0xFFD68A2E) // amber/ochre
    in 41..50  -> Color(0xFFD6A22A) // golden
    in 51..60  -> Color(0xFFDCBB18) // mustard
    in 61..70  -> Color(0xFFA8B23E) // chartreuse-olive
    in 71..80  -> Color(0xFF8AA84A) // sage olive
    in 81..90  -> Color(0xFF6E9A50) // moss green
    else       -> Color(0xFF4F8C3E) // deep green
}

// Dark mode viability ramp (brighter, warm-leaning)
fun viabilityColorDark(score: Int): Color = when (score) {
    in 0..10   -> Color(0xFFE26B5C) // soft terracotta
    in 11..20  -> Color(0xFFEC8062) // warm coral
    in 21..30  -> Color(0xFFEF965A) // warm orange
    in 31..40  -> Color(0xFFEFAB4A) // warm amber
    in 41..50  -> Color(0xFFE9BC44) // honey
    in 51..60  -> Color(0xFFDDC641) // warm yellow
    in 61..70  -> Color(0xFFC1CC5E) // chartreuse
    in 71..80  -> Color(0xFFA4C26A) // light olive
    in 81..90  -> Color(0xFF86B870) // soft green
    else       -> Color(0xFF6BAB68) // warm green
}

// Three-band viability constants (for simplified UI elements)
val ViabilityGoodLight   = Color(0xFF4F8C3E)
val ViabilityMediumLight = Color(0xFFD6A22A)
val ViabilityBadLight    = Color(0xFFB73A2C)
val ViabilityGoodDark    = Color(0xFF6BAB68)
val ViabilityMediumDark  = Color(0xFFE9BC44)
val ViabilityBadDark     = Color(0xFFE26B5C)

val WarningOrange     = Color(0xFFFF8F00)
val ExpiredGray       = Color(0xFF9E9E9E)
val ErrorRed          = Color(0xFFB3261E)
val ErrorRedLight     = Color(0xFFF2B8B5)

val RainBlueLight     = Color(0xFF4A7C9B)
val RainBlueDark      = Color(0xFF7BABC7)
