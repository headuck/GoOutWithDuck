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

import android.app.Application
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Result
import com.headuck.app.gooutwithduck.R
import com.headuck.app.gooutwithduck.ScanDoneFragment
import com.headuck.app.gooutwithduck.usecases.AutoCheckoutUseCase
import com.headuck.app.gooutwithduck.usecases.LeaveVenueUseCase
import kotlinx.coroutines.launch
import java.util.Calendar


/**
 * The ViewModel for [ScanDoneFragment].
 */
class ScanDoneViewModel @ViewModelInject internal constructor(
        applicationCtx: Application,
        private val leaveVenueUseCase: LeaveVenueUseCase,
        private val autoCheckoutUseCase: AutoCheckoutUseCase,
) : AndroidViewModel(applicationCtx) {

    private val durationArray = getApplication<Application>().resources.getIntArray(R.array.scan_done_auto_leave_duration_int)

    fun leaveVenueNow(visitHistoryId: Int): LiveData<Result<Boolean, Exception>> = liveData {
        emit(leaveVenueUseCase.leaveVenue(visitHistoryId, Calendar.getInstance()))
    }

    val autoCheckoutSetting: LiveData<Pair<Boolean, Int>> =
            autoCheckoutUseCase.autoCheckoutSetting().asLiveData()

    fun setAutoCheckout(visitHistoryId: Int, isOn: Boolean, durationPos: Int, savePref: Boolean) {
        viewModelScope.launch {
            if (savePref) {
                autoCheckoutUseCase.saveAutoCheckoutSetting(isOn, durationPos)
            }
            autoCheckoutUseCase.setAutoCheckoutForVisitHistory(visitHistoryId, isOn, posToSeconds(durationPos))
        }
    }

    private fun posToSeconds (pos: Int): Int {
        return if (pos >= 0 && pos < durationArray.size) {
            durationArray[pos]
        } else {
            3600
        }
    }
}
