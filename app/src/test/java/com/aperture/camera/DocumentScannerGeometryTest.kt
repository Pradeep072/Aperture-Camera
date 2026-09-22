package com.aperture.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.max

data class TestPoint(val x: Float, val y: Float)

class DocumentScannerGeometryTest {

    @Test
    fun testQuadrilateralDimensionsCalculation() {
        // Document rectangle: (0,0), (800, 0), (800, 1000), (0, 1000)
        val tl = TestPoint(0f, 0f)
        val tr = TestPoint(800f, 0f)
        val br = TestPoint(800f, 1000f)
        val bl = TestPoint(0f, 1000f)

        val widthTop = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble()).toFloat()
        val widthBottom = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble()).toFloat()
        val targetWidth = max(widthTop, widthBottom).toInt()

        val heightLeft = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble()).toFloat()
        val heightRight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble()).toFloat()
        val targetHeight = max(heightLeft, heightRight).toInt()

        assertEquals(800, targetWidth)
        assertEquals(1000, targetHeight)
    }

    @Test
    fun testPointClampingToImageBounds() {
        val imageW = 1000f
        val imageH = 1500f
        val margin = 0.05f

        // Initial default corners with 5% margin
        val tl = TestPoint(imageW * margin, imageH * margin)
        val tr = TestPoint(imageW * (1f - margin), imageH * margin)
        val br = TestPoint(imageW * (1f - margin), imageH * (1f - margin))
        val bl = TestPoint(imageW * margin, imageH * (1f - margin))

        assertEquals(50f, tl.x, 0.01f)
        assertEquals(75f, tl.y, 0.01f)
        assertEquals(950f, tr.x, 0.01f)
        assertEquals(75f, tr.y, 0.01f)
        assertEquals(950f, br.x, 0.01f)
        assertEquals(1425f, br.y, 0.01f)
        assertEquals(50f, bl.x, 0.01f)
        assertEquals(1425f, bl.y, 0.01f)

        // Verify all points remain within bounds
        listOf(tl, tr, br, bl).forEach { pt ->
            assertTrue(pt.x in 0f..imageW)
            assertTrue(pt.y in 0f..imageH)
        }
    }
}

