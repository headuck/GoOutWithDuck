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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.usecases.EditHistoryUseCase
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.launch
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import java.lang.IllegalArgumentException
import java.util.Calendar

class VenueDetailViewModel @AssistedInject constructor(
        private val editHistoryUseCase: EditHistoryUseCase,
        @Assisted private val visitHistoryId: Int
) : ViewModel() {

    var lastDialog = NONE

    fun getVenue() : LiveData<Result<VisitHistory, Exception>> =
        editHistoryUseCase.getVenueById(visitHistoryId).mapLatest {
            // Map bookmark to error, otherwise pass through
            it.component1()?.let { visitHistory ->
                when (visitHistory.bookmark) {
                    true -> Err(IllegalArgumentException("Unexpected state: venue is a bookmark"))
                    false -> it
                }
            } ?: it
        }.asLiveData()

    suspend fun deleteVenue(visitHistoryId: Int, autoEndDate: Calendar?) {
        editHistoryUseCase.deleteVenue(visitHistoryId, autoEndDate != null)
    }

    fun updateVenue(visitHistory: VisitHistory) {
        viewModelScope.launch {
            editHistoryUseCase.updateVenue(visitHistory)
        }
    }

    /**
     * Handles change time request for Visit History from dialog
     * @param visitHistory VisitHistory
     * @param hour hour value
     * @param minute minute value
     */
    suspend fun changeTime(visitHistory: VisitHistory, hour: Int, minute: Int) {
        if (lastDialog == NONE) return
        val lastDialogIsEntry = lastDialog == ENTRY_TIME
        val visitHistoryResult = editHistoryUseCase.getVenueById(visitHistory.id).firstOrNull()
        visitHistoryResult?.onSuccess { visitHistory ->
            if (lastDialogIsEntry) {
                val newVal = setDateTimeVal(visitHistory.scanDate, null, visitHistory.endDate, hour, minute)
                editHistoryUseCase.updateVenueTime(visitHistory.id, newVal, true)
            } else {
                visitHistory.endDate?.apply {
                    val newVal = setDateTimeVal(this, visitHistory.startDate, null, hour, minute)
                    editHistoryUseCase.updateVenueTime(visitHistory.id, newVal, false)
                }
            }
        }
    }

    /**
     * Return Calendar from hour and minute, given the preferred center of range, and lower / upper bound
     * @param center Desired center of range
     * @param earliest Earliest time
     * @param latest Latest time
     * @param hour Hour value (0-23)
     * @param minute Minute value (0-59)
     * @return Time in Calendar format
     */
    private fun setDateTimeVal(center: Calendar, earliest: Calendar?, latest: Calendar?, hour: Int, minute: Int) : Calendar {
        // Roll back 12 hours from center
        var centerHour = center.get(Calendar.HOUR_OF_DAY) - 12
        if (centerHour < 0) centerHour += 24
        val centerMin = center.get(Calendar.MINUTE)
        var offsetHour = hour - centerHour

        // Calculate offset of provided hh:mm from soft earliest bound
        var offsetMin = minute - centerMin
        if (offsetMin < 0) {
            offsetMin += 60
            offsetHour--
        }
        if (offsetHour < 0) offsetHour += 24

        // Create new Calendar from the offset
        val newVal = Calendar.getInstance().apply {
            timeInMillis = center.timeInMillis
            timeZone = center.timeZone
            add(Calendar.HOUR_OF_DAY, offsetHour - 12)
            add(Calendar.MINUTE, offsetMin)
        }
        // Adjust if breached earliest / latest
        earliest?.apply {
            if (newVal < this) {
                newVal.add(Calendar.DATE, 1)
            }
        }
        latest?.apply {
            if (newVal > latest) {
                newVal.add(Calendar.DATE, -1)
            }
        }
        return newVal
    }

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(visitHistoryId: Int): VenueDetailViewModel
    }

    companion object {
        fun provideFactory(
                assistedFactory: AssistedFactory,
                visitHistoryId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(visitHistoryId) as T
            }
        }
        // Last dialog
        const val NONE = 0
        const val ENTRY_TIME = 1
        const val EXIT_TIME = 2
    }

}