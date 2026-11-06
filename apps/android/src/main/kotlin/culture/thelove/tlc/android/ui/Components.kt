package culture.thelove.tlc.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.LedgerEntry
import java.io.File
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory

// ---------------------------------------------------------------------------------------------
// Shared visual language. Everything here is presentation only: it reads a fact and draws it.
// The surface is cool and paper-like; panels are quiet blue-to-white gradients with a hairline
// border, which gives depth without a blur that would cost frames on a modest phone.
// ---------------------------------------------------------------------------------------------

@Composable
public fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
public fun ScreenHeader(
    eyebrow: String,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow)
            Spacer(modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
public fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    scheme.primaryContainer.copy(alpha = 0.55f),
                    scheme.surface.copy(alpha = 0.98f),
                    scheme.surfaceVariant.copy(alpha = 0.55f),
                ),
            ),
        )
        .border(1.dp, scheme.outlineVariant, shape)
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** The one piece of chrome the whole app repeats: is the record intact? */
@Composable
public fun SealPill(
    healthy: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val container = if (healthy) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (healthy) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(modifier = modifier, shape = RoundedCornerShape(999.dp), color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (healthy) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
public fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(999.dp), color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
public fun KindLabel(kind: EntryKind, modifier: Modifier = Modifier) {
    val (label, container, content) = when (kind) {
        EntryKind.RECOGNITION -> Triple(
            stringResource(R.string.entry_kind_recognition),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        EntryKind.GRANT_PLEDGE,
        EntryKind.GRANT_EVIDENCE,
        EntryKind.MILESTONE_VERIFIED,
        -> Triple(
            kindLabel(kind),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        else -> Triple(
            kindLabel(kind),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Pill(text = label, modifier = modifier, container = container, content = content)
}

private fun kindLabel(kind: EntryKind): String =
    kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

@Composable
public fun RecordHero(
    entryCount: Int,
    headHash: String,
    healthy: Boolean,
    sealLabel: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    GlassPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.today_record))
            Spacer(modifier = Modifier.weight(1f))
            SealPill(healthy = healthy, label = sealLabel)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = entryCount.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onSurface,
            )
            Text(
                text = if (entryCount == 1) {
                    stringResource(R.string.today_one_entry)
                } else {
                    stringResource(R.string.today_entries, entryCount)
                },
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            text = stringResource(R.string.journal_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = headHash.take(20) + "…",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A large tap target with a title, a reason and a chevron. Used for the everyday verbs. */
@Composable
public fun ActionTile(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
            )
        }
    }
}

@Composable
public fun ModuleRow(
    index: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
public fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    GlassPanel(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

/** A single entry in the chain. The vertical rule is the visual argument: each one holds the last. */
@Composable
public fun EntryRow(
    entry: LedgerEntry,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 6.dp, end = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 1.dp, height = 46.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KindLabel(entry.kind)
                Text(
                    text = relativeTime(entry.occurredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "#${entry.seq}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.subject,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hashPrefix(entry.hash),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
public fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
public fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
public fun InfoRow(
    label: String,
    value: String,
    mono: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
public fun VerticalGap(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}

/**
 * The TLC emblem, drawn rather than bundled as a bitmap so it stays crisp at every size.
 *
 * A crowned figure, a split heart and a sprout: the person, the love and the growth. It is
 * deliberately bold so it survives a 24dp avatar and a 512px store listing without a second file.
 */
@Composable
public fun TlcMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val stroke = unit * 0.035f

        drawCircle(
            color = tint.copy(alpha = 0.35f),
            radius = unit / 2f - stroke,
            style = Stroke(width = stroke),
        )

        val crown = Path().apply {
            moveTo(w * 0.36f, h * 0.24f)
            lineTo(w * 0.36f, h * 0.12f)
            lineTo(w * 0.44f, h * 0.18f)
            lineTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.56f, h * 0.18f)
            lineTo(w * 0.64f, h * 0.12f)
            lineTo(w * 0.64f, h * 0.24f)
            close()
        }
        drawPath(crown, color = tint)

        val heart = Path().apply {
            moveTo(w * 0.50f, h * 0.80f)
            cubicTo(w * 0.16f, h * 0.58f, w * 0.16f, h * 0.30f, w * 0.35f, h * 0.30f)
            cubicTo(w * 0.44f, h * 0.30f, w * 0.49f, h * 0.36f, w * 0.50f, h * 0.40f)
            cubicTo(w * 0.51f, h * 0.36f, w * 0.56f, h * 0.30f, w * 0.65f, h * 0.30f)
            cubicTo(w * 0.84f, h * 0.30f, w * 0.84f, h * 0.58f, w * 0.50f, h * 0.80f)
            close()
        }
        drawPath(heart, color = accent)

        val sprout = Path().apply {
            moveTo(w * 0.50f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.52f)
        }
        drawPath(sprout, color = Color.White, style = Stroke(width = unit * 0.035f))
        drawCircle(
            color = Color.White,
            radius = unit * 0.035f,
            center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.49f),
        )
    }
}

/**
 * The person's own face, or a monogram until they choose one.
 *
 * The crossfade means a new photo arrives without a flash of empty space. With no photo the surface
 * shows the person's initials over a quiet blue gradient, so a profile never looks unfinished and
 * never shows a broken-image box. An optional ring is used for the large profile hero.
 */
@Composable
public fun TlcAvatar(
    photoRef: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    initials: String? = null,
    ring: Boolean = false,
    ringColor: Color = MaterialTheme.colorScheme.surface,
) {
    val directory = LocalMediaDir.current
    val file = remember(photoRef, directory) {
        if (directory == null || photoRef == null) null else File(directory, photoRef).takeIf { it.isFile }
    }
    val image = rememberFileImage(file, maxDimension = 512)
    val monogram = remember(initials) {
        initials
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.joinToString("")
            ?.takeIf { it.isNotBlank() }
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (ring) Modifier.shadow(10.dp, CircleShape, clip = false) else Modifier)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            )
            .border(if (ring) 3.dp else 1.dp, if (ring) ringColor else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = image,
            animationSpec = tween(240),
            label = "avatar",
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.profile_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (monogram != null) {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (size.value * 0.34f).sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.profile_photo),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.46f),
                )
            }
        }
    }
}

/** A receipt or other private image, decoded from the app's media folder. */
@Composable
public fun MediaImage(
    file: File?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxDimension: Int = 720,
) {
    val image = rememberFileImage(file, maxDimension)
    if (image == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.receipt_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

/** Decode a private JPEG at roughly the size it will be drawn. Runs once per file change. */
@Composable
public fun rememberFileImage(file: File?, maxDimension: Int): ImageBitmap? = remember(
    file?.path,
    file?.lastModified(),
    maxDimension,
) {
    file?.let { decodeSampled(it, maxDimension) }
}

private fun decodeSampled(file: File, maxDimension: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
}.getOrNull()
