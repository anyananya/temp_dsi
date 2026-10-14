package org.dsiders.nextstep.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Turns an attached file into something the intake parser can read.
 * PDFs and photos are sent as page images (OpenAI reads them directly), which
 * avoids shipping an OCR engine on a low-storage phone. Text files are sent as text.
 */
object ResumeReader {
    private const val MAX_PAGES = 2
    private const val MAX_SIDE = 1400

    fun displayName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Attached file"

    suspend fun read(context: Context, uri: Uri): ResumeInput = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri).orEmpty()
        when {
            type == "application/pdf" -> ResumeInput(imagesBase64Jpeg = pdfPages(context, uri))
            type.startsWith("image/") -> ResumeInput(imagesBase64Jpeg = listOfNotNull(image(context, uri)))
            else -> ResumeInput(
                text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }.orEmpty().take(8000)
            )
        }
    }

    private fun pdfPages(context: Context, uri: Uri): List<String> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        return pfd.use {
            PdfRenderer(it).use { renderer ->
                (0 until minOf(renderer.pageCount, MAX_PAGES)).map { index ->
                    renderer.openPage(index).use { page ->
                        val scale = MAX_SIDE.toFloat() / maxOf(page.width, page.height)
                        val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp.toBase64Jpeg().also { bmp.recycle() }
                    }
                }
            }
        }
    }

    private fun image(context: Context, uri: Uri): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bmp.width, bmp.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        return scaled.toBase64Jpeg().also { if (scaled !== bmp) scaled.recycle(); bmp.recycle() }
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
