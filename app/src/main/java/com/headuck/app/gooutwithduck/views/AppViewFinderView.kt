/*
 * Copyright 2021 headuck (https://blog.headuck.com/)
 *
 * This file is part of GoOutWithDuck
 *
 * GoOutWithDuck is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GoOutWithDuck is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GoOutWithDuck. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.headuck.app.gooutwithduck.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.headuck.app.gooutwithduck.R
import com.journeyapps.barcodescanner.ViewfinderView


/**
 * Finder view to add frame to the scan page
 */
class AppViewFinderView : ViewfinderView {
    var frameColor: Int = 0

    constructor(context: Context,
                attrs: AttributeSet? = null) : super(context, attrs) {
        val resources = resources
        maskColor = ResourcesCompat.getColor(resources, R.color.app_color_transparent_grey, context.theme)
        frameColor = getPrimaryColor(context)
    }

    private fun getPrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    override fun onDraw(canvas: Canvas) {
        refreshSizes()
        if (framingRect == null || previewSize == null) {
            return
        }

        val frame = framingRect
        val previewSize = previewSize

        val width = width
        val height = height

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)

        // Draw the frame
        paint.color = frameColor
        val fwidth = 24.0f
        val fsize = 120.0f
        canvas.drawRect(frame.left.toFloat() - fwidth, frame.top.toFloat() - fwidth, frame.left.toFloat() + fsize, frame.top.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat() - fwidth, frame.top.toFloat(), frame.left.toFloat(), frame.top.toFloat() + fsize, paint)
        canvas.drawRect((frame.right + 1).toFloat() + fwidth - fsize, frame.top.toFloat() - fwidth, (frame.right + 1).toFloat() + fwidth, frame.top.toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), (frame.right + 1).toFloat() + fwidth, frame.top.toFloat() + fsize, paint)

        canvas.drawRect(frame.left.toFloat() - fwidth, (frame.bottom + 1).toFloat(), frame.left.toFloat() + fsize, (frame.bottom + 1).toFloat() + fwidth, paint)
        canvas.drawRect(frame.left.toFloat() - fwidth, (frame.bottom + 1).toFloat() - fsize, frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat() + fwidth - fsize, (frame.bottom + 1).toFloat(), (frame.right + 1).toFloat() + fwidth, (frame.bottom + 1).toFloat() + fwidth, paint)
        canvas.drawRect((frame.right + 1).toFloat(), (frame.bottom + 1).toFloat() - fsize, (frame.right + 1).toFloat() + fwidth, (frame.bottom + 1).toFloat(), paint)

    }


}
