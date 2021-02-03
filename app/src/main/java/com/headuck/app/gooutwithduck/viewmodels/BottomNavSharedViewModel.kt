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

package com.headuck.app.gooutwithduck.viewmodels

import android.view.View
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.headuck.app.gooutwithduck.ScanCodeFragment
import timber.log.Timber

/**
 * The ViewModel for [ScanCodeFragment].
 */
class BottomNavSharedViewModel @ViewModelInject constructor(
        @Assisted private val savedStateHandle: SavedStateHandle
    ) : ViewModel() {

    val targetView = MutableLiveData<View?>()
    val hidden = MutableLiveData<Boolean?>().apply {
        value = savedStateHandle.get("navHidden")
    }

    fun setBottomNavTargetView(target: View?) {
        targetView.value = target
    }

    fun setBottomNavHidden(hide: Boolean) {
        hidden.value = hide
        savedStateHandle["navHidden"] = hide
    }
}
