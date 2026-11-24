package com.example.ui.designer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.RoseAlert
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

enum class CoverAspectRatio(
    val label: String,
    val ratio: Float,
    val exportWidth: Int,
    val exportHeight: Int,
) {
    BANNER_16_9("16:9 Banner", 16f / 9f, 1920, 1080),
    POSTER_2_3("2:3 Poster", 2f / 3f, 1200, 1800),
    SQUARE_1_1("1:1 Tile", 1f, 1440, 1440),
}

enum class CoverLayoutStyle(val label: String) {
    EDITORIAL_BOTTOM("Editorial Bottom"),
    CENTERED_CREST("Centered Crest"),
    MINIMAL_CORNER("Minimal Corner"),
    CINEMA_STRIP("Cinema Strip"),
}

enum class BackdropSourceMode(val label: String) {
    STUDIO_ART("Studio Art"),
    NEON_MESH("Neon Mesh"),
    DEVICE_PHOTO("My Photo"),
    GEMINI_AI("Gemini AI"),
}

data class StudioBackdropSpec(
    val id: String,
    val name: String,
    @DrawableRes val drawableRes: Int,
)

data class NeonGradientTheme(
    val id: String,
    val name: String,
    val colors: List<Color>,
)

data class CollectionArtworkSpec(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val aspectRatio: CoverAspectRatio,
    val layoutStyle: CoverLayoutStyle,
    val accentColor: Color,
    val backdropMode: BackdropSourceMode,
    @DrawableRes val studioDrawableRes: Int,
    val neonTheme: NeonGradientTheme,
    val scrimStrength: Float,
    val showFrameGlow: Boolean,
    val showCinemaGrid: Boolean,
    val importedPhotoBitmap: Bitmap? = null,
    val aiGeneratedBitmap: Bitmap? = null,
)

object CollectionDesignerPresets {
    val studioBackdrops: List<StudioBackdropSpec> = listOf(
        StudioBackdropSpec("scifi", "Deep Space", R.drawable.img_col_scifi),
        StudioBackdropSpec("cinema", "Obsidian Theater", R.drawable.img_col_cinema),
        StudioBackdropSpec("anime", "Neon Skyline", R.drawable.img_col_anime),
        StudioBackdropSpec("constellation", "Catalog Stars", R.drawable.img_catalog_constellation),
        StudioBackdropSpec("hero", "Home Cinema", R.drawable.img_hero_setup),
    )

    val neonThemes: List<NeonGradientTheme> = listOf(
        NeonGradientTheme(
            id = "cyber_obsidian",
            name = "Cyber Obsidian",
            colors = listOf(Color(0xFF07131E), Color(0xFF0E2A3D), Color(0xFF1A1438)),
        ),
        NeonGradientTheme(
            id = "amber_cinema",
            name = "Amber Cinema",
            colors = listOf(Color(0xFF1A1008), Color(0xFF3A210B), Color(0xFF0B0E14)),
        ),
        NeonGradientTheme(
            id = "indigo_nebula",
            name = "Indigo Nebula",
            colors = listOf(Color(0xFF0D0E26), Color(0xFF281D59), Color(0xFF081421)),
        ),
        NeonGradientTheme(
            id = "emerald_matrix",
            name = "Emerald Vault",
            colors = listOf(Color(0xFF071A14), Color(0xFF0D382B), Color(0xFF081018)),
        ),
        NeonGradientTheme(
            id = "crimson_noir",
            name = "Crimson Noir",
            colors = listOf(Color(0xFF1E0811), Color(0xFF421022), Color(0xFF0A0D14)),
        ),
    )

    val accentSwatches: List<Pair<String, Color>> = listOf(
        "Cyan" to CyanSignal,
        "Amber" to AmberDebrid,
        "Indigo" to IndigoMeta,
        "Emerald" to EmeraldVerified,
        "Rose" to RoseAlert,
        "Gold" to Color(0xFFFDE047),
    )
}

object CollectionArtworkRenderer {

