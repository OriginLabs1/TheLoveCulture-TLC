package culture.thelove.tlc.android.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The avatar studio: pick, then frame the face before it is saved.
 *
 * v0.0.6 silently centre-cropped a chosen photo, which cut heads off and gave the person no say.
 * Here the photo is shown in a circular frame, the person pans and pinches (or uses the zoom
 * slider), and the exact frame they approve is the frame that is written. The preview and the saved
 * file use the same geometry, so what you see is what you get.
 */
@Composable
public fun AvatarStudio(
    uri: Uri,
    onCancel: () -> Unit,
    onApply: (zoom: Float, offsetX: Float, offsetY: Float) -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }
    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }

    LaunchedEffect(uri) {
        val loaded = withContext(Dispatchers.IO) { loadOriented(context, uri, PREVIEW_MAX) }
        if (loaded == null) failed = true else bitmap = loaded
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Eyebrow(stringResource(R.string.avatar_studio_eyebrow))
                        Text(
                            text = stringResource(R.string.avatar_studio_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(
                        onClick = {
                            zoom = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.avatar_studio_reset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val frame = bitmap
                    when {
                        failed -> Text(
                            text = stringResource(R.string.avatar_studio_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        frame == null -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        else -> AvatarFrame(
                            bitmap = frame,
                            zoom = zoom,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            onTransform = { nextZoom, nextX, nextY ->
                                zoom = nextZoom
                                offsetX = nextX
                                offsetY = nextY
                            },
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.avatar_studio_zoom),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.avatar_studio_zoom_value, (zoom * 100).roundToInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = zoom,
                        onValueChange = { next ->
                            zoom = next.coerceIn(1f, 4f)
                            val frame = bitmap ?: return@Slider
                            val maxX = maxOffsetX(frame, zoom)
                            val maxY = maxOffsetY(frame, zoom)
                            offsetX = offsetX.coerceIn(-maxX, maxX)
                            offsetY = offsetY.coerceIn(-maxY, maxY)
                        },
                        valueRange = 1f..4f,
                    )
                    Text(
                        text = stringResource(R.string.avatar_studio_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { onApply(zoom, offsetX, offsetY) },
                        enabled = bitmap != null,
                        modifier = Modifier.weight(1.6f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.avatar_studio_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarFrame(
    bitmap: Bitmap,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (Float, Float, Float) -> Unit,
) {
    val image: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val ring = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                    val maxX = maxOffsetX(bitmap, nextZoom)
                    val maxY = maxOffsetY(bitmap, nextZoom)
                    // Drag moves the photo with the finger: the crop window moves the other way.
                    val nextX = (offsetX - pan.x / size.width / nextZoom).coerceIn(-maxX, maxX)
                    val nextY = (offsetY - pan.y / size.height / nextZoom).coerceIn(-maxY, maxY)
                    onTransform(nextZoom, nextX, nextY)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(bitmap.width, bitmap.height) / zoom
            val left = ((bitmap.width - side) / 2f + offsetX * bitmap.width)
                .coerceIn(0f, bitmap.width - side)
            val top = ((bitmap.height - side) / 2f + offsetY * bitmap.height)
                .coerceIn(0f, bitmap.height - side)
            drawImage(
                image = image,
                srcOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                srcSize = IntSize(side.roundToInt().coerceAtLeast(1), side.roundToInt().coerceAtLeast(1)),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
            val radius = size.minDimension / 2f - 2.dp.toPx()
            // A soft outer dim makes the circular crop unmistakable without hiding the photo.
            drawCircle(color = Color.Black.copy(alpha = 0.28f), radius = radius + 2.dp.toPx(), style = Stroke(4.dp.toPx()))
            drawCircle(color = ring, radius = radius, style = Stroke(1.5.dp.toPx()))
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius + 3.dp.toPx(),
                    center.y - radius + 3.dp.toPx(),
                ),
                size = androidx.compose.ui.geometry.Size(
                    (radius - 3.dp.toPx()) * 2f,
                    (radius - 3.dp.toPx()) * 2f,
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = stringResource(R.string.avatar_studio_frame),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun maxOffsetX(bitmap: Bitmap, zoom: Float): Float {
    val side = min(bitmap.width, bitmap.height) / zoom
    return ((bitmap.width - side) / 2f) / bitmap.width
}

private fun maxOffsetY(bitmap: Bitmap, zoom: Float): Float {
    val side = min(bitmap.width, bitmap.height) / zoom
    return ((bitmap.height - side) / 2f) / bitmap.height
}

/** Decode upright, at a bounded size, for the preview. Matches the import geometry exactly. */
internal fun loadOriented(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val resolver = context.applicationContext.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return decoded
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

private const val PREVIEW_MAX = 1440
