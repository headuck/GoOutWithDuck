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


import androidx.paging.PagingData
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository
import com.headuck.app.gooutwithduck.data.VenueInfo

import com.headuck.app.gooutwithduck.data.VisitHistory;
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository;
import com.headuck.app.gooutwithduck.proto.UserPreferences
import com.headuck.app.gooutwithduck.utilities.NO_HISTORY_TYPE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transform

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class GetHistoryUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository) {

    fun getHistoryList(filter: String):  Flow<PagingData<VisitHistory>> =

        if (filter == NO_HISTORY_TYPE) {
            visitHistoryRepository.getVisitHistoryFlow("")
        } else {
            visitHistoryRepository.getVisitHistoryFlow(filter)
        }

    fun getCheckInHistoryList(): Flow<List<VisitHistory>> = visitHistoryRepository.getCheckInVisitHistoryFlow()

    fun getBookmarkList(): Flow<PagingData<VisitHistory>> {
        return userPreferencesRepository.userPreferencesFlow
                .mapLatest {
                    when (it.language) {

                        UserPreferences.Language.ZH_HK -> "zh"
                        UserPreferences.Language.ZH_CN -> "zh"
                        else -> "en"
                    }
                }
                .transform { lang ->
                    emitAll(visitHistoryRepository.getBookmarkFlow(lang))
                }

    }

    fun getBookmarkCountByTypeAndVenueId(type: String, venueId: String) = visitHistoryRepository.getBookmarkCountByTypeAndVenueId(type, venueId)

    suspend fun createBookmark(venueInfo: VenueInfo) = visitHistoryRepository.insertBookmark(venueInfo)
    suspend fun deleteBookmark(venueInfo: VenueInfo) = visitHistoryRepository.deleteBookmark(venueInfo)

    suspend fun deleteVenue(visitHistoryId: Int) = visitHistoryRepository.deleteVisitHistoryById(visitHistoryId)


}