    /**
     * Safely decodes a user-selected image URI from the zero-permission Android Photo Picker
     * downsampled to fit cleanly within memory limits.
     */
    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDim || (boundsOpts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders the designed collection cover/banner into a full-resolution ARGB_8888 Bitmap.
     */
    fun renderHighResBitmap(context: Context, spec: CollectionArtworkSpec): Bitmap {
        val w = spec.aspectRatio.exportWidth
        val h = spec.aspectRatio.exportHeight
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Backdrop Layer
        val sourceBitmap: Bitmap? = when (spec.backdropMode) {
            BackdropSourceMode.STUDIO_ART -> BitmapFactory.decodeResource(context.resources, spec.studioDrawableRes)
            BackdropSourceMode.DEVICE_PHOTO -> spec.importedPhotoBitmap
            BackdropSourceMode.GEMINI_AI -> spec.aiGeneratedBitmap
            BackdropSourceMode.NEON_MESH -> null
        }

        if (sourceBitmap != null) {
            drawBitmapCenterCrop(canvas, sourceBitmap, w, h)
        } else {
            val themeColors = spec.neonTheme.colors.map { it.toArgb() }.toIntArray()
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), themeColors, null, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

            // Subtle accent atmospheric orb
            val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = applyAlpha(spec.accentColor.toArgb(), 0.22f)
            }
            canvas.drawCircle(w * 0.78f, h * 0.24f, min(w, h) * 0.38f, orbPaint)
        }

