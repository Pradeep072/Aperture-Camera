package com.aperture.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.hypot

class CameraSpecParsingTest {

    @Test
    fun test35mmEquivalentCalculation() {
        // Sensor size: 6.4mm x 4.8mm (typical 1/2" sensor, diag = 8.0mm)
        val sensorW = 6.4f
        val sensorH = 4.8f
        val focalLengthMm = 5.0f

        val sensorDiag = hypot(sensorW.toDouble(), sensorH.toDouble()).toFloat()
        assertEquals(8.0f, sensorDiag, 0.01f)

        val cropFactor = 43.267f / sensorDiag
        val focalLength35mm = focalLengthMm * cropFactor

        // Expected 35mm equivalent ~27.04mm
        assertEquals(27.04f, focalLength35mm, 0.1f)
    }

    @Test
    fun testFovCalculation() {
        val sensorWidth = 6.4f
        val focalLength = 5.0f

        val fovHorizontal = (2f * atan(sensorWidth / (2f * focalLength)) * (180f / Math.PI.toFloat()))
        // Expected FOV ~65.2 degrees
        assertTrue(fovHorizontal in 64.0f..66.0f)
    }

    @Test
    fun testQuadBayerBinningDetection() {
        val standardMp = 12.5
        val maxMp = 50.0

        val isQuadBayer = maxMp > (standardMp * 1.5)
        assertTrue(isQuadBayer)

        val detectedMatrixMp = when {
            maxMp > (standardMp * 1.5) -> maxMp
            standardMp in 11.0..13.5 -> 50.0
            else -> standardMp
        }

        assertEquals(50.0, detectedMatrixMp, 0.01)
    }
}
