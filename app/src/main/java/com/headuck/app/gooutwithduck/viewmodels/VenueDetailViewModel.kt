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
import kotlinx.coroutines.flow.mapLatest
import java.lang.IllegalArgumentException

class VenueDetailViewModel @AssistedInject constructor(
        private val editHistoryUseCase: EditHistoryUseCase,
        @Assisted private val visitHistoryId: Int
) : ViewModel() {

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

    suspend fun deleteVenue(visitHistoryId: Int) {
        editHistoryUseCase.deleteVenue(visitHistoryId)
    }

    fun updateVenue(visitHistory: VisitHistory) {
        viewModelScope.launch {
            editHistoryUseCase.updateVenue(visitHistory)
        }
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
    }

}