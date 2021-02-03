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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.headuck.app.gooutwithduck.data.AppDatabase
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistory

import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar

class SeedDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        try {

            val database = AppDatabase.getInstance(applicationContext)

            /*
            database.visitHistoryDao().insertVisitHistory(VisitHistory(
                    venueInfo = VenueInfo("Eng1", "Chin1", null, "IMPORT", "Z0aPj3be"),
                    scanType = "CITIZEN",
                    scanDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    startDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    endDate = Calendar.getInstance().apply { timeInMillis = 1606765600000 },
                    meta = null
            ))
            database.visitHistoryDao().insertVisitHistory(VisitHistory(
                    venueInfo = VenueInfo("Eng2", "Chin2", null, "IMPORT", "cnqMHec0"),
                    scanType = "CITIZEN",
                    scanDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    startDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    endDate = Calendar.getInstance().apply { timeInMillis = 1606765600000 },
                    meta = null
            ))
            database.visitHistoryDao().insertVisitHistory(VisitHistory(
                    venueInfo = VenueInfo("Eng2", "Chin2", null, "IMPORT", "cnqMHec0"),
                    scanType = "CITIZEN",
                    scanDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    startDate = Calendar.getInstance().apply { timeInMillis = 1606665600000 },
                    endDate = Calendar.getInstance().apply { timeInMillis = 1606765600000 },
                    meta = null
            ))

            database.visitHistoryDao().insertVisitHistory(VisitHistory(
                    venueInfo = VenueInfo(null, null, "VK7413", "TAXI", "00VK7413"),
                    scanType = "CITIZEN",
                    scanDate = Calendar.getInstance().apply { timeInMillis = 1606924800000 },
                    startDate = Calendar.getInstance().apply { timeInMillis = 1606934800000 },
                    endDate = Calendar.getInstance().apply { timeInMillis = 1606944800000 },
                    meta = null
            ))

            database.visitHistoryDao().insertVisitHistory(VisitHistory(
                    venueInfo = VenueInfo(null, null, "VK7413", "TAXI", "00VK7413"),
                    scanType = "CITIZEN",
                    scanDate = Calendar.getInstance().apply { timeInMillis = 1606838400000 },
                    startDate = Calendar.getInstance().apply { timeInMillis = 1606838400000 },
                    endDate = Calendar.getInstance().apply { timeInMillis = 1606924800000 },
                    meta = null
            ))
            */

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Error init database")
            Result.failure()
        }
    }

}
