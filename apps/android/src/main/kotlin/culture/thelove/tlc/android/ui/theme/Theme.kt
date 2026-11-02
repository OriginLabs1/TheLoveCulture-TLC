package culture.thelove.tlc.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// TLC design tokens — warm, quiet, editorial. The record is personal, so the surface is paper-like
// in light and ink-like in dark; the accent is the colour of a wax seal, not of a fintech app.
// No bundled fonts: the system serif for display and the system sans for text keep the download
// small, the rendering native, and the licence surface clean.
// ---------------------------------------------------------------------------------------------

private val Rosewood = Color(0xFF8A2F45)
private val RosewoodDeep = Color(0xFF3B0A17)
private val RosewoodMist = Color(0xFFF7DEE2)
private val Sage = Color(0xFF3F5C4C)
private val SageMist = Color(0xFFDCEBE1)
private val Amber = Color(0xFF9A6B2F)
private val AmberMist = Color(0xFFF6E3C2)
private val Paper = Color(0xFFFBF7F2)
private val PaperRaised = Color(0xFFFFFCF8)
private val Sand = Color(0xFFF1E7DE)
private val Ink = Color(0xFF1E1A18)
private val InkSoft = Color(0xFF5A4A44)
private val Hairline = Color(0xFFC6B4AA)
private val HairlineSoft = Color(0xFFE4D6CC)

private val DarkRose = Color(0xFFF0A8B6)
private val DarkRoseContainer = Color(0xFF6E2233)
private val DarkSage = Color(0xFFA9C9B5)
private val DarkSageContainer = Color(0xFF2C4636)
private val DarkAmber = Color(0xFFE5C078)
private val Night = Color(0xFF14100F)
private val NightRaised = Color(0xFF1C1615)
private val NightSoft = Color(0xFF2A2220)
private val Moon = Color(0xFFF2E7E2)
private val MoonSoft = Color(0xFFCBB8AF)

private val LightScheme = lightColorScheme(
    primary = Rosewood,
    onPrimary = Color.White,
    primaryContainer = RosewoodMist,
    onPrimaryContainer = RosewoodDeep,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = SageMist,
    onSecondaryContainer = Color(0xFF15271D),
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberMist,
    onTertiaryContainer = Color(0xFF3A2508),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkScheme = darkColorScheme(
    primary = DarkRose,
    onPrimary = Color(0xFF4A0E1D),
    primaryContainer = DarkRoseContainer,
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = DarkSage,
    onSecondary = Color(0xFF163023),
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = Color(0xFFD6EBDD),
    tertiary = DarkAmber,
    onTertiary = Color(0xFF3A2508),
    tertiaryContainer = Color(0xFF5A3E14),
    onTertiaryContainer = Color(0xFFFFEBC7),
    background = Night,
    onBackground = Moon,
    surface = NightRaised,
    onSurface = Moon,
    surfaceVariant = NightSoft,
    onSurfaceVariant = MoonSoft,
    outline = Color(0xFF7A6660),
    outlineVariant = Color(0xFF3D322F),
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = TlcTypography,
        shapes = TlcShapes,
        content = content,
    )
}
