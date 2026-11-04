package culture.thelove.tlc.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// TLC design tokens — royal blue, azure, silver, forest and a restrained royal purple. The record
// is personal, so the surface is quiet paper in light and midnight ink in dark. Blue is the action
// colour; forest marks what is received or verified; purple marks the Pioneer and stewardship.
// Status colours are ink stamps, never surface washes, and never a second call to action. No
// bundled fonts: the system serif for display and the system sans for text keep the download small
// and the licence surface clean.
// ---------------------------------------------------------------------------------------------

private val RoyalBlue = Color(0xFF1B3A8C)
private val RoyalBlueDeep = Color(0xFF0B1B33)
private val Azure = Color(0xFF1466C7)
private val Silver = Color(0xFFD9E2EE)
private val SilverMist = Color(0xFFEDF2F9)
private val Paper = Color(0xFFF6F8FC)
private val PaperRaised = Color(0xFFFFFFFF)
private val CoolSand = Color(0xFFE8EEF7)
private val Ink = Color(0xFF0E1A2B)
private val InkSoft = Color(0xFF46566F)
private val Hairline = Color(0xFFB9C6D8)
private val HairlineSoft = Color(0xFFDDE5EF)

private val DarkBlue = Color(0xFF9CBBFF)
private val DarkBlueContainer = Color(0xFF17376E)
private val DarkAzure = Color(0xFF7FB6FF)
private val DarkAzureContainer = Color(0xFF123A66)
private val DarkSlate = Color(0xFFC3D0E4)
private val Night = Color(0xFF0B1B33)
private val NightRaised = Color(0xFF102340)
private val NightSoft = Color(0xFF172C4B)
private val Moon = Color(0xFFF1F5FB)
private val MoonSoft = Color(0xFFAFC0D8)

// Two new accent families, used like ink stamps — never as a surface wash.
// Forest green: received, verified, growth. Royal purple: Pioneer, stewardship, recognition.
// Every pair below is checked at WCAG 2.2 AA or better (lowest is 7.54:1).
private val Forest = Color(0xFF1E5B3E)
private val ForestContainer = Color(0xFFDCEFE3)
private val OnForestContainer = Color(0xFF0B3524)
private val RoyalPurple = Color(0xFF4B2E83)
private val RoyalPurpleContainer = Color(0xFFE9E1FA)
private val OnRoyalPurpleContainer = Color(0xFF2A1750)

private val DarkForest = Color(0xFF7FD1A8)
private val DarkForestContainer = Color(0xFF123A2A)
private val OnDarkForestContainer = Color(0xFFC9F2DC)
private val DarkRoyalPurple = Color(0xFFC3A6FF)
private val DarkRoyalPurpleContainer = Color(0xFF2E1F52)
private val OnDarkRoyalPurpleContainer = Color(0xFFE9DFFF)

/**
 * Semantic colours that Material 3 does not provide.
 *
 * [forest] is for "received", "verified" and "growth". [royal] is for the Pioneer, stewardship
 * and recognition. They are deliberately separate from [androidx.compose.material3.ColorScheme.primary]
 * so a screen cannot accidentally turn a status into a call to action.
 */
@Immutable
public data class TlcSemanticColors(
    public val forest: Color,
    public val onForest: Color,
    public val forestContainer: Color,
    public val onForestContainer: Color,
    public val royal: Color,
    public val onRoyal: Color,
    public val royalContainer: Color,
    public val onRoyalContainer: Color,
)

private val LightSemanticColors = TlcSemanticColors(
    forest = Forest,
    onForest = Color.White,
    forestContainer = ForestContainer,
    onForestContainer = OnForestContainer,
    royal = RoyalPurple,
    onRoyal = Color.White,
    royalContainer = RoyalPurpleContainer,
    onRoyalContainer = OnRoyalPurpleContainer,
)

private val DarkSemanticColors = TlcSemanticColors(
    forest = DarkForest,
    onForest = Color(0xFF052015),
    forestContainer = DarkForestContainer,
    onForestContainer = OnDarkForestContainer,
    royal = DarkRoyalPurple,
    onRoyal = Color(0xFF1B0F33),
    royalContainer = DarkRoyalPurpleContainer,
    onRoyalContainer = OnDarkRoyalPurpleContainer,
)

public val LocalTlcSemanticColors: androidx.compose.runtime.ProvidableCompositionLocal<TlcSemanticColors> =
    staticCompositionLocalOf { LightSemanticColors }

/** Read the TLC-only accents: `TlcPalette.colors.forest`, `.royal`. */
public object TlcPalette {
    public val colors: TlcSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTlcSemanticColors.current
}

private val LightScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FA),
    onPrimaryContainer = RoyalBlueDeep,
    secondary = Azure,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E7FB),
    onSecondaryContainer = Color(0xFF082B55),
    tertiary = Color(0xFF4E617E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7EDF6),
    onTertiaryContainer = Color(0xFF16233A),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = CoolSand,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkScheme = darkColorScheme(
    primary = DarkBlue,
    onPrimary = Color(0xFF06214D),
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = Color(0xFFDCE7FA),
    secondary = DarkAzure,
    onSecondary = Color(0xFF062B55),
    secondaryContainer = DarkAzureContainer,
    onSecondaryContainer = Color(0xFFD7E7FB),
    tertiary = DarkSlate,
    onTertiary = Color(0xFF1B2A42),
    tertiaryContainer = Color(0xFF263B5C),
    onTertiaryContainer = Color(0xFFE7EDF6),
    background = Night,
    onBackground = Moon,
    surface = NightRaised,
    onSurface = Moon,
    surfaceVariant = NightSoft,
    onSurfaceVariant = MoonSoft,
    outline = Color(0xFF5E7594),
    outlineVariant = Color(0xFF243A5C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val TlcShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
)

private val TlcTypography: Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            lineHeight = 42.sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.6.sp),
    )
}

@Composable
public fun TlcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTlcSemanticColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = TlcTypography,
            shapes = TlcShapes,
            content = content,
        )
    }
}
