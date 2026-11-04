package culture.thelove.tlc.android.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * The private media folder.
 *
 * A profile photo and a receipt image are copied into app-private storage, rotated upright from
 * their EXIF orientation, downscaled, re-encoded as JPEG — which drops every EXIF tag — and hashed.
 * Only the file name and the hash enter durable state; the image bytes never enter the state file,
 * the hash-chained record, a backup bundle, a log or an analytics event.
 *
 * The folder is deliberately not shared storage and the manifest asks for no storage permission:
 * the system photo picker hands us one readable stream, and nothing else on the device can browse
 * what we keep.
 */
public class MediaFiles(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val root: File = File(context.applicationContext.filesDir, FOLDER)

    public fun file(name: String?): File? {
        if (name == null || !NAME.matches(name)) return null
        val candidate = File(root, name)
        return candidate.takeIf { it.isFile }
    }

    public suspend fun importAvatar(uri: Uri): ImportedMedia? = withContext(Dispatchers.IO) {
        import(uri = uri, prefix = "avatar", maxDimension = 720, square = true)
    }

    public suspend fun importReceipt(uri: Uri): ImportedMedia? = withContext(Dispatchers.IO) {
        import(uri = uri, prefix = "receipt", maxDimension = 1440, square = false)
    }

    public suspend fun delete(name: String?) {
        if (name == null || !NAME.matches(name)) return
        withContext(Dispatchers.IO) { File(root, name).delete() }
    }

    private fun import(uri: Uri, prefix: String, maxDimension: Int, square: Boolean): ImportedMedia? {
        val decoded = decode(uri, maxDimension) ?: return null
        val upright = orient(decoded, orientation(uri))
        val scaled = scale(upright, maxDimension)
        val squared = if (square) centerSquare(scaled) else scaled
        val name = "$prefix-${UUID.randomUUID().toString().replace("-", "").take(20)}.jpg"
        if (!root.exists()) root.mkdirs()
        val target = File(root, name)
        val written = runCatching {
            FileOutputStream(target).use { out ->
                if (!squared.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)) error("encode failed")
                out.flush()
                out.fd.sync()
            }
        }.isSuccess
        if (!written) {
            target.delete()
            return null
        }
        val hash = runCatching { sha256(target.readBytes()) }.getOrNull()
        if (hash == null) {
            target.delete()
            return null
        }
        return ImportedMedia(fileName = name, hash = hash)
    }

    private fun decode(uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun orientation(uri: Uri): Int = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun centerSquare(bitmap: Bitmap): Bitmap {
        val side = minOf(bitmap.width, bitmap.height)
        if (side == bitmap.width && side == bitmap.height) return bitmap
        val x = (bitmap.width - side) / 2
        val y = (bitmap.height - side) / 2
        return Bitmap.createBitmap(bitmap, x, y, side, side)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

    public data class ImportedMedia(
        public val fileName: String,
        public val hash: String,
    )

    public companion object {
        public const val FOLDER: String = "media"
        private const val QUALITY: Int = 88
        private val NAME: Regex = Regex("[a-z0-9-]{1,80}\\.jpg")
    }
}
