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
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.headuck.app.gooutwithduck.InboxFragment
import com.headuck.app.gooutwithduck.data.Inbox
import com.headuck.app.gooutwithduck.usecases.InboxUseCase
import com.headuck.app.gooutwithduck.utilities.NO_INBOX_FILTER

import kotlinx.coroutines.flow.Flow


/**
 * The ViewModel for [InboxFragment].
 */
class InboxViewModel @ViewModelInject internal constructor(
        applicationCtx: Application,
        private val inboxUseCase: InboxUseCase,
        @Assisted private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(applicationCtx) {
    private var currentSearchResult: Flow<PagingData<Inbox>>? = null

    /**
     * Get downloaded cases list paging data for display
     */
    fun getInboxList(): Flow<PagingData<Inbox>> {
        val lastResult = currentSearchResult
        if (lastResult != null) {
            return lastResult
        }
        val filter = getInboxTypeFilter()

        val newResult = inboxUseCase.getInboxList()
            .cachedIn(viewModelScope)

        currentSearchResult = newResult
        return newResult
    }

    fun setInboxFilter(type: String) {
        savedStateHandle.set(INBOX_FILTER_SAVED_STATE_KEY, type)
        currentSearchResult = null
    }

    fun clearInboxFilter() {
        savedStateHandle.set(INBOX_FILTER_SAVED_STATE_KEY, NO_INBOX_FILTER)
        currentSearchResult = null
    }

    fun isFiltered() = getInboxTypeFilter() != NO_INBOX_FILTER

    private fun getInboxTypeFilter(): String {
        return savedStateHandle.get(INBOX_FILTER_SAVED_STATE_KEY) ?: NO_INBOX_FILTER
    }


    companion object {
        private const val INBOX_FILTER_SAVED_STATE_KEY = "INBOX_FILTER_SAVED_STATE_KEY"
    }
}
