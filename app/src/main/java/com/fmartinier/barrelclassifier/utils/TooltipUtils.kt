package com.fmartinier.barrelclassifier.utils

import android.content.Context
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec

class TooltipUtils {

    companion object {
        fun createTooltip(context: Context, text: String): Balloon {
            return Balloon.Builder(context)
                .setText(text)
                .setArrowSize(10)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setTextSize(15f)
                .setCornerRadius(8f)
                .setAlpha(0.9f)
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setLifecycleOwner(null)
                .setPadding(5)
                .build()
        }
    }
}