package com.fmartinier.barrelclassifier.utils

import android.animation.ValueAnimator
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import com.fmartinier.barrelclassifier.R

class TextViewUtils {
    
    companion object {
        fun convertToDetailedDescription(context: Context, textView: TextView, expand: TextView, text: String?) {
            if (text.isNullOrBlank()) {
                textView.visibility = View.GONE
                expand.visibility = View.GONE
            } else {
                textView.text = text
                textView.visibility = View.INVISIBLE
                expand.visibility = View.GONE

                textView.maxLines = Integer.MAX_VALUE
                textView.ellipsize = null
            }

            textView.afterMeasured {

                val lineCount = textView.lineCount

                if (lineCount <= 1) {
                    expand.visibility = View.GONE
                } else {
                    expand.visibility = View.VISIBLE
                    expand.text = textView.context.getString(R.string.see_more)
                    textView.maxLines = 1
                    textView.ellipsize = TextUtils.TruncateAt.END
                }

                textView.visibility = View.VISIBLE
            }

            expand.setOnClickListener {

                val expanded = textView.maxLines > 1

                if (expanded) {
                    // collapse
                    val startHeight = textView.height

                    textView.maxLines = 1
                    textView.ellipsize = TextUtils.TruncateAt.END
                    textView.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            textView.width,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.UNSPECIFIED
                    )
                    val endHeight = textView.measuredHeight

                    animateTextViewHeight(textView, startHeight, endHeight)

                    expand.text = context.getString(R.string.see_more)

                } else {
                    // expand
                    val startHeight = textView.height

                    textView.maxLines = Int.MAX_VALUE
                    textView.ellipsize = null
                    textView.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            textView.width,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.UNSPECIFIED
                    )
                    val endHeight = textView.measuredHeight

                    animateTextViewHeight(textView, startHeight, endHeight)

                    expand.text = context.getString(R.string.see_less)
                }
            }
        }

        private fun TextView.afterMeasured(block: () -> Unit) {
            if (width > 0) {
                block()
                return
            }

            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (width > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }

        private fun animateTextViewHeight(view: TextView, start: Int, end: Int, duration: Long = 220) {
            val animator = ValueAnimator.ofInt(start, end)
            animator.duration = duration
            animator.interpolator = android.view.animation.DecelerateInterpolator()

            animator.addUpdateListener {
                val value = it.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }

            animator.start()
        }
    }
}