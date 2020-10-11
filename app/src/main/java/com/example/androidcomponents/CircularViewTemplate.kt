package com.example.androidcomponents

import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.ImageView
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class CircularViewTemplate(context: Context): View(context) {

    var noOfButtons: Int = 0
    var mode: Mode = Mode.RADIAL
    var shouldVibrateOnSelection = false
    var isSeparatorsEnabled = true
    var isSkeletonEnabled = true
    var separatorColor = Color.BLACK
    var panelColor = Color.parseColor("#3848A5")
    var isCloseButtonEnabled = true

    private lateinit var canvas: Canvas
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {

        this.canvas = canvas

        val x = width
        val y = height
        val radius = width/2

        paint.style = Paint.Style.FILL
        paint.color = Color.TRANSPARENT
        canvas.drawPaint(paint)

        if (isSkeletonEnabled) {
            // Use Color.parseColor to define HTML colors
            // Use Color.parseColor to define HTML colors
            paint.color = Color.parseColor("#CD5C5C")
            paint.color = panelColor
            if (this.mode == Mode.RADIAL) {
                canvas.drawCircle((x / 2).toFloat(), y.toFloat(), radius.toFloat(), paint)
                if (isCloseButtonEnabled) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.BLACK
                    canvas.drawCircle((x / 2).toFloat(), y.toFloat(), (width/7.9).toFloat(), paint)
                }
            } else {
                canvas.drawCircle((x / 2).toFloat(), y / 2.toFloat(), radius.toFloat(), paint)
                if (isCloseButtonEnabled) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.BLACK
                    canvas.drawCircle((x / 2).toFloat(), y/2.toFloat(), (width/9.9).toFloat(), paint)
                }
            }
            paint.color = separatorColor
            if (isSeparatorsEnabled) {
                drawSeparators()
            }
        }

    }

    private fun drawSeparators() {
        if (this.mode == Mode.CIRCULAR) {
            when(noOfButtons) {
                2 -> {
                    drawLine(0.00)
                    drawLine(180.00)
                }
                3 -> {
                    drawLine(210.00)
                    drawLine(330.00)
                    drawLine(90.00)
                }
                4 -> {
                    drawLine(45.00)
                    drawLine(135.00)
                    drawLine(225.00)
                    drawLine(315.00)
                }
                5 -> {
                    drawLine(162.00)
                    drawLine(18.00)
                    drawLine(234.00)
                    drawLine(306.00)
                    drawLine(90.00)


                }
                6 -> {
                    drawLine(300.00)
                    drawLine(0.00)
                    drawLine(60.00)
                    drawLine(120.00)
                    drawLine(180.00)
                    drawLine(240.00)
                }
                7 -> {
                    drawLine(90.00)
                    drawLine(141.40)
                    drawLine(192.80)
                    drawLine(244.20)
                    drawLine(295.60)
                    drawLine(347.00)
                    drawLine(38.40)
                }
                8 -> {
                    drawLine(292.50)
                    drawLine(337.50)
                    drawLine(22.50)
                    drawLine(67.50)
                    drawLine(112.50)
                    drawLine(157.50)
                    drawLine(202.50)
                    drawLine(247.50)
                }
                9 -> {
                    drawLine(10.00)
                    drawLine(50.00)
                    drawLine(90.00)
                    drawLine(130.00)
                    drawLine(170.00)
                    drawLine(210.00)
                    drawLine(250.00)
                    drawLine(290.00)
                    drawLine(330.00)
                }
                10 -> {
                    drawLine(36.00)
                    drawLine(72.00)
                    drawLine(108.00)
                    drawLine(144.00)
                    drawLine(180.00)
                    drawLine(216.00)
                    drawLine(252.00)
                    drawLine(288.00)
                    drawLine(324.00)
                    drawLine(360.00)
                }
            }
        } else if (this.mode == Mode.RADIAL) {
            when(noOfButtons) {
                2 -> {
                    drawLine(270.00)
                }
                3 -> {
                    drawLine(240.00)
                    drawLine(300.00)
                }
                4 -> {
                    drawLine(225.00)
                    drawLine(270.00)
                    drawLine(315.00)
                }
                5, 9, 10 -> {
                    drawLine(216.00)
                    drawLine(252.00)
                    drawLine(288.00)
                    drawLine(324.00)
                }
                6, 7, 8 -> {
                    drawLine(240.00)
                    drawLine(300.00)
                }
            }
        }
    }

    private fun drawLine(degree: Double) {
        val angle = degree * (Math.PI/180F)
        if (this.mode == Mode.RADIAL) {
            canvas.drawLine((width/ 2).toFloat(), height.toFloat(), getX(angle).toFloat(), getY(angle).toFloat(), paint)
        } else {
            canvas.drawLine((width/ 2).toFloat(), height/2.toFloat(), getX(angle).toFloat(), getY(angle).toFloat(), paint)
        }
    }

    private fun getX(angle: Double): Double {
        return if (this.mode == Mode.RADIAL) {
            (width/2) + (width/2) * cos(angle)
        } else {
            width/2 + (width/2) * cos(angle)
        }
    }

    private fun getY(angle: Double): Double {
        return if (this.mode == Mode.RADIAL) {
            height + (width/2) * sin(angle)
        } else {
            height/2 + (width/2) * sin(angle)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (this.mode == Mode.RADIAL) {
            setMeasuredDimension(widthMeasureSpec, widthMeasureSpec/2)
        } else if (this.mode == Mode.CIRCULAR ) {
            setMeasuredDimension(widthMeasureSpec, widthMeasureSpec)
        }
    }
}