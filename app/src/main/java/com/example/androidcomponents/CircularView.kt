package com.example.androidcomponents

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.TranslateAnimation
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlin.math.*

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ViewConstructor")
class CircularView(context: Context, private var selectionCallback: Callback) :
    ConstraintLayout(context) {

    private val buttons = selectionCallback.getButtons()
    private val mode = selectionCallback.getMode()
    private var rad: CircularViewTemplate
    private lateinit var btn: Button
    private var w: Int = 0
    private var h: Int = 0
    private val vibrationEffect = VibrationEffect.createOneShot(20, 1)
    private val vb = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
    private lateinit var buttonsView: RotatingView

    init {
        checkPreConditions()

        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        h = displayMetrics.widthPixels / 2
        w = displayMetrics.widthPixels

        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        rad = CircularViewTemplate(context)
        rad.id = View.generateViewId()
        rad.noOfButtons = buttons.size
        rad.mode = selectionCallback.getMode()
        rad.shouldVibrateOnSelection = selectionCallback.shouldVibrateOnSelection()
        rad.isCloseButtonEnabled = selectionCallback.isCloseButtonEnabled()
        rad.isSeparatorsEnabled = selectionCallback.isSeparatorsVisible()
        rad.isSkeletonEnabled = selectionCallback.isSkeletonEnabled()

        if (selectionCallback.isCloseButtonEnabled()) {
            btn = Button(context)
            btn.id = View.generateViewId()
            if (selectionCallback.getMode() == Mode.RADIAL) {
                btn.layoutParams = LayoutParams(w / 4, w / 8)
                btn.setBackgroundResource(R.drawable.semicircleshape)
            } else {
                btn.layoutParams = LayoutParams(w / 5, w / 5)
                btn.setBackgroundResource(R.drawable.circleshape)
            }
            btn.text = "X"
            btn.setTextColor(selectionCallback.closeIconColor())
            btn.textSize = 30F
            btn.isHapticFeedbackEnabled = true
            btn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            btn.background.setColorFilter(
                selectionCallback.buttonColor(),
                PorterDuff.Mode.SRC_OVER
            )
            btn.setOnClickListener {
                if (selectionCallback.shouldVibrateOnSelection()) {
                    vb.vibrate(vibrationEffect)
                }
                close()
            }
            this.addView(btn)
        }

        this.addView(rad)

        val conSet = ConstraintSet()
        conSet.clone(this)
        conSet.connect(rad.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        conSet.connect(rad.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        conSet.connect(rad.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        if (selectionCallback.isCloseButtonEnabled()) {
            conSet.connect(btn.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            conSet.connect(btn.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
            conSet.connect(btn.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }

        if (selectionCallback.getMode() == Mode.CIRCULAR) {
            buttonsView = RotatingView(context, selectionCallback, buttons)
            buttonsView.id = View.generateViewId()
            this.addView(buttonsView)

            conSet.clone(this)
            if (selectionCallback.isCloseButtonEnabled()) {
                conSet.connect(btn.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }
            conSet.connect(rad.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            conSet.connect(buttonsView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            conForRotationView(conSet)

        } else {
            when (buttons.size) {
                10, 9, 8, 7, 6 -> {
                    buttonsView = RotatingView(context, selectionCallback, buttons)
                    buttonsView.id = View.generateViewId()
                    this.addView(buttonsView)
                    conForRotationView(conSet)
                }
                else -> {
                    for (button in buttons) {
                        this.addView(button)
                    }
                    when (buttons.size) {
                        5 -> for5(w, h, conSet)
                        4 -> for4(w, h, conSet)
                        3 -> for3(w, h, conSet)
                        2 -> for2(w, h, conSet)
                        1 -> for1(w, h, conSet)
                    }
                }
            }
        }

        conSet.applyTo(this)

        // circular reveal
        rad.viewTreeObserver
            .addOnGlobalLayoutListener {
                var viewHeight = height
                if (selectionCallback.getMode() == Mode.CIRCULAR) {
                    viewHeight = height / 2
                }
                when {
                    selectionCallback.getAnimationType() == AnimationType.FROM_LEFT -> {
                        val anim = ViewAnimationUtils.createCircularReveal(
                            this,
                            0,
                            viewHeight,
                            0f,
                            width.toFloat()
                        )
                        anim.duration = 500
                        this.visibility = View.VISIBLE
                        anim.start()
                    }
                    selectionCallback.getAnimationType() == AnimationType.FROM_RIGHT -> {
                        val anim = ViewAnimationUtils.createCircularReveal(
                            this,
                            width,
                            viewHeight,
                            0f,
                            width.toFloat()
                        )
                        anim.duration = 500
                        this.visibility = View.VISIBLE
                        anim.start()
                    }
                    selectionCallback.getAnimationType() == AnimationType.FROM_CENTER -> {
                        val cx = this.width / 2
                        val cy = this.height / 2
                        val finalRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
                        val anim = ViewAnimationUtils.createCircularReveal(
                            this,
                            width / 2,
                            viewHeight,
                            0f,
                            finalRadius
                        )
                        anim.duration = 500
                        this.visibility = View.VISIBLE
                        anim.start()
                    }
                    selectionCallback.getAnimationType() == AnimationType.NONE -> {
                        this.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun conForRotationView(conSet: ConstraintSet) {
        conSet.connect(rad.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        conSet.connect(rad.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        conSet.connect(rad.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        if (selectionCallback.isCloseButtonEnabled()) {
            conSet.connect(btn.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            conSet.connect(btn.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
            conSet.connect(btn.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }
        conSet.connect(buttonsView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        conSet.connect(buttonsView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        conSet.connect(buttonsView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun for5(w: Int, h: Int, conSet: ConstraintSet) {
        val radius = w / 2
        val iconPosAngle1 = 198.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 234.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 270.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 306.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 342.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h).toFloat() + radius * sin(iconPosAngle5)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val px4 = iconPosX4 - ((w / 5 * (iconPosX4 - px)) / d)
        val py4 = iconPosY4 - ((h / (buttons.size) * (iconPosY4 - h)) / d)

        val px5 = iconPosX5 - ((w / 5 * (iconPosX5 - px)) / d)
        val py5 = iconPosY5 - ((h / (buttons.size) * (iconPosY5 - h)) / d)

        val anim0 = TranslateAnimation(
            w / 2.toFloat(),
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            h.toFloat(),
            py1.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            w / 2.toFloat(),
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            h.toFloat(),
            py2.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            w / 2.toFloat(),
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            h.toFloat(),
            py3.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)

        val anim3 = TranslateAnimation(
            w / 2.toFloat(),
            (px4.toFloat() - buttons[3].layoutParams.width / 2),
            h.toFloat(),
            py4.toFloat()
        )
        anim3.duration = 700
        anim3.fillAfter = true
        anim3.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim3.startOffset = 300
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[3].startAnimation(anim3)

        val anim4 = TranslateAnimation(
            w / 2.toFloat(),
            (px5.toFloat() - buttons[4].layoutParams.width / 2),
            h.toFloat(),
            py5.toFloat()
        )
        anim4.duration = 700
        anim4.fillAfter = true
        anim4.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim4.startOffset = 400
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[4].startAnimation(anim4)
    }

    private fun for4(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 202.50 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 247.50 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 292.50 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 337.50 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h).toFloat() + radius * sin(iconPosAngle4)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val d2 = sqrt((iconPosX2 - w).pow(2) + (iconPosY2 - h).pow(2))
        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d2)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d2)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val px4 = iconPosX4 - ((w / 5 * (iconPosX4 - px)) / d2)
        val py4 = iconPosY4 - ((h / (buttons.size) * (iconPosY4 - h)) / d2)

        val anim0 = TranslateAnimation(
            w / 2.toFloat(),
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            h.toFloat(),
            py1.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            w / 2.toFloat(),
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            h.toFloat(),
            py2.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            w / 2.toFloat(),
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            h.toFloat(),
            py3.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)

        val anim3 = TranslateAnimation(
            w / 2.toFloat(),
            (px4.toFloat() - buttons[3].layoutParams.width / 2),
            h.toFloat(),
            py4.toFloat()
        )
        anim3.duration = 700
        anim3.fillAfter = true
        anim3.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim3.startOffset = 300
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[3].startAnimation(anim3)
    }

    private fun for3(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 210.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 270.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 330.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val anim0 = TranslateAnimation(
            w / 2.toFloat(),
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            h.toFloat(),
            py1.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            w / 2.toFloat(),
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            h.toFloat(),
            py2.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            w / 2.toFloat(),
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            h.toFloat(),
            py3.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)
    }

    private fun for2(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 225.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 315.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val anim0 = TranslateAnimation(
            w / 2.toFloat(),
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            h.toFloat(),
            py1.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            w / 2.toFloat(),
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            h.toFloat(),
            py2.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)
    }

    private fun for1(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 270.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / 3 * (iconPosY1 - h)) / d)

        val anim0 = TranslateAnimation(
            w / 2.toFloat(),
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            h.toFloat(),
            py1.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)
    }

    private fun close() {
        var viewHeight = height
        if (selectionCallback.getMode() == Mode.CIRCULAR) {
            viewHeight = height / 2
        }
        when (buttons.size) {
            5 -> closeFor5()
            4 -> closeFor4()
            3 -> closeFor3()
            2 -> closeFor2()
            1 -> closeFor1()
        }
        when {
            selectionCallback.getAnimationType() == AnimationType.FROM_LEFT -> {
                val anim = ViewAnimationUtils.createCircularReveal(
                    this,
                    0,
                    viewHeight,
                    width.toFloat(),
                    0f
                )
                anim.duration = 500
                anim.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        this@CircularView.visibility = View.INVISIBLE
                    }
                })
                anim.start()
            }
            selectionCallback.getAnimationType() == AnimationType.FROM_RIGHT -> {
                val anim = ViewAnimationUtils.createCircularReveal(
                    this,
                    width,
                    viewHeight,
                    width.toFloat(),
                    0f
                )
                anim.duration = 500
                anim.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        this@CircularView.visibility = View.INVISIBLE
                    }
                })
                anim.start()
            }
            selectionCallback.getAnimationType() == AnimationType.FROM_CENTER -> {
                val cx = this.width / 2
                val cy = this.height / 2
                val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
                val anim = ViewAnimationUtils.createCircularReveal(
                    this,
                    width / 2,
                    viewHeight,
                    initialRadius,
                    0f
                )
                anim.startDelay = 600
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        this@CircularView.visibility = View.INVISIBLE
                    }

                    override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {

                    }
                })
                anim.start()
            }
            selectionCallback.getAnimationType() == AnimationType.NONE -> {
                this@CircularView.visibility = View.INVISIBLE
            }
        }
    }

    private fun closeFor5() {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 198.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 234.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 270.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 306.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 342.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h).toFloat() + radius * sin(iconPosAngle5)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val px4 = iconPosX4 - ((w / 5 * (iconPosX4 - px)) / d)
        val py4 = iconPosY4 - ((h / (buttons.size) * (iconPosY4 - h)) / d)

        val px5 = iconPosX5 - ((w / 5 * (iconPosX5 - px)) / d)
        val py5 = iconPosY5 - ((h / (buttons.size) * (iconPosY5 - h)) / d)

        val anim0 = TranslateAnimation(
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            w / 2.toFloat(),
            py1.toFloat(),
            h.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            w / 2.toFloat(),
            py2.toFloat(),
            h.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            w / 2.toFloat(),
            py3.toFloat(),
            h.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)

        val anim3 = TranslateAnimation(
            (px4.toFloat() - buttons[3].layoutParams.width / 2),
            w / 2.toFloat(),
            py4.toFloat(),
            h.toFloat()
        )
        anim3.duration = 700
        anim3.fillAfter = true
        anim3.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim3.startOffset = 300
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[3].startAnimation(anim3)

        val anim4 = TranslateAnimation(
            (px5.toFloat() - buttons[4].layoutParams.width / 2),
            w / 2.toFloat(),
            py5.toFloat(),
            h.toFloat()
        )
        anim4.duration = 700
        anim4.fillAfter = true
        anim4.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim4.startOffset = 400
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[4].startAnimation(anim4)
    }

    private fun closeFor4() {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 202.50 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 247.50 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 292.50 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 337.50 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h).toFloat() + radius * sin(iconPosAngle4)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val d2 = sqrt((iconPosX2 - w).pow(2) + (iconPosY2 - h).pow(2))
        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d2)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d2)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val px4 = iconPosX4 - ((w / 5 * (iconPosX4 - px)) / d2)
        val py4 = iconPosY4 - ((h / (buttons.size) * (iconPosY4 - h)) / d2)

        val anim0 = TranslateAnimation(
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            w / 2.toFloat(),
            py1.toFloat(),
            h.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            w / 2.toFloat(),
            py2.toFloat(),
            h.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            w / 2.toFloat(),
            py3.toFloat(),
            h.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)

        val anim3 = TranslateAnimation(
            (px4.toFloat() - buttons[3].layoutParams.width / 2),
            w / 2.toFloat(),
            py4.toFloat(),
            h.toFloat()
        )
        anim3.duration = 700
        anim3.fillAfter = true
        anim3.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim3.startOffset = 300
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[3].startAnimation(anim3)
    }

    private fun closeFor3() {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 210.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 270.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 330.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h).toFloat() + radius * sin(iconPosAngle3)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val px3 = iconPosX3 - ((w / 5 * (iconPosX3 - px)) / d)
        val py3 = iconPosY3 - ((h / (buttons.size) * (iconPosY3 - h)) / d)

        val anim0 = TranslateAnimation(
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            w / 2.toFloat(),
            py1.toFloat(),
            h.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            w / 2.toFloat(),
            py2.toFloat(),
            h.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)

        val anim2 = TranslateAnimation(
            (px3.toFloat() - buttons[2].layoutParams.width / 2),
            w / 2.toFloat(),
            py3.toFloat(),
            h.toFloat()
        )
        anim2.duration = 700
        anim2.fillAfter = true
        anim2.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim2.startOffset = 200
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[2].startAnimation(anim2)
    }

    private fun closeFor2() {
        // drawing lines to mark icon positions
        val radius = w / 2
        val iconPosAngle1 = 225.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 315.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h).toFloat() + radius * sin(iconPosAngle2)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / (buttons.size) * (iconPosY1 - h)) / d)

        val px2 = iconPosX2 - ((w / 5 * (iconPosX2 - px)) / d)
        val py2 = iconPosY2 - ((h / (buttons.size) * (iconPosY2 - h)) / d)

        val anim0 = TranslateAnimation(
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            w / 2.toFloat(),
            py1.toFloat(),
            h.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)

        val anim1 = TranslateAnimation(
            (px2.toFloat() - buttons[1].layoutParams.width / 2),
            w / 2.toFloat(),
            py2.toFloat(),
            h.toFloat()
        )
        anim1.duration = 700
        anim1.fillAfter = true
        anim1.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {
                anim1.startOffset = 100
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        buttons[1].startAnimation(anim1)
    }

    private fun closeFor1() {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 270.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h).toFloat() + radius * sin(iconPosAngle1)

        val d = sqrt((iconPosX1 - w).pow(2) + (iconPosY1 - h).pow(2))
        val px = w / 2

        // Icon positions
        val px1 = iconPosX1 - ((w / 5 * (iconPosX1 - px)) / d)
        val py1 = iconPosY1 - ((h / 3 * (iconPosY1 - h)) / d)

        val anim0 = TranslateAnimation(
            (px1.toFloat() - buttons[0].layoutParams.width / 2),
            w / 2.toFloat(),
            py1.toFloat(),
            h.toFloat()
        )
        anim0.duration = 700
        anim0.fillAfter = true
        buttons[0].startAnimation(anim0)
    }

    private fun checkPreConditions() {
        if (buttons.size > 10 || buttons.isEmpty()) {
            selectionCallback.onError(Exception("Number of buttons is off limits."))
            return
        }

        for (view in buttons) {
            if (view.id == View.NO_ID) {
                selectionCallback.onError(Exception("Please provide an ID to the button."))
                return
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (selectionCallback.getMode() == Mode.RADIAL) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec / 2)
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTouchEvent(event: MotionEvent): Boolean {
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
            }
        } else {
            if (selectionCallback.closeOnTouchOutside()) {
                close()
            }
        }
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun forCircular10(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 288.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 324.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 360.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 36.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 72.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val iconPosAngle6 = 108.00 * (Math.PI / 180F)
        val iconPosX6 = (w / 2).toFloat() + radius * cos(iconPosAngle6)
        val iconPosY6 = (h / 2).toFloat() + radius * sin(iconPosAngle6)

        val iconPosAngle7 = 144.00 * (Math.PI / 180F)
        val iconPosX7 = (w / 2).toFloat() + radius * cos(iconPosAngle7)
        val iconPosY7 = (h / 2).toFloat() + radius * sin(iconPosAngle7)

        val iconPosAngle8 = 180.00 * (Math.PI / 180F)
        val iconPosX8 = (w / 2).toFloat() + radius * cos(iconPosAngle8)
        val iconPosY8 = (h / 2).toFloat() + radius * sin(iconPosAngle8)

        val iconPosAngle9 = 216.00 * (Math.PI / 180F)
        val iconPosX9 = (w / 2).toFloat() + radius * cos(iconPosAngle9)
        val iconPosY9 = (h / 2).toFloat() + radius * sin(iconPosAngle9)

        val iconPosAngle10 = 252.00 * (Math.PI / 180F)
        val iconPosX10 = (w / 2).toFloat() + radius * cos(iconPosAngle10)
        val iconPosY10 = (h / 2).toFloat() + radius * sin(iconPosAngle10)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        val px6 = w / 2 + (((iconPosX6 - w / 2) / d) * w / 3)
        val py6 = h / 2 + (((iconPosY6 - h / 2) / d) * w / 3)

        val px7 = w / 2 + (((iconPosX7 - w / 2) / d) * w / 3)
        val py7 = h / 2 + (((iconPosY7 - h / 2) / d) * w / 3)

        val px8 = w / 2 + (((iconPosX8 - w / 2) / d) * w / 3)
        val py8 = h / 2 + (((iconPosY8 - h / 2) / d) * w / 3)

        val px9 = w / 2 + (((iconPosX9 - w / 2) / d) * w / 3)
        val py9 = h / 2 + (((iconPosY9 - h / 2) / d) * w / 3)

        val px10 = w / 2 + (((iconPosX10 - w / 2) / d) * w / 3)
        val py10 = h / 2 + (((iconPosY10 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )

        conSet.connect(
            buttons[5].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px6.toInt()
        )

        conSet.connect(
            buttons[6].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px7.toInt()
        )

        conSet.connect(
            buttons[7].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px8.toInt()
        )

        conSet.connect(
            buttons[8].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px9.toInt()
        )

        conSet.connect(
            buttons[9].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px10.toInt()
        )
        conSet.connect(
            buttons[9].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py10.toInt()
        )
        conSet.connect(
            buttons[9].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py10.toInt()
        )
        conSet.connect(
            buttons[9].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px10.toInt()
        )
    }

    private fun forCircular9(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 290.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 330.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 10.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 50.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 90.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val iconPosAngle6 = 130.00 * (Math.PI / 180F)
        val iconPosX6 = (w / 2).toFloat() + radius * cos(iconPosAngle6)
        val iconPosY6 = (h / 2).toFloat() + radius * sin(iconPosAngle6)

        val iconPosAngle7 = 170.00 * (Math.PI / 180F)
        val iconPosX7 = (w / 2).toFloat() + radius * cos(iconPosAngle7)
        val iconPosY7 = (h / 2).toFloat() + radius * sin(iconPosAngle7)

        val iconPosAngle8 = 210.00 * (Math.PI / 180F)
        val iconPosX8 = (w / 2).toFloat() + radius * cos(iconPosAngle8)
        val iconPosY8 = (h / 2).toFloat() + radius * sin(iconPosAngle8)

        val iconPosAngle9 = 250.00 * (Math.PI / 180F)
        val iconPosX9 = (w / 2).toFloat() + radius * cos(iconPosAngle9)
        val iconPosY9 = (h / 2).toFloat() + radius * sin(iconPosAngle9)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        val px6 = w / 2 + (((iconPosX6 - w / 2) / d) * w / 3)
        val py6 = h / 2 + (((iconPosY6 - h / 2) / d) * w / 3)

        val px7 = w / 2 + (((iconPosX7 - w / 2) / d) * w / 3)
        val py7 = h / 2 + (((iconPosY7 - h / 2) / d) * w / 3)

        val px8 = w / 2 + (((iconPosX8 - w / 2) / d) * w / 3)
        val py8 = h / 2 + (((iconPosY8 - h / 2) / d) * w / 3)

        val px9 = w / 2 + (((iconPosX9 - w / 2) / d) * w / 3)
        val py9 = h / 2 + (((iconPosY9 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )

        conSet.connect(
            buttons[5].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px6.toInt()
        )

        conSet.connect(
            buttons[6].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px7.toInt()
        )

        conSet.connect(
            buttons[7].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px8.toInt()
        )

        conSet.connect(
            buttons[8].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py9.toInt()
        )
        conSet.connect(
            buttons[8].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px9.toInt()
        )
    }

    private fun forCircular8(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 292.50 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 337.50 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 22.50 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 67.50 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 112.50 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val iconPosAngle6 = 157.50 * (Math.PI / 180F)
        val iconPosX6 = (w / 2).toFloat() + radius * cos(iconPosAngle6)
        val iconPosY6 = (h / 2).toFloat() + radius * sin(iconPosAngle6)

        val iconPosAngle7 = 202.50 * (Math.PI / 180F)
        val iconPosX7 = (w / 2).toFloat() + radius * cos(iconPosAngle7)
        val iconPosY7 = (h / 2).toFloat() + radius * sin(iconPosAngle7)

        val iconPosAngle8 = 247.50 * (Math.PI / 180F)
        val iconPosX8 = (w / 2).toFloat() + radius * cos(iconPosAngle8)
        val iconPosY8 = (h / 2).toFloat() + radius * sin(iconPosAngle8)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        val px6 = w / 2 + (((iconPosX6 - w / 2) / d) * w / 3)
        val py6 = h / 2 + (((iconPosY6 - h / 2) / d) * w / 3)

        val px7 = w / 2 + (((iconPosX7 - w / 2) / d) * w / 3)
        val py7 = h / 2 + (((iconPosY7 - h / 2) / d) * w / 3)

        val px8 = w / 2 + (((iconPosX8 - w / 2) / d) * w / 3)
        val py8 = h / 2 + (((iconPosY8 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )

        conSet.connect(
            buttons[5].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px6.toInt()
        )

        conSet.connect(
            buttons[6].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px7.toInt()
        )

        conSet.connect(
            buttons[7].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py8.toInt()
        )
        conSet.connect(
            buttons[7].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px8.toInt()
        )
    }

    private fun forCircular7(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 295.70 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 347.10 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 38.50 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 89.90 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 141.30 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val iconPosAngle6 = 192.70 * (Math.PI / 180F)
        val iconPosX6 = (w / 2).toFloat() + radius * cos(iconPosAngle6)
        val iconPosY6 = (h / 2).toFloat() + radius * sin(iconPosAngle6)

        val iconPosAngle7 = 244.10 * (Math.PI / 180F)
        val iconPosX7 = (w / 2).toFloat() + radius * cos(iconPosAngle7)
        val iconPosY7 = (h / 2).toFloat() + radius * sin(iconPosAngle7)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        val px6 = w / 2 + (((iconPosX6 - w / 2) / d) * w / 3)
        val py6 = h / 2 + (((iconPosY6 - h / 2) / d) * w / 3)

        val px7 = w / 2 + (((iconPosX7 - w / 2) / d) * w / 3)
        val py7 = h / 2 + (((iconPosY7 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )

        conSet.connect(
            buttons[5].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px6.toInt()
        )

        conSet.connect(
            buttons[6].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py7.toInt()
        )
        conSet.connect(
            buttons[6].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px7.toInt()
        )
    }

    private fun forCircular6(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 300.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 360.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 60.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 120.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 180.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val iconPosAngle6 = 240.00 * (Math.PI / 180F)
        val iconPosX6 = (w / 2).toFloat() + radius * cos(iconPosAngle6)
        val iconPosY6 = (h / 2).toFloat() + radius * sin(iconPosAngle6)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        val px6 = w / 2 + (((iconPosX6 - w / 2) / d) * w / 3)
        val py6 = h / 2 + (((iconPosY6 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )

        conSet.connect(
            buttons[5].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py6.toInt()
        )
        conSet.connect(
            buttons[5].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px6.toInt()
        )
    }

    private fun forCircular5(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 306.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 18.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 90.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 162.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val iconPosAngle5 = 234.00 * (Math.PI / 180F)
        val iconPosX5 = (w / 2).toFloat() + radius * cos(iconPosAngle5)
        val iconPosY5 = (h / 2).toFloat() + radius * sin(iconPosAngle5)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        val px5 = w / 2 + (((iconPosX5 - w / 2) / d) * w / 3)
        val py5 = h / 2 + (((iconPosY5 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )

        conSet.connect(
            buttons[4].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py5.toInt()
        )
        conSet.connect(
            buttons[4].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px5.toInt()
        )
    }

    private fun forCircular4(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 315.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 45.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 135.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val iconPosAngle4 = 225.00 * (Math.PI / 180F)
        val iconPosX4 = (w / 2).toFloat() + radius * cos(iconPosAngle4)
        val iconPosY4 = (h / 2).toFloat() + radius * sin(iconPosAngle4)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        val px4 = w / 2 + (((iconPosX4 - w / 2) / d) * w / 3)
        val py4 = h / 2 + (((iconPosY4 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )

        conSet.connect(
            buttons[3].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py4.toInt()
        )
        conSet.connect(
            buttons[3].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px4.toInt()
        )
    }

    private fun forCircular3(w: Int, h: Int, conSet: ConstraintSet) {
        // drawing lines to mark icon positions
        val radius = w / 2

        val iconPosAngle1 = 330.00 * (Math.PI / 180F)
        val iconPosX1 = (w / 2).toFloat() + radius * cos(iconPosAngle1)
        val iconPosY1 = (h / 2).toFloat() + radius * sin(iconPosAngle1)

        val iconPosAngle2 = 90.00 * (Math.PI / 180F)
        val iconPosX2 = (w / 2).toFloat() + radius * cos(iconPosAngle2)
        val iconPosY2 = (h / 2).toFloat() + radius * sin(iconPosAngle2)

        val iconPosAngle3 = 210.00 * (Math.PI / 180F)
        val iconPosX3 = (w / 2).toFloat() + radius * cos(iconPosAngle3)
        val iconPosY3 = (h / 2).toFloat() + radius * sin(iconPosAngle3)

        val d = sqrt((iconPosX1 - w / 2).pow(2) + (iconPosY1 - h / 2).pow(2))

        // Icon positions
        val px1 = w / 2 + (((iconPosX1 - w / 2) / d) * w / 3)
        val py1 = h / 2 + (((iconPosY1 - h / 2) / d) * w / 3)

        val px2 = w / 2 + (((iconPosX2 - w / 2) / d) * w / 3)
        val py2 = h / 2 + (((iconPosY2 - h / 2) / d) * w / 3)

        val px3 = w / 2 + (((iconPosX3 - w / 2) / d) * w / 3)
        val py3 = h / 2 + (((iconPosY3 - h / 2) / d) * w / 3)

        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py1.toInt()
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px1.toInt()
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py2.toInt()
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px2.toInt()
        )

        conSet.connect(
            buttons[2].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            px3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            h - py3.toInt()
        )
        conSet.connect(
            buttons[2].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - px3.toInt()
        )
    }

    private fun forCircular2(w: Int, h: Int, conSet: ConstraintSet) {
        conSet.connect(
            buttons[0].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            w / 5
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            w / 2
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            w / 2
        )
        conSet.connect(
            buttons[0].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w - w / 5
        )

        conSet.connect(
            buttons[1].id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            w - w / 5
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            w / 2
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            w / 2
        )
        conSet.connect(
            buttons[1].id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            w / 5
        )
    }

}