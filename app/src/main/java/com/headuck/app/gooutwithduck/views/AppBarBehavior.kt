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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

/**
 * AppBarBehavior - prevent bottom sheet from interfering with appbar scrolling
 * https://stackoverflow.com/questions/49105946/prevent-scrolls-in-bottom-sheet-ancestor-view
 */
class AppBarBehavior @JvmOverloads constructor(context: Context?, attrs: AttributeSet?):
        AppBarLayout.Behavior(context, attrs)  {

    var mIsSheetTouched: Boolean
    var bottomSheetId: Int?

    init {
        mIsSheetTouched = false
        bottomSheetId = null
    }

    override fun onStartNestedScroll(parent: CoordinatorLayout, child: AppBarLayout, directTargetChild: View, target: View, nestedScrollAxes: Int, type: Int): Boolean {
        // Set flag if the bottom sheet is responsible for the nested scroll.
        mIsSheetTouched = bottomSheetId?.let{ target.id == it } ?: false
        // Only consider starting a nested scroll if the bottom sheet is not touched; otherwise,
        // we will let the other views do the scrolling.
        return (!mIsSheetTouched
                && super.onStartNestedScroll(parent, child, directTargetChild,
                target, nestedScrollAxes, type))
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: AppBarLayout, ev: MotionEvent): Boolean {
        // Don't accept touch stream here if the bottom sheet is touched. This will permit the
        // bottom sheet to be dragged down without interaction with the appBar. Reset on cancel.
        if (ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            mIsSheetTouched = false
        }
        return !mIsSheetTouched && super.onInterceptTouchEvent(parent, child, ev)
    }

    companion object {
        // Adopted from BottomSheet behavior
        fun <V : View?> from(view: V): AppBarBehavior {
            val params = view!!.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "The view is not a child of CoordinatorLayout" }
            val behavior = params.behavior
            require(behavior is AppBarBehavior) { "The view is not associated with BottomSheetBehavior" }
            return behavior
        }
    }
}