package com.dual_shock.paralax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val onPrimary: Color,
    val onPrimaryMuted: Color,
    val onPrimaryOverlay: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val viabilityGood: Color,
    val viabilityMedium: Color,
    val viabilityBad: Color,
    val warning: Color,
    val expired: Color,
    val rainHighlight: Color,
)

val LightAppColors = AppColorScheme(
    background       = Cream,            // page  #F2EADB
    surface          = CreamSurface,     // cards #FBF5E8  ← now lighter than bg, so cards "pop"
    surfaceVariant   = CreamRecessed,    // wells #E8DCC6  ← darker than surface, for insets
    surfaceAlt       = CreamSurface,
    primary          = Orange,
    onPrimary        = CreamSurface,
    onPrimaryMuted   = WarmMuted,        // warm grey (was neutral)
    onPrimaryOverlay = CreamSurface,
    onBackground     = DarkText,         // warm near-black
    onSurface        = DarkText,
    onSurfaceVariant = WarmMuted,        // warm-grey muted text
    outline          = WarmGrayBorder,
    outlineVariant   = WarmGrayBorderLight,
    error            = ErrorRed,
    viabilityGood    = ViabilityGoodLight,
    viabilityMedium  = ViabilityMediumLight,
    viabilityBad     = ViabilityBadLight,
    warning          = WarningOrange,
    expired          = ExpiredGray,
    rainHighlight    = RainBlueLight,
)

val DarkAppColors = AppColorScheme(
    background       = WarmDarkBg,       // page  #1C1816  ← DARKEST
    surface          = WarmDarkSurface,  // cards #29221F  ← raised, LIGHTER than bg
    surfaceVariant   = WarmDarkRaised,   // wells #362C28  ← most elevated (selected, popovers)
    surfaceAlt       = WarmDarkAlt,
    primary          = Cyan,             // #6FB8B5 — desaturated, still complementary to orange
    onPrimary        = WarmDarkBg,       // dark text on the cyan
    onPrimaryMuted   = WarmGrayText,     // warm muted (was cool LightGrayText)
    onPrimaryOverlay = WarmDarkBg,
    onBackground     = LightText,        // warm cream
    onSurface        = LightText,
    onSurfaceVariant = WarmGrayText,     // warm muted body
    outline          = WarmDarkBorder,   // warm border
    outlineVariant   = WarmDarkBorderSub,
    error            = ErrorRedLight,
    viabilityGood    = ViabilityGoodDark,
    viabilityMedium  = ViabilityMediumDark,
    viabilityBad     = ViabilityBadDark,
    warning          = WarningOrange,
    expired          = ExpiredGray,
    rainHighlight    = RainBlueDark,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

val appColors: AppColorScheme
    @Composable get() = LocalAppColors.current

val isDarkTheme: Boolean
    @Composable get() = LocalIsDarkTheme.current

@Composable
fun viabilityColor(score: Int): Color {
    return if (isDarkTheme) viabilityColorDark(score) else viabilityColorLight(score)
}

private val DarkColorScheme = darkColorScheme(
    primary          = Cyan,
    onPrimary        = WarmDarkBg,
    background       = WarmDarkBg,
    onBackground     = LightText,
    surface          = WarmDarkSurface,
    onSurface        = LightText,
    surfaceVariant   = WarmDarkRaised,
    onSurfaceVariant = WarmGrayText,
    outline          = WarmDarkBorder,
    outlineVariant   = WarmDarkBorderSub,
    error            = ErrorRedLight,
)

private val LightColorScheme = lightColorScheme(
    primary          = Orange,
    onPrimary        = CreamSurface,
    background       = Cream,
    onBackground     = DarkText,
    surface          = CreamSurface,
    onSurface        = DarkText,
    surfaceVariant   = CreamRecessed,
    onSurfaceVariant = WarmMuted,
    outline          = WarmGrayBorder,
    outlineVariant   = WarmGrayBorderLight,
    error            = ErrorRed,
)

@Composable
fun ParalaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColorScheme = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(
        LocalAppColors provides appColorScheme,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
