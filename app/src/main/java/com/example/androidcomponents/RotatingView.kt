package com.example.androidcomponents

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.annotation.RequiresApi
import java.security.InvalidParameterException
import java.util.*
import kotlin.math.*


@SuppressLint("ViewConstructor")
class RotatingView(context: Context, var selectionCallback: Callback, val buttons: List<ImageView>): ViewGroup(context), GestureDetector.OnGestureListener {

    private val mode = selectionCallback.getMode()
    private val vibrationEffect = VibrationEffect.createOneShot(20, 1)
    private val vb = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    enum class FirstChildPosition(val angle: Int) {
        EAST(0), SOUTH(270), WEST(180), NORTH(270);
    }

    // Child sizes
    private var maxChildWidth: Int = 0
    private var maxChildHeight = 0

    // Sizes of the ViewGroup
    private var circleWidth = 0  // Sizes of the ViewGroup
    private var circleHeight = 0
    private var radius = -1f

    // Touch detection
    private var gestureDetector: GestureDetector? = null

    // Detecting inverse rotations
    private lateinit var quadrantTouched: BooleanArray

    // Settings of the ViewGroup
    private var speed = 25
    private var angle: Float = 270F
    private val firstChildPosition: FirstChildPosition = FirstChildPosition.SOUTH

    // Touch helpers
    private var touchStartAngle = 0.0
    private var didMove = false

    // Rotation animator
    private var animator: ObjectAnimator? = null

    // Tapped and selected child
    private var selectedView: View? = null

    private var isRotating = selectionCallback.isRotatable()

    init {
        gestureDetector = GestureDetector(context, this)
        quadrantTouched = booleanArrayOf(false, false, false, false, false)
        this.setBackgroundColor(Color.TRANSPARENT)
        this.buttons.forEach { button -> addView(button) }
        setChildAngles()
    }

    fun getAngle(): Float {
        return angle
    }

    fun setAngle(angle: Float) {
        this.angle = angle % 360
        setChildAngles()
    }

    fun getSpeed(): Int {
        return speed
    }

    fun setSpeed(speed: Int) {
        if (speed <= 0) {
            throw InvalidParameterException("Speed must be a positive integer number")
        }
        this.speed = speed
    }

    fun getRadius(): Float {
        return radius
    }

    fun setRadius(radius: Float) {
        if (radius < 0) {
            throw InvalidParameterException("Radius cannot be negative")
        }
        this.radius = radius
        setChildAngles()
    }

    fun isRotating(): Boolean {
        return isRotating
    }

    fun setRotating(isRotating: Boolean) {
        this.isRotating = isRotating
    }

    fun getFirstChildPosition(): FirstChildPosition {
        return firstChildPosition
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val layoutWidth = r - l
        val layoutHeight = b - t
        if (radius < 0) {
            radius = if (layoutWidth <= layoutHeight) layoutWidth / 2.toFloat() else layoutHeight / 2.toFloat()
        }
        circleHeight = height - 100
        circleWidth = width - 100
        setChildAngles()
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        updateAngle()
    }

    override fun removeViewAt(index: Int) {
        super.removeViewAt(index)
        updateAngle()
    }

    override fun removeViews(start: Int, count: Int) {
        super.removeViews(start, count)
        updateAngle()
    }

    override fun addView(
        child: View?,
        index: Int,
        params: LayoutParams?
    ) {
        super.addView(child, index, params)
    }

    override fun addView(child: View?) {
        super.addView(child)
    }

    private fun stopAnimation() {
        if (animator != null && animator!!.isRunning) {
            animator!!.cancel()
            animator = null
        }
    }

    /**
     * @return The angle of the unit circle with the image views center
     */
    private fun getPositionAngle(xTouch: Double, yTouch: Double): Double {
        val x: Double = xTouch - width / 2.0
        val y: Double = height - yTouch - height / 2.0
        val angle =
            asin(y / hypot(x, y)) * 180 / Math.PI
        return when (getPositionQuadrant(x, y)) {
            1 -> angle
            2, 3 -> 180 - angle
            4 -> 360 + angle
            else ->
                // ignore, does not happen
                0.00
        }
    }

