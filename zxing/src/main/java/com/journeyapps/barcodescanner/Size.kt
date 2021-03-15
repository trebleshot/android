package com.journeyapps.barcodescanner

class Size(val width: Int, val height: Int) : Comparable<Size> {
    override fun compareTo(other: Size): Int {
        val aPixels = height * width
        val bPixels = other.height * other.width

        return when {
            bPixels < aPixels -> 1
            bPixels > aPixels -> -1
            else -> 0
        }
    }

    override fun equals(other: Any?) = this === other
            || (other is Size && other.width == width && other.height == height)

    fun fitsIn(other: Size): Boolean {
        return width <= other.width && height <= other.height
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        return result
    }

    fun rotate(): Size {
        return Size(height, width)
    }

    fun scale(n: Int, d: Int): Size {
        return Size(width * n / d, height * n / d)
    }

    fun scaleCrop(into: Size): Size {
        return if (width * into.height <= into.width * height) {
            Size(into.width, height * into.width / width)
        } else {
            Size(width * into.height / height, into.height)
        }
    }

    fun scaleFit(into: Size): Size {
        return if (width * into.height >= into.width * height) {
            Size(into.width, height * into.width / width)
        } else {
            Size(width * into.height / height, into.height)
        }
    }

    override fun toString(): String {
        return width.toString() + "x" + height
    }
}