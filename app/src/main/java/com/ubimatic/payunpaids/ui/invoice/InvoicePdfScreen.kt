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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.ui.theme.TextSecondary
import java.io.File

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

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val bitmap = remember(invoiceId, pageIndex) {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    val bmp = Bitmap.createBitmap(
                        page.width * 3,
                        page.height * 3,
                        Bitmap.Config.ARGB_8888,
                    )
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                ZoomableImage(
                    bitmap = bitmap,
                    contentDescription = "PDF page ${pageIndex + 1}",
                )
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

