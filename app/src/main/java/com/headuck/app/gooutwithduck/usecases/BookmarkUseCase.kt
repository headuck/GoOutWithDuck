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

package com.headuck.app.gooutwithduck.usecases;


import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.headuck.app.gooutwithduck.data.VenueInfo

import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository
import com.headuck.app.gooutwithduck.utilities.CITIZEN_CHECK_IN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import javax.inject.Inject
import javax.inject.Singleton
import java.lang.Exception
import java.util.Calendar

/**
 * Logic to leave venue
 */
@Singleton
class BookmarkUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository) {

    fun enterBookmarkVenue(visitHistoryId: Int): Flow<Result<VisitHistory, String>> = flow {
        retrieveBookmark(visitHistoryId)
                    .onSuccess { bookmark ->
                        val bookmarkVenueInfo = bookmark.venueInfo
                        val now = Calendar.getInstance()
                        val visitHistory = VisitHistory(
                                venueInfo = VenueInfo(bookmarkVenueInfo.nameEn, bookmarkVenueInfo.nameZh, null, bookmarkVenueInfo.type, bookmarkVenueInfo.venueId),
                                meta = Json.encodeToString(bookmark.meta),
                                scanType = CITIZEN_CHECK_IN,
                                startDate = now
                        )
                        visitHistoryRepository.insertVisitHistory(visitHistory)
                        .onSuccess {
                            if (it == -1L) {
                                emit(Err(ALREADY_CHECKED_IN))
                            } else {
                                emit(Ok(visitHistory.apply { id = it.toInt() }))  // Set the saved id
                            }
                        }
                        .onFailure {
                            emit(Err("Failure writing record: " + it.message))
                        }
                    }
                    .onFailure {
                        emit(Err(it.localizedMessage))
                    }

    }

    suspend fun pinBookmark(visitHistoryId: Int): Result<Boolean, Exception> =
        visitHistoryRepository.setUploaded(visitHistoryId, true)


    suspend fun unpinBookmark(visitHistoryId: Int): Result<Boolean, Exception> =
        visitHistoryRepository.setUploaded(visitHistoryId, false)


    private suspend fun retrieveBookmark(visitHistoryId: Int): Result<VisitHistory, Exception> =
            visitHistoryRepository.getVisitHistoryById(visitHistoryId)

    companion object {
        const val ALREADY_CHECKED_IN = "ALREADY_CHECKED_IN"
    }
}