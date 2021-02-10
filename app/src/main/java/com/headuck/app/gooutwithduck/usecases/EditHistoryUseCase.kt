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

import com.headuck.app.gooutwithduck.data.VisitHistory;
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository;

import com.github.michaelbull.result.Result
import com.headuck.app.gooutwithduck.workers.ExitCheckWorkerUtil
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class EditHistoryUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository,
                                             private val exitCheckWorkerUtil: ExitCheckWorkerUtil) {

    fun getVenueById(visitHistoryId: Int): Flow<Result<VisitHistory, Exception>> =
        visitHistoryRepository.getVisitHistoryByIdFlow(visitHistoryId)

    suspend fun deleteVenue(visitHistoryId: Int, hasAutoEndDate: Boolean): Result<Int, Exception> {
        if (hasAutoEndDate) {
            exitCheckWorkerUtil.cancelExitSchedlue(visitHistoryId)
        }
        return visitHistoryRepository.deleteVisitHistoryById(visitHistoryId)
    }

    suspend fun updateVenue(visitHistory: VisitHistory) {
        if (visitHistory.autoEndDate != null) {
            exitCheckWorkerUtil.setExitSchedule(visitHistory.id, visitHistory.autoEndDate)
        } else {
            exitCheckWorkerUtil.cancelExitSchedlue(visitHistory.id)
        }
        visitHistoryRepository.updateVisitHistory(visitHistory)
    }

    suspend fun updateVenueTime(visitHistoryId: Int, timeVal: Calendar, isEntryTime: Boolean) {
        visitHistoryRepository.updateVisitHistoryTime(visitHistoryId, timeVal, isEntryTime)
    }

}