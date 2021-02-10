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
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

// Adopted from https://stackoverflow.com/questions/55068560/allow-bottomsheet-to-slide-up-after-threshold-is-reached-on-an-area-outside
// But we need to do this on a view in separate fragment

class BottomNavigationViewCust : BottomNavigationView {
    private var passThroughTarget: View? = null

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    private var initialY = 0f
    private var moving = false

    constructor(context: Context) : super(context)
    constructor(
            context: Context,
            attrs: AttributeSet?
    ) : super(context, attrs)

    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                initialY = abs(event.rawY)
                return super.onInterceptTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moving && initialY - abs(event.rawY) > touchSlop) {
                    moving = true
                    val downTime = SystemClock.uptimeMillis() - 100
                    val eventTime = SystemClock.uptimeMillis()
                    val x = event.rawX
                    val y = event.rawY
                    val metaState = 0
                    //create new motion event

                    val motionEvent = MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_DOWN,
                            x,
                            y,
                            metaState
                    )
                    passThroughTarget?.apply {
                        onTouchEvent(motionEvent) // sent event to proxyView
                    }
                }
                if (moving) {
                    passThroughTarget?.apply {
                        onTouchEvent(event) // sent event to proxyView
                    }
                }

                return super.onInterceptTouchEvent(event)


            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                initialY = 0f
                if (moving) {
                    passThroughTarget?.apply {
                        onTouchEvent(event) // sent event to proxyView
                    }
                    moving = false
                }
                return super.onInterceptTouchEvent(event)
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    fun setTouchTarget(targetView: View?) {
        if (moving && this.passThroughTarget != null) {
            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis() + 100
            val motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_CANCEL,
                    x,
                    y,
                    0
            )
            passThroughTarget?.apply {
                dispatchTouchEvent(motionEvent) // sent event to proxyView
            }
        }
        this.passThroughTarget = targetView
        moving = false
    }

}