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
    fun testQuadBayerAndHighResDetection() {
        // 1. 100MP High-Res sensor test (e.g. 11520 x 8640 = 99.53 MP with 12.5 MP binned output)
        val binnedMp100 = 12.5
        val maxAvailableMp100 = 99.53
        val isHighRes100 = maxAvailableMp100 > (binnedMp100 * 1.15)
        assertTrue(isHighRes100)
        val calculated100 = if (isHighRes100) maxAvailableMp100 else binnedMp100
        assertEquals(99.53, calculated100, 0.01)

        // 2. 50MP sensor test (e.g. 8192 x 6144 = 50.33 MP with 12.5 MP binned output)
        val binnedMp50 = 12.5
        val maxAvailableMp50 = 50.33
        val isHighRes50 = maxAvailableMp50 > (binnedMp50 * 1.15)
        assertTrue(isHighRes50)
        val calculated50 = if (isHighRes50) maxAvailableMp50 else binnedMp50
        assertEquals(50.33, calculated50, 0.01)

        // 3. True 12.5MP sensor (no false 50MP guessing)
        val standardMp12 = 12.5
        val maxAvailableMp12 = 12.5
        val isHighRes12 = maxAvailableMp12 > (standardMp12 * 1.15)
        org.junit.Assert.assertFalse(isHighRes12)
        val calculated12 = if (isHighRes12) maxAvailableMp12 else standardMp12
        assertEquals(12.5, calculated12, 0.01)
    }

    @Test
    fun testAdvertisedTierClassification() {
        // Test 1: Explicit 108MP HAL array
        val maxMp108 = 108.0
        val standardMp108 = 12.0
        val isExplicit108 = maxMp108 > (standardMp108 * 1.15)
        assertTrue(isExplicit108)
        val tier108 = if (maxMp108 in 90.0..150.0) "%.0f MP High-Res Sensor Matrix".format(java.util.Locale.US, maxMp108) else ""
        assertEquals("108 MP High-Res Sensor Matrix", tier108)

        // Test 2: Standard 12.5MP stream on Rear Wide (OEM hidden 50MP/100MP binning)
        val isFrontBack = false
        val isWide = true
        val standardMp = 12.5
        val isQuadBayerClass = !isFrontBack && isWide && standardMp in 11.5..13.5
        assertTrue(isQuadBayerClass)

        // Test 3: Standard 16MP stream on Rear (64MP 4-in-1 binning)
        val standardMp16 = 16.0
        val is64MpClass = !isFrontBack && standardMp16 in 15.0..16.5
        assertTrue(is64MpClass)

        // Test 4: Standard 8MP stream on Front Selfie (32MP 4-in-1 binning)
        val isFront = true
        val standardMp8 = 8.0
        val is32MpSelfie = isFront && standardMp8 in 7.5..8.5
        assertTrue(is32MpSelfie)
    }
}

