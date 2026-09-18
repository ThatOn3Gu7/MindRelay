package com.mindrelay.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.mindrelay.R

/**
 * Roboto Flex — bundled as a resource font so the whole app (including the
 * emphatic M3 Expressive type scale) uses the variable family offline.
 */
private val RobotoFlex = FontFamily(
    Font(R.font.robotoflex, FontWeight.Light, FontStyle.Normal),
    Font(R.font.robotoflex, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.robotoflex, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.robotoflex, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.robotoflex, FontWeight.Bold, FontStyle.Normal),
)

// Light scheme — the exact teal roles from the spec.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F6),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4D6263),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E9),
    onSecondaryContainer = Color(0xFF051F20),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF001C3B),
    surface = Color(0xFFF4FBFB),
    surfaceContainerLow = Color(0xFFEEF5F5),
    surfaceContainer = Color(0xFFE8EFEF),
    surfaceContainerHigh = Color(0xFFE2EAEA),
    surfaceContainerHighest = Color(0xFFDDE4E4),
    onSurface = Color(0xFF161D1D),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC8C8),
    inverseSurface = Color(0xFF2B3232),
    inverseOnSurface = Color(0xFFECF2F2),
    inversePrimary = Color(0xFF80D5DA),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

// Dark scheme — the exact teal roles from the spec.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF83D4D8),
    onPrimary = Color(0xFF063639),
    primaryContainer = Color(0xFF0B4F52),
    onPrimaryContainer = Color(0xFF9FF0F5),
    secondary = Color(0xFFB3CBCC),
    onSecondary = Color(0xFF1D3435),
    secondaryContainer = Color(0xFF354A4C),
    onSecondaryContainer = Color(0xFFCFE7E8),
    tertiaryContainer = Color(0xFF38485A),
    onTertiaryContainer = Color(0xFFD3E4FA),
    surface = Color(0xFF0D1515),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF2F3636),
    onSurface = Color(0xFFDCE4E4),
    onSurfaceVariant = Color(0xFFB7CACB),
    outline = Color(0xFF819495),
    outlineVariant = Color(0xFF394A4B),
    inverseSurface = Color(0xFFDCE4E4),
    inverseOnSurface = Color(0xFF2B3232),
    inversePrimary = Color(0xFF00696E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * M3 Expressive shape defaults tuned to the guidance: pill buttons (extraLarge),
 * 20dp cards (large), 28dp dialogs.
 */
private val MindRelayShapes = Shapes(
    extraSmall = RoundedCornerShape(8),
    small = RoundedCornerShape(12),
    medium = RoundedCornerShape(16),
    large = RoundedCornerShape(20),
    extraLarge = RoundedCornerShape(28),
)

/**
 * Type scale on Roboto Flex. The emphasized styles (headlineMediumEmphasized,
 * titleLargeEmphasized, labelLargeEmphasized…) carry the heavier grades used for
 * headlines, button labels and tabs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun mindRelayTypography(): Typography {
    val base = Typography()
    fun reg(style: TextStyle): TextStyle = style.copy(fontFamily = RobotoFlex)
    fun emph(style: TextStyle, weight: FontWeight): TextStyle =
        style.copy(fontFamily = RobotoFlex, fontWeight = weight)
    return Typography(
        displayLarge = reg(base.displayLarge),
        displayMedium = reg(base.displayMedium),
        displaySmall = reg(base.displaySmall),
        headlineLarge = reg(base.headlineLarge),
        headlineMedium = reg(base.headlineMedium),
        headlineSmall = reg(base.headlineSmall),
        titleLarge = reg(base.titleLarge),
        titleMedium = reg(base.titleMedium),
        titleSmall = reg(base.titleSmall),
        bodyLarge = reg(base.bodyLarge),
        bodyMedium = reg(base.bodyMedium),
        bodySmall = reg(base.bodySmall),
        labelLarge = reg(base.labelLarge),
        labelMedium = reg(base.labelMedium),
        labelSmall = reg(base.labelSmall),
        displayLargeEmphasized = emph(base.displayLargeEmphasized, FontWeight.Bold),
        displayMediumEmphasized = emph(base.displayMediumEmphasized, FontWeight.Bold),
        displaySmallEmphasized = emph(base.displaySmallEmphasized, FontWeight.Bold),
        headlineLargeEmphasized = emph(base.headlineLargeEmphasized, FontWeight.Bold),
        headlineMediumEmphasized = emph(base.headlineMediumEmphasized, FontWeight.Bold),
        headlineSmallEmphasized = emph(base.headlineSmallEmphasized, FontWeight.Bold),
        titleLargeEmphasized = emph(base.titleLargeEmphasized, FontWeight.SemiBold),
        titleMediumEmphasized = emph(base.titleMediumEmphasized, FontWeight.SemiBold),
        titleSmallEmphasized = emph(base.titleSmallEmphasized, FontWeight.SemiBold),
        bodyLargeEmphasized = emph(base.bodyLargeEmphasized, FontWeight.SemiBold),
        bodyMediumEmphasized = emph(base.bodyMediumEmphasized, FontWeight.SemiBold),
        bodySmallEmphasized = emph(base.bodySmallEmphasized, FontWeight.SemiBold),
        labelLargeEmphasized = emph(base.labelLargeEmphasized, FontWeight.SemiBold),
        labelMediumEmphasized = emph(base.labelMediumEmphasized, FontWeight.SemiBold),
        labelSmallEmphasized = emph(base.labelSmallEmphasized, FontWeight.SemiBold),
    )
}

var LightColorScheme: ColorScheme = LightColors
    private set
var DarkColorScheme: ColorScheme = DarkColors
    private set

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MindRelayShapes,
        typography = mindRelayTypography(),
        content = content,
    )
}

internal const val THEME_FILE_MARKER = "mindrelay-teal"
