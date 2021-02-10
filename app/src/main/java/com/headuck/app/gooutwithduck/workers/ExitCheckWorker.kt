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

package com.headuck.app.gooutwithduck.workers

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.usecases.GetHistoryUseCase
import com.headuck.app.gooutwithduck.usecases.LeaveVenueUseCase
import com.headuck.app.gooutwithduck.viewmodels.HistoryListViewModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExitCheckWorker @WorkerInject constructor(@Assisted val ctx: Context, @Assisted params: WorkerParameters,
                                                        private val getHistoryUseCase: GetHistoryUseCase,
                                                        private val leaveVenueUseCase: LeaveVenueUseCase) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        val now = Calendar.getInstance()

        // Get list of check in items
        val checkInList = getHistoryUseCase.getCheckInHistoryList().firstOrNull()
        val outputData = if (checkInList != null) {
            var count = 0
            val msg = ArrayList<String>()
            for (checkIn in checkInList) {
                if (checkIn.autoEndDate != null && checkIn.endDate == null) {
                    // If pending auto checkout
                    if (checkIn.autoEndDate < now) {
                        // and auto checkout time already passed, check out with the preset time
                        leaveVenueUseCase.leaveVenue(checkIn.id, checkIn.autoEndDate)
                                .onSuccess {
                                    count ++
                                }
                                .onFailure {
                                    msg.add("Leave error for venue ${checkIn.id}: ${it.message}")
                                }
                    }
                }
            }
            workDataOf(KEY_NUM_VENUE_EXITED to count,
                    KEY_EXIT_ERROR to msg.joinToString(","))

        } else {
            workDataOf(KEY_NUM_VENUE_EXITED to 0,
                    KEY_EXIT_ERROR to "Error getting checkIn list")
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success(outputData)
    }

    companion object {
        const val KEY_NUM_VENUE_EXITED = "KEY_NUM_VENUE_EXITED"
        const val KEY_EXIT_ERROR = "KEY_EXIT_ERROR"
    }

}