        // 2. Cinema Grid Overlay (optional)
        if (spec.showCinemaGrid) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(26, 255, 255, 255)
                strokeWidth = 1.5f
            }
            val step = (min(w, h) / 14f).coerceAtLeast(48f)
            var x = step
            while (x < w) {
                canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
                x += step
            }
            var y = step
            while (y < h) {
                canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
                y += step
            }
        }

        // 3. Gradient Scrim / Vignette
        val scrimAlphaTop = (spec.scrimStrength * 0.35f).coerceIn(0f, 0.85f)
        val scrimAlphaMid = (spec.scrimStrength * 0.52f).coerceIn(0f, 0.90f)
        val scrimAlphaBottom = (spec.scrimStrength * 0.94f).coerceIn(0.2f, 0.98f)
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                h.toFloat(),
                intArrayOf(
                    applyAlpha(0xFF070A10.toInt(), scrimAlphaTop),
                    applyAlpha(0xFF070A10.toInt(), scrimAlphaMid),
                    applyAlpha(0xFF070A10.toInt(), scrimAlphaBottom),
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scrimPaint)

        // 4. Optional Accent Frame Glow
        val pad = min(w, h) * 0.055f
        val accentArgb = spec.accentColor.toArgb()
        if (spec.showFrameGlow) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = min(w, h) * 0.012f
                color = applyAlpha(accentArgb, 0.25f)
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = min(w, h) * 0.0045f
                color = applyAlpha(accentArgb, 0.85f)
            }
            val rect = RectF(pad * 0.65f, pad * 0.65f, w - pad * 0.65f, h - pad * 0.65f)
            val corner = min(w, h) * 0.035f
            canvas.drawRoundRect(rect, corner, corner, glowPaint)
            canvas.drawRoundRect(rect, corner, corner, borderPaint)
        }

        // 5. Typography & Badge Composition
        val titleText = spec.title.trim().ifBlank { "UNTITLED COLLECTION" }
        val subtitleText = spec.subtitle.trim()
        val badgeText = spec.badgeText.trim().uppercase()

        val titleSize = min(w, h) * when (spec.aspectRatio) {
            CoverAspectRatio.BANNER_16_9 -> 0.088f
            CoverAspectRatio.POSTER_2_3 -> 0.095f
            CoverAspectRatio.SQUARE_1_1 -> 0.084f
        }
        val subtitleSize = titleSize * 0.38f
        val badgeSize = titleSize * 0.26f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = titleSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(18f, 0f, 6f, AndroidColor.argb(180, 0, 0, 0))
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(225, 224, 232, 245)
            textSize = subtitleSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = badgeSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
        }

        val maxTextWidth = w - pad * 2.4f
        val titleLines = wrapText(titleText, titlePaint, maxTextWidth, maxLines = 2)
        val subtitleLines = if (subtitleText.isNotBlank()) {
            wrapText(subtitleText, subtitlePaint, maxTextWidth, maxLines = 2)
        } else emptyList()

        val lineHeightTitle = titleSize * 1.15f
        val lineHeightSub = subtitleSize * 1.35f

        when (spec.layoutStyle) {
            CoverLayoutStyle.CENTERED_CREST -> {
                titlePaint.textAlign = Paint.Align.CENTER
                subtitlePaint.textAlign = Paint.Align.CENTER
                val centerX = w / 2f
                val totalBlockHeight = (if (badgeText.isNotBlank()) badgeSize * 2.4f else 0f) +
                    (titleLines.size * lineHeightTitle) +
                    (subtitleLines.size * lineHeightSub) + 32f
                var cursorY = (h - totalBlockHeight) / 2f + badgeSize

                if (badgeText.isNotBlank()) {
                    drawBadgePill(canvas, badgeText, centerX, cursorY, badgeTextPaint, accentArgb, centered = true)
                    cursorY += badgeSize * 2.3f
                }

                // Accent crest divider
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
                canvas.drawRoundRect(
                    RectF(centerX - 54f, cursorY - badgeSize * 0.6f, centerX + 54f, cursorY - badgeSize * 0.6f + 7f),
                    4f,
                    4f,
                    barPaint,
                )
                cursorY += titleSize * 0.92f

                titleLines.forEach { line ->
                    canvas.drawText(line, centerX, cursorY, titlePaint)
                    cursorY += lineHeightTitle
                }
                cursorY += 8f
                subtitleLines.forEach { line ->
                    canvas.drawText(line, centerX, cursorY, subtitlePaint)
                    cursorY += lineHeightSub
                }
            }

            CoverLayoutStyle.MINIMAL_CORNER -> {
                titlePaint.textAlign = Paint.Align.LEFT
                subtitlePaint.textAlign = Paint.Align.LEFT
                val startX = pad * 1.15f
                if (badgeText.isNotBlank()) {
                    drawBadgePill(canvas, badgeText, startX, pad * 1.35f, badgeTextPaint, accentArgb, centered = false)
                }
                var cursorY = h - pad * 1.1f - (subtitleLines.size * lineHeightSub)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
                val firstTitleY = cursorY - ((titleLines.size - 1) * lineHeightTitle)
                canvas.drawRoundRect(
                    RectF(startX, firstTitleY - titleSize * 1.1f, startX + 96f, firstTitleY - titleSize * 1.1f + 8f),
                    4f,
                    4f,
                    barPaint,
                )
                var ty = firstTitleY
                titleLines.forEach { line ->
                    canvas.drawText(line, startX, ty, titlePaint)
                    ty += lineHeightTitle
                }
                var sy = ty - lineHeightTitle + subtitleSize * 1.35f
                subtitleLines.forEach { line ->
                    canvas.drawText(line, startX, sy, subtitlePaint)
                    sy += lineHeightSub
                }
            }

            CoverLayoutStyle.CINEMA_STRIP -> {
                titlePaint.textAlign = Paint.Align.LEFT
                subtitlePaint.textAlign = Paint.Align.LEFT
                val stripHeight = (titleLines.size * lineHeightTitle) +
                    (subtitleLines.size * lineHeightSub) +
                    (if (badgeText.isNotBlank()) badgeSize * 2.5f else 0f) + pad * 1.1f
                val stripTop = h - stripHeight - pad * 0.85f
                val stripRect = RectF(pad * 0.9f, stripTop, w - pad * 0.9f, h - pad * 0.85f)
                val stripBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(205, 10, 15, 24)
                }
                val stripStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = applyAlpha(accentArgb, 0.65f)
                }
                canvas.drawRoundRect(stripRect, 28f, 28f, stripBg)
                canvas.drawRoundRect(stripRect, 28f, 28f, stripStroke)

                // Accent left rail inside strip
                val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
                canvas.drawRoundRect(
                    RectF(stripRect.left + 20f, stripRect.top + 24f, stripRect.left + 32f, stripRect.bottom - 24f),
                    6f,
                    6f,
                    railPaint,
                )

                val textX = stripRect.left + 58f
                var cursorY = stripRect.top + pad * 0.55f + badgeSize
                if (badgeText.isNotBlank()) {
                    drawBadgePill(canvas, badgeText, textX, cursorY, badgeTextPaint, accentArgb, centered = false)
                    cursorY += badgeSize * 2.1f
                }
                cursorY += titleSize * 0.65f
                titleLines.forEach { line ->
                    canvas.drawText(line, textX, cursorY, titlePaint)
                    cursorY += lineHeightTitle
                }
                subtitleLines.forEach { line ->
                    canvas.drawText(line, textX, cursorY, subtitlePaint)
                    cursorY += lineHeightSub
                }
            }

            CoverLayoutStyle.EDITORIAL_BOTTOM -> {
                titlePaint.textAlign = Paint.Align.LEFT
                subtitlePaint.textAlign = Paint.Align.LEFT
                val startX = pad * 1.15f
                val subHeight = subtitleLines.size * lineHeightSub
                val titleHeight = titleLines.size * lineHeightTitle
                val baseBottom = h - pad * 1.15f
                var cursorY = baseBottom - subHeight - titleHeight

                if (badgeText.isNotBlank()) {
                    drawBadgePill(canvas, badgeText, startX, cursorY - badgeSize * 1.15f, badgeTextPaint, accentArgb, centered = false)
                }
                var ty = cursorY + titleSize * 0.82f
                titleLines.forEach { line ->
                    canvas.drawText(line, startX, ty, titlePaint)
                    ty += lineHeightTitle
                }
                var sy = ty - lineHeightTitle + subtitleSize * 1.45f
                subtitleLines.forEach { line ->
                    canvas.drawText(line, startX, sy, subtitlePaint)
                    sy += lineHeightSub
                }
            }
        }

        return bitmap
    }

    /**
     * Publishes the rendered PNG into the user's Android Photo Gallery (Pictures/NuvioCollections)
     * using zero-permission scoped MediaStore on Android 10+, with app-scoped fallback on older versions.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Result<String> {
        val safeBase = title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "nuvio_collection" }
            .take(32)
        val fileName = "${safeBase}_${System.currentTimeMillis()}.png"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NuvioCollections")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Could not create MediaStore entry."))
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        return Result.failure(IllegalStateException("Failed to encode PNG."))
                    }
                } ?: return Result.failure(IllegalStateException("Could not open gallery output stream."))
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("Saved to Gallery -> Pictures/NuvioCollections/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NuvioCollections")
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Saved PNG to ${outFile.name}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the rendered PNG artwork via FileProvider and standard Android Share Sheet.
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, title: String): Result<Unit> {
        return try {
            val cacheFolder = File(context.cacheDir, "shared_artwork")
            if (!cacheFolder.exists()) cacheFolder.mkdirs()
            val safeBase = title.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "nuvio_collection" }
                .take(32)
            val file = File(cacheFolder, "${safeBase}_cover.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Nuvio Collection Cover — $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Collection Picture"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, source: Bitmap, targetW: Int, targetH: Int) {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val dstRatio = targetW.toFloat() / targetH.toFloat()
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (source.height * dstRatio).toInt()
            val left = (source.width - cropW) / 2
            Rect(left, 0, left + cropW, source.height)
        } else {
            val cropH = (source.width / dstRatio).toInt()
            val top = (source.height - cropH) / 2
            Rect(0, top, source.width, top + cropH)
        }
        canvas.drawBitmap(source, srcRect, Rect(0, 0, targetW, targetH), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawBadgePill(
        canvas: Canvas,
        text: String,
        anchorX: Float,
        baselineY: Float,
        textPaint: Paint,
        accentArgb: Int,
        centered: Boolean,
    ) {
        val textWidth = textPaint.measureText(text)
        val padH = textPaint.textSize * 0.9f
        val padV = textPaint.textSize * 0.45f
        val left = if (centered) anchorX - textWidth / 2f - padH else anchorX
        val right = left + textWidth + padH * 2f
        val top = baselineY - textPaint.textSize - padV * 0.6f
        val bottom = baselineY + padV * 0.9f
        val rect = RectF(left, top, right, bottom)
        val radius = (bottom - top) / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = applyAlpha(accentArgb, 0.22f)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = applyAlpha(accentArgb, 0.72f)
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, strokePaint)

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, left + padH, baselineY, textPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                if (lines.size >= maxLines) return lines
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) {
            lines.add(current.toString())
        }
        return lines
    }

    private fun applyAlpha(argb: Int, alphaFraction: Float): Int {
        val a = (255f * max(0f, min(1f, alphaFraction))).toInt()
        return AndroidColor.argb(a, AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))
    }
}
