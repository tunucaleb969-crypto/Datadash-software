package com.kwame.datadash.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A FrameLayout that caps its own measured width so content doesn't
 * stretch edge-to-edge on large screens (tablets, Chromebooks, or
 * Android running on a laptop). Used only in layout-sw600dp/ variants —
 * phone layouts are untouched. Pair with android:layout_gravity=
 * "center_horizontal" on this view so the parent centers the capped
 * column.
 */
class MaxWidthFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val maxWidthPx: Int = (MAX_WIDTH_DP * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val cappedWidth = minOf(originalWidth, maxWidthPx)
        val newWidthSpec = MeasureSpec.makeMeasureSpec(cappedWidth, MeasureSpec.getMode(widthMeasureSpec))
        super.onMeasure(newWidthSpec, heightMeasureSpec)
    }

    companion object {
        private const val MAX_WIDTH_DP = 640
    }
}
