package com.aperture.camera.ui.components

import android.util.Range
import android.view.ViewGroup
import androidx.camera.core.MeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aperture.camera.data.model.GridType

@Composable
fun ViewfinderPreview(
    gridType: GridType,
    isDocumentMode: Boolean = false,
    zoomRatio: Float,
    isFocusing: Boolean,
    focusPoint: Pair<Float, Float>?,
    exposureIndex: Int,
    exposureRange: Range<Int>,
    exposureStep: Float,
    onPreviewViewAvailable: (PreviewView) -> Unit,
    onTapToFocus: (Float, Float, MeteringPointFactory) -> Unit,
    onZoomChange: (Float) -> Unit,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var currentZoom by remember(zoomRatio) { mutableStateOf(zoomRatio) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        currentZoom *= zoom
                        onZoomChange(currentZoom)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    activePreviewView?.let { pv ->
                        onTapToFocus(offset.x, offset.y, pv.meteringPointFactory)
                    }
                }
            }
    ) {
        // 1. AndroidView creating PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { pv ->
                    activePreviewView = pv
                    onPreviewViewAvailable(pv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Composition Grid Overlay
        GridOverlay(gridType = gridType)

        // 3. Document Framing Guide (when in DOCS mode)
        if (isDocumentMode) {
            DocumentFramingGuide(modifier = Modifier.fillMaxSize())
        }

        // 4. Animated Focus Ring
        FocusMeteringIndicator(
            focusPoint = focusPoint,
            isFocusing = isFocusing
        )

        // 5. Exposure Compensation Slider
        ExposureSlider(
            visible = focusPoint != null,
            exposureIndex = exposureIndex,
            exposureRange = exposureRange,
            exposureStep = exposureStep,
            onExposureChange = onExposureChange,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
    }
}

@Composable
private fun DocumentFramingGuide(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padX = size.width * 0.08f
            val padY = size.height * 0.16f
            val rectW = size.width - (padX * 2)
            val rectH = size.height - (padY * 2)
            val cornerLen = 40.dp.toPx()
            val strokeW = 3.dp.toPx()
            val color = Color(0xFFFFD600)

            // Top-Left corner
            drawLine(color, Offset(padX, padY), Offset(padX + cornerLen, padY), strokeW)
            drawLine(color, Offset(padX, padY), Offset(padX, padY + cornerLen), strokeW)

            // Top-Right corner
            drawLine(color, Offset(padX + rectW, padY), Offset(padX + rectW - cornerLen, padY), strokeW)
            drawLine(color, Offset(padX + rectW, padY), Offset(padX + rectW, padY + cornerLen), strokeW)

            // Bottom-Left corner
            drawLine(color, Offset(padX, padY + rectH), Offset(padX + cornerLen, padY + rectH), strokeW)
            drawLine(color, Offset(padX, padY + rectH), Offset(padX, padY + rectH - cornerLen), strokeW)

            // Bottom-Right corner
            drawLine(color, Offset(padX + rectW, padY + rectH), Offset(padX + rectW - cornerLen, padY + rectH), strokeW)
            drawLine(color, Offset(padX + rectW, padY + rectH), Offset(padX + rectW, padY + rectH - cornerLen), strokeW)
        }

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Text(
                text = "Align document within yellow frame",
                color = Color(0xFFFFD600),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