    private fun updateAngle() {
        // Update the position of the views, so we know which is the selected
//        setChildAngles()
        rotateViewToCenter(selectedView)
    }

    /**
     * Rotates the given view to the firstChildPosition
     * @param view the view to be rotated
     */
    fun rotateViewToCenter(view: View?) {
        if (isRotating) {
            val viewAngle: Float = if (view?.tag != null) view.tag as Float else 0F
            var destAngle: Float = firstChildPosition.angle - viewAngle
            if (destAngle < 0F) {
                destAngle += 360f
            }
            if (destAngle > 180F) {
                destAngle = -1 * (360 - destAngle)
            }
            animateTo(angle + destAngle, 7500L / speed)
        }
    }

    private fun rotateButtons(degrees: Float) {
        angle += degrees
        setChildAngles()
    }

    private fun setChildAngles() {
        var left: Int
        var top: Int
        var childWidth: Int
        var childHeight: Int
        val childCount = this.buttons.size
        val angleDelay = 360.0F / childCount
        val halfAngle = angleDelay / 2
        var localAngle = angle

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) {
                continue
            }
            if (localAngle > 360) {
                localAngle -= 360f
            } else if (localAngle < 0) {
                localAngle += 360f
            }
            childWidth = child.layoutParams.width
            childHeight = child.layoutParams.height
            left = (width / 2.5 - childWidth / 3.0 + width / 2.5
                    * cos(Math.toRadians(localAngle.toDouble()))).toFloat().roundToInt()
            top = (height / 2.5 - childHeight / 3.0 + width / 2.5
                    * sin(Math.toRadians(localAngle.toDouble()))).toFloat().roundToInt()
            child.tag = localAngle
            val distance = abs(localAngle - firstChildPosition.angle)
            val isFirstItem = distance <= halfAngle || distance >= 360 - halfAngle
            if (isFirstItem && selectedView !== this) {
                selectedView = child
            }
            val err = (width/11)

            // 9,10 - (width/2.5).toInt()
            // 8,7,6 - (width/1.7).toInt() // (width/3.5).toInt()

            if (mode == Mode.CIRCULAR) {
                child.layout(left + err, top + err, left + childWidth + err, top + childHeight + err)
            } else {
                if (buttons.size == 9 || buttons.size == 10) {
                    child.layout(left + err, top + err, left + childWidth + err, top + childHeight + err + (width/2.5).toInt())
                } else if (buttons.size == 6 || buttons.size == 7 || buttons.size == 8) {
                    child.layout(left + err, top + err, left + childWidth + err, top + childHeight + err + (width/1.7).toInt())
                }
            }

//            val anim0 = TranslateAnimation(
//                width / 2.toFloat(),
//                (left + err).toFloat(),
//                height.toFloat(),
//                top + childHeight.toFloat()
//            )
//            anim0.duration = 700
//            anim0.fillAfter = true
//            buttons[0].startAnimation(anim0)

