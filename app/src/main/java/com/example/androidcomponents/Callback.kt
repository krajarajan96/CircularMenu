package com.example.androidcomponents

import android.graphics.Color
import android.widget.ImageView

interface Callback {

    fun getButtons(): List<ImageView>

    fun onItemClick(id: Int)

    fun onRotationFinished(id: Int)

    fun panelColor(): Int {
        return Color.parseColor("#3848A5")
    }

    fun separatorColor(): Int {
        return Color.BLACK
    }

    fun buttonColor(): Int {
        return Color.parseColor("#3848A5")
    }

    fun closeIconColor(): Int {
        return Color.WHITE
    }

    fun closeOnTouchOutside(): Boolean {
        return true
    }

    fun shouldVibrateOnSelection(): Boolean {
        return false
    }

    fun getMode(): Mode {
        return Mode.CIRCULAR
    }

    fun getAnimationType(): AnimationType {
        return AnimationType.FROM_CENTER
    }

    fun isSkeletonEnabled(): Boolean {
        return false
    }

    fun isSeparatorsVisible(): Boolean {
        return false
    }

    fun isCloseButtonEnabled(): Boolean {
        return true
    }

    fun isRotatable(): Boolean {
        return true
    }

    fun onError(exception: Exception)

}

enum class Mode {
    RADIAL, CIRCULAR
}

enum class AnimationType {
    NONE,
    FROM_LEFT,
    FROM_CENTER,
    FROM_RIGHT,
    DISPERSE,
    DISPERSE_INDIVIDUAL,
    FROM_LEFT_AND_DISPERSE,
    FROM_RIGHT_AND_DISPERSE,
    FROM_CENTER_AND_DISPERSE,
}