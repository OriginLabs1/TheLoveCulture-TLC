package culture.thelove.tlc.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import java.io.File

/**
 * Motion is a preference, not decoration. Every new animation reads this flag, so turning motion
 * off in Settings removes the entrance, the pulse and the press response together.
 */
public val LocalMotion = staticCompositionLocalOf { true }

/** The app-private media folder, provided once at the root. Screens never build file paths. */
public val LocalMediaDir = staticCompositionLocalOf<File?> { null }

/**
 * A tap with a small, physical response. The scale is subtle (3%) and the indication stays the
 * Material one, so the control still reads as tappable when motion is off or the user prefers
 * reduced motion.
 */
@Composable
public fun Modifier.tlcClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && motion) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * A short, staggered entrance for a list item. The stagger is capped so a long list never feels
 * slow, and the whole effect collapses to "already visible" when motion is off.
 */
@Composable
public fun Reveal(index: Int, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    var shown by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) {
        if (motion) {
            delay(index.coerceIn(0, 8) * 38L)
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 7 },
    ) {
        content()
    }
}
