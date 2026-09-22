package com.aperture.camera.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.aperture.camera.data.model.GridType

@Composable
fun GridOverlay(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    val gridColor = Color.White.copy(alpha = 0.35f)
    val strokeWidth = 1.5f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (gridType) {
            GridType.NONE -> {}
            GridType.RULE_OF_THIRDS -> {
                // Vertical lines at 1/3 and 2/3
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)

                // Horizontal lines at 1/3 and 2/3
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
            }
            GridType.GOLDEN_RATIO -> {
                val phi = 0.618f
                val invPhi = 1f - phi

                drawLine(gridColor, Offset(w * invPhi, 0f), Offset(w * invPhi, h), strokeWidth)
                drawLine(gridColor, Offset(w * phi, 0f), Offset(w * phi, h), strokeWidth)

                drawLine(gridColor, Offset(0f, h * invPhi), Offset(w, h * invPhi), strokeWidth)
                drawLine(gridColor, Offset(0f, h * phi), Offset(w, h * phi), strokeWidth)
            }
            GridType.SQUARE -> {
                // Centered 1:1 square
                val squareSize = minOf(w, h)
                val left = (w - squareSize) / 2f
                val top = (h - squareSize) / 2f

                // Draw darkened scrim outside the square
                if (top > 0) {
                    drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 0f), Size(w, top))
                    drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, top + squareSize), Size(w, h - (top + squareSize)))
                } else if (left > 0) {
                    drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 0f), Size(left, h))
                    drawRect(Color.Black.copy(alpha = 0.5f), Offset(left + squareSize, 0f), Size(w - (left + squareSize), h))
                }

                // Square border
                drawRect(
                    color = gridColor,
                    topLeft = Offset(left, top),
                    size = Size(squareSize, squareSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
        }
    }
}