            localAngle += angleDelay
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            gestureDetector!!.onTouchEvent(event)
            if (isRotating) {

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // reset the touched quadrants
                        Arrays.fill(quadrantTouched, false)
                        stopAnimation()
                        touchStartAngle = getPositionAngle(
                            event.x.toDouble(),
                            event.y.toDouble()
                        )
                        didMove = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val currentAngle: Double = getPositionAngle(
                            event.x.toDouble(),
                            event.y.toDouble()
                        )
                        rotateButtons((touchStartAngle - currentAngle).toFloat())
                        touchStartAngle = currentAngle
                        didMove = true
                    }
                    MotionEvent.ACTION_UP -> if (didMove) {
                        val currentAngle: Double = getPositionAngle(
                            event.x.toDouble(),
                            event.y.toDouble()
                        )
                        println("<<< TOUCHED $currentAngle ${event.x} ${event.y}")
                        rotateViewToCenter(selectedView)
                    }
                    else -> {
                        // nothing
                    }
                }

                // set the touched quadrant to true
                quadrantTouched[getPositionQuadrant(
                    (event.x - width / 2).toDouble(),
                    (height - event.y - height / 2).toDouble()
                )] = true

                if (selectionCallback.shouldVibrateOnSelection()) {
                  vb.vibrate(vibrationEffect)
                }
            }

        }

        performClick()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }


    private fun onClick(event: MotionEvent) {
        val R: Float
        val A: Float

        if (selectionCallback.getMode() == Mode.RADIAL) {
            R = sqrt((event.x - width / 2).pow(2) + (event.y - this.height).pow(2))
            A = atan2(event.y - this.height, event.x - width / 2)
        } else {
            R = sqrt((event.x - width / 2).pow(2) + (event.y - this.height / 2).pow(2))
            A = atan2(event.y - this.height / 2, event.x - width / 2)
        }

        val deg = Math.toDegrees(A.toDouble())
        var degree = deg + 360
        if (deg + 360 > 180 && deg + 360 < 270) {
            degree += 360
        }

        if (R <= this.height) {
            if (selectionCallback.getMode() == Mode.RADIAL) {
                degree = deg + 360
                when (buttons.size) {
                    1 -> {
                        selectionCallback.onItemClick(buttons[0].id)
                    }
                    2 -> {
                        if (degree > 180 && degree < 270) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else {
                            selectionCallback.onItemClick(buttons[1].id)
                        }
                    }
                    3 -> {
                        if (degree > 180 && degree < 240) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 240 && degree < 300) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else {
                            selectionCallback.onItemClick(buttons[2].id)
                        }
                    }
                    4 -> {
                        if (degree > 180 && degree < 225) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 225 && degree < 270) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 270 && degree < 315) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else {
                            selectionCallback.onItemClick(buttons[3].id)
                        }
                    }
                    5 -> {
                        if (degree > 180 && degree < 216) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 216 && degree < 252) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 252 && degree < 288) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 288 && degree < 324) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else {
                            selectionCallback.onItemClick(buttons[4].id)
                        }
                    }
                }
            } else if (selectionCallback.getMode() == Mode.CIRCULAR) {
                when (buttons.size) {
                    1 -> {
                        selectionCallback.onItemClick(buttons[0].id)
                    }
                    2 -> {
                        if (degree > 270 && degree < 450) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else {
                            selectionCallback.onItemClick(buttons[1].id)
                        }
                    }
                    3 -> {
                        if (degree > 270 && degree < 390) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 390 && degree < 510) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else {
                            selectionCallback.onItemClick(buttons[2].id)
                        }
                    }
                    4 -> {
                        if (degree > 270 && degree < 360) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 360 && degree < 450) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 450 && degree < 540) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else {
                            selectionCallback.onItemClick(buttons[3].id)
                        }
                    }
                    5 -> {
                        if (degree > 270 && degree < 342) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 342 && degree < 414) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 414 && degree < 486) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 486 && degree < 558) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else {
                            selectionCallback.onItemClick(buttons[4].id)
                        }
                    }
                    6 -> {
                        if (degree > 270 && degree < 330) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 330 && degree < 390) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 390 && degree < 450) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 450 && degree < 510) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else if (degree > 510 && degree < 570) {
                            selectionCallback.onItemClick(buttons[4].id)
                        } else {
                            selectionCallback.onItemClick(buttons[5].id)
                        }
                    }
                    7 -> {
                        if (degree > 270 && degree < 321.4) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 321.4 && degree < 372.8) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 372.8 && degree < 424.2) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 424.2 && degree < 475.6) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else if (degree > 475.6 && degree < 527) {
                            selectionCallback.onItemClick(buttons[4].id)
                        } else if (degree > 527 && degree < 578.4) {
                            selectionCallback.onItemClick(buttons[5].id)
                        } else {
                            selectionCallback.onItemClick(buttons[6].id)
                        }
                    }
                    8 -> {
                        if (degree > 270 && degree < 315) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 315 && degree < 360) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 360 && degree < 405) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 405 && degree < 450) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else if (degree > 450 && degree < 495) {
                            selectionCallback.onItemClick(buttons[4].id)
                        } else if (degree > 495 && degree < 540) {
                            selectionCallback.onItemClick(buttons[5].id)
                        } else if (degree > 540 && degree < 585) {
                            selectionCallback.onItemClick(buttons[6].id)
                        } else {
                            selectionCallback.onItemClick(buttons[7].id)
                        }
                    }
                    9 -> {
                        if (degree > 270 && degree < 310) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 310 && degree < 350) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 350 && degree < 390) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 390 && degree < 430) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else if (degree > 430 && degree < 470) {
                            selectionCallback.onItemClick(buttons[4].id)
                        } else if (degree > 470 && degree < 510) {
                            selectionCallback.onItemClick(buttons[5].id)
                        } else if (degree > 510 && degree < 550) {
                            selectionCallback.onItemClick(buttons[6].id)
                        } else if (degree > 550 && degree < 590) {
                            selectionCallback.onItemClick(buttons[7].id)
                        } else {
                            selectionCallback.onItemClick(buttons[8].id)
                        }
                    }
                    10 -> {
                        if (degree > 270 && degree < 306) {
                            selectionCallback.onItemClick(buttons[0].id)
                        } else if (degree > 306 && degree < 342) {
                            selectionCallback.onItemClick(buttons[1].id)
                        } else if (degree > 342 && degree < 378) {
                            selectionCallback.onItemClick(buttons[2].id)
                        } else if (degree > 378 && degree < 414) {
                            selectionCallback.onItemClick(buttons[3].id)
                        } else if (degree > 414 && degree < 450) {
                            selectionCallback.onItemClick(buttons[4].id)
                        } else if (degree > 450 && degree < 486) {
                            selectionCallback.onItemClick(buttons[5].id)
                        } else if (degree > 486 && degree < 522) {
                            selectionCallback.onItemClick(buttons[6].id)
                        } else if (degree > 522 && degree < 558) {
                            selectionCallback.onItemClick(buttons[7].id)
                        } else if (degree > 558 && degree < 594) {
                            selectionCallback.onItemClick(buttons[8].id)
                        } else {
                            selectionCallback.onItemClick(buttons[9].id)
                        }
                    }
                }
            }
        }
    }
    /**
     * @return The quadrant of the position
     */
    private fun getPositionQuadrant(x: Double, y: Double): Int {
        return if (x >= 0) {
            if (y >= 0) 1 else 4
        } else {
            if (y >= 0) 2 else 3
        }
    }

    /**
     * Returns the currently selected menu
     *
     * @return the view which is currently the closest to the first item
     * position
     */
    fun getSelectedItem(): View {
        if (selectedView == null) {
            selectedView = getChildAt(0)
        }
        return selectedView!!
    }

    private fun animateTo(endDegree: Float, duration: Long) {
        if (animator != null && animator!!.isRunning || abs(angle - endDegree) < 1) {
            return
        }
        animator = ObjectAnimator.ofFloat(this, "angle", angle, endDegree)
        animator?.duration = duration
        animator?.interpolator = DecelerateInterpolator()
        animator?.addListener(object : Animator.AnimatorListener {
            private var wasCanceled = false
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                if (wasCanceled) {
                    return
                }
                val view = getSelectedItem()
                this@RotatingView.selectionCallback.onRotationFinished(view.id)
            }

            override fun onAnimationCancel(animation: Animator) {
                wasCanceled = true
            }
        })
        animator?.start()
    }

    private fun pointToChildPosition(x: Float, y: Float): Int {
        for (i in this.buttons.indices) {
            val view: View = getChildAt(i)

//            val positionAngle = getPositionAngle(x.toDouble(), y.toDouble())
//
//            val nX = view.left + view.top/2
//            val nY = view.right + view.bottom/2
//
//            val actualViewAngle = view.tag as Float
//
//            println("<<< POS $positionAngle $actualViewAngle")
//            println("<<< L_R ${view.left} ${view.right} ${view.top} ${view.bottom}")
//
//            if (mode == Mode.CIRCULAR) {
//                when(buttons.size) {
//                    1 -> {}
//                    2 -> {}
//                    3 -> {}
//                    4 -> {}
//                    5 -> {}
//                    6 -> {}
//                    7 -> {}
//                    8 -> {}
//                    9 -> {
//                        if ((actualViewAngle in 10.00..50.00 && positionAngle in 10.00..50.00) ||
//                            (actualViewAngle in 50.00..90.00 && positionAngle in 50.00..90.00) ||
//                            (actualViewAngle in 90.00..130.00 && positionAngle in 90.00..130.00) ||
//                            (actualViewAngle in 130.00..170.00 && positionAngle in 130.00..170.00) ||
//                            (actualViewAngle in 170.00..210.00 && positionAngle in 170.00..210.00) ||
//                            (actualViewAngle in 210.00..250.00 && positionAngle in 210.00..250.00) ||
//                            (actualViewAngle in 250.00..290.00 && positionAngle in 250.00..290.00) ||
//                            (actualViewAngle in 290.00..330.00 && positionAngle in 290.00..330.00) ) {
//                            return i
//                        }
//                    }
//                    10-> {
//                        if ((actualViewAngle in 324.00..360.00 && positionAngle in 324.00..360.00) ||
//                                (actualViewAngle in 288.00..324.00 && positionAngle in 288.00..324.00) ||
//                                (actualViewAngle in 252.00..288.00 && positionAngle in 252.00..288.00) ||
//                                (actualViewAngle in 216.00..252.00 && positionAngle in 216.00..252.00) ||
//                                (actualViewAngle in 180.00..216.00 && positionAngle in 180.00..216.00) ||
//                                (actualViewAngle in 144.00..180.00 && positionAngle in 144.00..180.00) ||
//                                (actualViewAngle in 108.00..144.00 && positionAngle in 108.00..144.00) ||
//                                (actualViewAngle in 72.00..108.00 && positionAngle in 72.00..108.00) ||
//                                (actualViewAngle in 36.00..72.00 && positionAngle in 36.00..72.00) ) {
//                                return i
//                        }
//                    }
//                }
//            }

            if (view.left < x && ((view.right > x).and(view.top < y)) && view.bottom > y) {
                return i
            }
        }
        return -1
    }

    private fun getCenteredAngle(angle: Float): Float {
        var changedAngle = angle

        if (this.buttons.isEmpty()) {
            // Prevent divide by zero
            return changedAngle
        }

        val angleDelay = 360 / this.buttons.size
        var localAngle = angle % 360

        if (localAngle < 0) {
            localAngle += 360;
        }

        var i = firstChildPosition.angle
        while (i < firstChildPosition.angle + 360) {
            val locI = i % 360
            val diff = localAngle - locI;
            if (abs(diff) < angleDelay) {
                changedAngle -= diff;
                break;
            }
            i += angleDelay
        }
        return changedAngle;
    }

    override fun onShowPress(e: MotionEvent?) {
        // do nothing
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        var tappedView: View? = null
        val tappedViewsPosition = pointToChildPosition(e.x, e.y)
        if (tappedViewsPosition >= 0) {
            tappedView = getChildAt(tappedViewsPosition)
            tappedView!!.isPressed = true

        } else {
            // Determine if it was a center click
            val centerX: Float = width / 2f
            val centerY: Float = height / 2f
//            if (onCenterClickListener != null && e.x < centerX + radius - maxChildWidth / 2 && e.x > centerX - radius + maxChildWidth / 2 && e.y < centerY + radius - maxChildHeight / 2 && e.y > centerY - radius + maxChildHeight / 2
//            ) {
//                onCenterClickListener.onCenterClick()
//                return true
//            }
        }

        if (tappedView != null) {
            if (selectedView === tappedView) {
                this.selectionCallback.onItemClick(tappedView.id)

            } else {
                rotateViewToCenter(tappedView)
                this.selectionCallback.onItemClick(tappedView.id)
            }
            return true
        }
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (!isRotating) {
            return false
        }
        // get the quadrant of the start and the end of the fling
        val q1: Int = getPositionQuadrant(
            e1.x - (width / 2).toDouble(), height - e1.y - (height / 2).toDouble()
        )
        val q2: Int = getPositionQuadrant(
            e2.x - (width / 2).toDouble(), height - e2.y - (height / 2).toDouble()
        )
        if (q1 == 2 && q2 == 2 && abs(velocityX) < abs(velocityY)
            || q1 == 3 && q2 == 3
            || q1 == 1 && q2 == 3
            || q1 == 4 && q2 == 4 && abs(velocityX) > abs(velocityY)
            || q1 == 2 && q2 == 3 || q1 == 3 && q2 == 2
            || q1 == 3 && q2 == 4 || q1 == 4 && q2 == 3
            || q1 == 2 && q2 == 4 && quadrantTouched[3]
            || q1 == 4 && q2 == 2 && quadrantTouched[3]
        ) {
            // the inverted rotations
            animateTo(getCenteredAngle(angle - (velocityX + velocityY) / 25), 25000L / speed)
        } else {
            // the normal rotation
            animateTo(getCenteredAngle(angle + (velocityX + velocityY) / 25), 25000L / speed)
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        // do nothing
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure child views first
        maxChildWidth = 0
        maxChildHeight = 0

        if (this.selectionCallback.getMode() == Mode.RADIAL) {
            setMeasuredDimension(widthMeasureSpec, widthMeasureSpec/2 )
        } else {
            setMeasuredDimension(widthMeasureSpec, widthMeasureSpec)
        }

//        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
//            MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST
//        )
//        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
//            MeasureSpec.getSize(widthMeasureSpec/2), MeasureSpec.AT_MOST
//        )/2
//
//        val childCount = childCount
//        for (i in 0 until childCount) {
//            val child = getChildAt(i)
//            if (child.visibility == View.GONE) {
//                continue
//            }
//            measureChild(child, childWidthMeasureSpec, childHeightMeasureSpec)
//            maxChildWidth = maxChildWidth.coerceAtLeast(child.measuredWidth)
//            maxChildHeight = maxChildHeight.coerceAtLeast(child.measuredHeight)/2
//        }
//
//        // Then decide what size we want to be
//
//        // Then decide what size we want to be
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//
//        val width: Int
//        val height: Int
//
//        //Measure Width
//        width = when (widthMode) {
//            MeasureSpec.EXACTLY -> {
//                //Must be this size
//                widthSize
//            }
//            MeasureSpec.AT_MOST -> {
//                //Can't be bigger than...
//                Math.min(widthSize, heightSize/2)
//            }
//            else -> {
//                //Be whatever you want
//                maxChildWidth * 3
//            }
//        }
//
//        //Measure Height
//        height = when (heightMode) {
//            MeasureSpec.EXACTLY -> {
//                //Must be this size
//                heightSize
//            }
//            MeasureSpec.AT_MOST -> {
//                //Can't be bigger than...
//                Math.min(heightSize/2, widthSize/2)
//            }
//            else -> {
//                //Be whatever you want
//                maxChildHeight * 3
//            }
//        }
//
//        setMeasuredDimension(
//            View.resolveSize(width, widthMeasureSpec),
//            View.resolveSize(width/2, widthMeasureSpec/2)
//        )
    }
}