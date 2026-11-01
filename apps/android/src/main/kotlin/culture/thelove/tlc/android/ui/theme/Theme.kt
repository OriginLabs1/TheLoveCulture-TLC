package culture.thelove.tlc.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TLC design tokens (design/tokens is the source of truth once the brand pass lands).
private val Terracotta = Color(0xFF7A2E3A)
private val TerracottaDeep = Color(0xFF4A1A22)
private val Sand = Color(0xFFFBF6F0)
private val Ink = Color(0xFF1E1A18)
private val Sage = Color(0xFF3F5C4C)

private val TlcColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4D9DC),
    onPrimaryContainer = TerracottaDeep,
    secondary = Sage,
    onSecondary = Color.White,
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0E9E2),
    onSurfaceVariant = Color(0xFF544742),
    outline = Color(0xFFB9A9A2),
)

@Composable
fun TlcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TlcColors,
        typography = Typography(),
        content = content,
    )
}
