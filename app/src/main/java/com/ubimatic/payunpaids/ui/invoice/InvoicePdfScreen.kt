package com.ubimatic.payunpaids.ui.invoice

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.min

@Composable
fun InvoicePdfScreen(
    invoiceId: Int,
    pdfData: ByteArray,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val pdfFile = remember(invoiceId) {
        File(context.cacheDir, "invoice_$invoiceId.pdf").apply {
            writeBytes(pdfData)
        }
    }

    val pdfRenderer = remember(invoiceId) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(invoiceId) {
        onDispose {
            pdfRenderer?.close()
            pdfFile.delete()
        }
    }

    if (pdfRenderer == null || pdfRenderer.pageCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not render PDF", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val pageCount = pdfRenderer.pageCount
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Target bitmap size based on screen — cap to avoid OOM on scanned PDFs
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val targetWidth = remember(configuration) {
        with(density) { configuration.screenWidthDp.dp.toPx().toInt().coerceIn(600, 2000) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val bitmap = remember(invoiceId, pageIndex, targetWidth) {
                renderPdfPage(pdfRenderer, pageIndex, targetWidth)
            }

            if (bitmap != null) {
                ZoomableImage(
                    bitmap = bitmap,
                    contentDescription = "PDF page ${pageIndex + 1}",
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Could not render page ${pageIndex + 1}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (pageCount > 1) {
            // Page indicator overlay (top-right)
            Text(
                text = "${pagerState.currentPage + 1} / $pageCount \u2195",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                        androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 11.sp,
            )
        }
    }
}

private fun renderPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    targetWidthPx: Int,
): Bitmap? {
    return try {
        val page = renderer.openPage(pageIndex)
        try {
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            // Scale so that the page is rendered at `targetWidthPx` wide
            var width = targetWidthPx
            var height = (targetWidthPx * aspectRatio).toInt()

            // Cap total pixels to avoid OOM (scanned PDFs can be very tall)
            val maxPixels = 4_000_000 // ~16MB with ARGB_8888
            val totalPixels = width.toLong() * height.toLong()
            if (totalPixels > maxPixels) {
                val scale = kotlin.math.sqrt(maxPixels.toDouble() / totalPixels.toDouble())
                width = (width * scale).toInt()
                height = (height * scale).toInt()
            }

            // Try ARGB_8888 first, fall back to RGB_565 on OOM
            val bmp = try {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } catch (e: OutOfMemoryError) {
                try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                } catch (e2: OutOfMemoryError) {
                    val half = min(width, height) / 2
                    Bitmap.createBitmap(
                        (width * 0.5f).toInt().coerceAtLeast(half),
                        (height * 0.5f).toInt().coerceAtLeast(half),
                        Bitmap.Config.RGB_565,
                    )
                }
            }
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        } finally {
            page.close()
        }
    } catch (e: Throwable) {
        android.util.Log.e("InvoicePdfScreen", "Failed to render page $pageIndex: ${e.message}")
        null
    }
}

@Composable
private fun ZoomableImage(
    bitmap: Bitmap,
    contentDescription: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale == 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = newScale
                        // Only consume pan when zoomed in
                        val maxX = (size.width * (newScale - 1)) / 2
                        val maxY = (size.height * (newScale - 1)) / 2
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )
    }
}

