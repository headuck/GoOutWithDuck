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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.headuck.app.gooutwithduck.MainFragment
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.usecases.BookmarkUseCase
import com.headuck.app.gooutwithduck.usecases.EditHistoryUseCase
import com.headuck.app.gooutwithduck.usecases.GetHistoryUseCase
import com.headuck.app.gooutwithduck.usecases.InboxUseCase
import com.headuck.app.gooutwithduck.usecases.LeaveVenueUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * The ViewModel for [MainFragment].
 */
class MainViewModel @ViewModelInject internal constructor(
        applicationCtx: Application,
        private val getHistoryUseCase: GetHistoryUseCase,
        private val editHistoryUseCase: EditHistoryUseCase,
        private val leaveVenueUseCase: LeaveVenueUseCase,
        private val bookmarkUseCase: BookmarkUseCase,
        private val inboxUseCase: InboxUseCase,
        @Assisted private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(applicationCtx) {

    private val currentCheckInList = MutableStateFlow(emptyList<VisitHistory>())

    // The UI collects from liveData of this StateFlow to get updates
    val checkInList: LiveData<List<VisitHistory>> = currentCheckInList.asLiveData()
    val isEmpty: LiveData<Boolean> = currentCheckInList
            .mapLatest {
                it.isEmpty()
            }
            .distinctUntilChanged()
            .asLiveData()

    init {
        viewModelScope.launch {
            getHistoryUseCase.getCheckInHistoryList().collect {
                currentCheckInList.value = it
            }
        }
    }

    val bookmarkPagingData: Flow<PagingData<VisitHistory>> = getHistoryUseCase.getBookmarkList()

    val lastUpdate: Flow<Calendar?> =
        inboxUseCase.getLastUpdate().map {
           it.getOrElse { e ->
               Timber.e(e, "Error getting last update time")
               null
           }
        }


    val unreadCount: Flow<Int> =
        inboxUseCase.getInboxUnreadCount().map {
            it.getOrElse { e ->
                Timber.e(e, "Error getting inbox unread count")
                0
            }
        }


   fun leaveVenueNow(visitHistoryId: Int): LiveData<Result<Boolean, Exception>> = liveData {
        emit(leaveVenueUseCase.leaveVenue(visitHistoryId, Calendar.getInstance()))
    }

    fun enterBookmarkVenue(visitHistoryId: Int): LiveData<Result<VisitHistory, String>> =
        bookmarkUseCase.enterBookmarkVenue(visitHistoryId).asLiveData()

    suspend fun pinBookmark(visitHistoryId: Int): Boolean =
            bookmarkUseCase.pinBookmark(visitHistoryId).getOrElse{
                Timber.e(it, "Pin bookmark failed")
                false
            }



    suspend fun unpinBookmark(visitHistoryId: Int): Boolean =
            bookmarkUseCase.unpinBookmark(visitHistoryId).getOrElse {
                Timber.e(it, "Unpin bookmark failed")
                false
            }


    suspend fun deleteBookmark(venueInfo: VenueInfo) =
            getHistoryUseCase.deleteBookmark(venueInfo)

    suspend fun deleteCheckInVenue(visitHistoryId: Int, autoEndDate: Calendar?) =
            editHistoryUseCase.deleteVenue(visitHistoryId, autoEndDate != null)

    companion object {
        const val ALREADY_CHECKED_IN = BookmarkUseCase.ALREADY_CHECKED_IN
    }
}
