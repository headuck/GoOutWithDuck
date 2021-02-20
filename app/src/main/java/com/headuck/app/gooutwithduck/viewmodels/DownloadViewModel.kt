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
import com.headuck.app.gooutwithduck.DownloadFragment
import com.headuck.app.gooutwithduck.data.DownloadCase
import com.headuck.app.gooutwithduck.usecases.DownloadUseCase
import com.headuck.app.gooutwithduck.utilities.NO_DOWNLOAD_TYPE

import com.headuck.app.gooutwithduck.workers.GetBatchesWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The ViewModel for [DownloadFragment].
 */
class DownloadViewModel @ViewModelInject internal constructor(
        applicationCtx: Application,
        private val downloadUseCase: DownloadUseCase,
        @Assisted private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(applicationCtx) {
    private var currentSearchResult: Flow<PagingData<DownloadCase>>? = null
    private val workManager = WorkManager.getInstance(applicationCtx)

    /**
     * Get downloaded cases list paging data for display
     */
    fun getDownloadList(): Flow<PagingData<DownloadCase>> {
        val lastResult = currentSearchResult
        if (lastResult != null) {
            return lastResult
        }
        val filter = getDownloadTypeFilter()

        val newResult = downloadUseCase.getDownloadList(filter)
            .cachedIn(viewModelScope)

        currentSearchResult = newResult
        return newResult
    }

    fun setDownloadTypeFilter(type: String) {
        savedStateHandle.set(DOWNLOAD_TYPE_SAVED_STATE_KEY, type)
        currentSearchResult = null
    }

    fun clearDownloadTypeFilter() {
        savedStateHandle.set(DOWNLOAD_TYPE_SAVED_STATE_KEY, NO_DOWNLOAD_TYPE)
        currentSearchResult = null
    }

    fun isFiltered() = getDownloadTypeFilter() != NO_DOWNLOAD_TYPE

    private fun getDownloadTypeFilter(): String {
        return savedStateHandle.get(DOWNLOAD_TYPE_SAVED_STATE_KEY) ?: NO_DOWNLOAD_TYPE
    }

    /**
     * Trigger download and verification of new cases
     * @return request id of [OneTimeWorkRequest]
     */
    internal fun download(): UUID {
        // Add WorkRequest to Cleanup temporary images
        val request = OneTimeWorkRequest.from(GetBatchesWorker::class.java)
        val continuation = workManager
                .beginUniqueWork(
                        DOWNLOAD_WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        request
                )
        // Actually start the work
        continuation.enqueue()

        return request.id
    }

    companion object {
        private const val DOWNLOAD_TYPE_SAVED_STATE_KEY = "DOWNLOAD_TYPE_SAVED_STATE_KEY"
        private const val DOWNLOAD_WORK_NAME = "DOWNLOAD_WORK"
    }
}
