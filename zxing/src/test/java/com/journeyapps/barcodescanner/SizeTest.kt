package com.journeyapps.barcodescanner

import org.junit.Assert
import org.junit.Test

class SizeTest {
    @Test
    fun testScale() {
        val a = Size(12, 9)
        val scaled = a.scale(2, 3)
        Assert.assertEquals(8, scaled.width)
        Assert.assertEquals(6, scaled.height)
        Assert.assertEquals(12, a.width)
        Assert.assertEquals(9, a.height)
    }

    @Test
    fun testFitsIn() {
        val a = Size(12, 9)
        Assert.assertTrue(a.fitsIn(a))
        Assert.assertTrue(a.fitsIn(Size(13, 10)))
        Assert.assertTrue(a.fitsIn(Size(13, 9)))
        Assert.assertTrue(a.fitsIn(Size(12, 10)))
        Assert.assertFalse(a.fitsIn(Size(120, 8)))
        Assert.assertFalse(a.fitsIn(Size(11, 900)))
    }

    @Test
    fun testCompare() {
        val a = Size(12, 9)
        Assert.assertEquals(0, a.compareTo(Size(12, 9)))
        Assert.assertEquals(0, a.compareTo(Size(9, 12)))
        Assert.assertEquals(-1, a.compareTo(Size(10, 11)))
        Assert.assertEquals(1, a.compareTo(Size(10, 10)))
    }

    @Test
    fun testRotate() {
        val a = Size(12, 9)
        Assert.assertEquals(Size(9, 12), a.rotate())
    }

    @Test
    fun testScaleCrop() {
        val a = Size(12, 9)
        Assert.assertEquals(Size(120, 90), a.scaleCrop(Size(120, 90)))
        Assert.assertEquals(Size(120, 90), a.scaleCrop(Size(120, 80)))
        Assert.assertEquals(Size(120, 90), a.scaleCrop(Size(110, 90)))
        Assert.assertEquals(Size(110, 82), a.scaleCrop(Size(110, 0)))
    }

    @Test
    fun testScaleFit() {
        val a = Size(12, 9)
        Assert.assertEquals(Size(120, 90), a.scaleFit(Size(120, 90)))
        Assert.assertEquals(Size(120, 90), a.scaleFit(Size(120, 100)))
        Assert.assertEquals(Size(120, 90), a.scaleFit(Size(130, 90)))
        Assert.assertEquals(Size(0, 0), a.scaleFit(Size(110, 0)))
    }
}