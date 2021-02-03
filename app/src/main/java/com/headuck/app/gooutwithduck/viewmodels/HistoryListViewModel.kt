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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.headuck.app.gooutwithduck.HistoryListFragment
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.usecases.GetHistoryUseCase
import com.headuck.app.gooutwithduck.utilities.NO_HISTORY_TYPE
import com.headuck.app.gooutwithduck.workers.GetBatchesWorker
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.*

/**
 * The ViewModel for [HistoryListFragment].
 */
class HistoryListViewModel @ViewModelInject internal constructor(
        applicationCtx: Application,
        private val getHistoryUseCase: GetHistoryUseCase,
        @Assisted private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(applicationCtx) {
    private var currentSearchResult: Flow<PagingData<VisitHistory>>? = null
    private val workManager = WorkManager.getInstance(applicationCtx)

    fun getVisitHistoryList(): Flow<PagingData<VisitHistory>> {
        val lastResult = currentSearchResult
        if (lastResult != null) {
            return lastResult
        }
        val filter = getHistoryTypeFilter()

        val newResult = getHistoryUseCase.getHistoryList(filter)
            .cachedIn(viewModelScope)

        currentSearchResult = newResult
        return newResult
    }

    fun setHistoryTypeFilter(type: String) {
        savedStateHandle.set(HISTORY_TYPE_SAVED_STATE_KEY, type)
        currentSearchResult = null
    }

    fun clearHistoryTypeFilter() {
        savedStateHandle.set(HISTORY_TYPE_SAVED_STATE_KEY, NO_HISTORY_TYPE)
        currentSearchResult = null
    }

    fun getBookmarkCountByTypeAndVenueId(type: String, venueId: String): Flow<Int> =
        getHistoryUseCase.getBookmarkCountByTypeAndVenueId(type, venueId)

    suspend fun createBookmark(venueInfo: VenueInfo) =
        getHistoryUseCase.createBookmark(venueInfo)

    suspend fun deleteBookmark(venueInfo: VenueInfo) =
        getHistoryUseCase.deleteBookmark(venueInfo)

    fun isFiltered() = getHistoryTypeFilter() != NO_HISTORY_TYPE

    internal fun download(): UUID {
        // Add WorkRequest to Cleanup temporary images
        val request = OneTimeWorkRequest.from(GetBatchesWorker::class.java)
        var continuation = workManager
                .beginUniqueWork(
                        DOWNLOAD_WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        request
                )
        // Actually start the work
        continuation.enqueue()

        return request.id
    }

    private fun getHistoryTypeFilter(): String {
        return savedStateHandle.get(HISTORY_TYPE_SAVED_STATE_KEY) ?: NO_HISTORY_TYPE
    }

    companion object {
        private const val HISTORY_TYPE_SAVED_STATE_KEY = "HISTORY_TYPE_SAVED_STATE_KEY"
        private const val DOWNLOAD_WORK_NAME = "DOWNLOAD_WORK"
    }
}
