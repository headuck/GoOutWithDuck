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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExitCheckWorkerUtil @Inject constructor(@ApplicationContext val ctx: Context) {

    fun setExitSchedule(visitHistoryId: Int, autoEndTime: Calendar) {
        val msDelay = autoEndTime.timeInMillis - System.currentTimeMillis()
        val requestBuilder = OneTimeWorkRequest.Builder(ExitCheckWorker::class.java).addTag(EXIT_CHECK_WORK_TAG)
        if (msDelay > 0) {
            requestBuilder.setInitialDelay(msDelay, TimeUnit.MILLISECONDS)
        }

        val request = requestBuilder.build()
        val workManager = WorkManager.getInstance(ctx)
        val continuation = workManager.beginUniqueWork(
                        EXIT_CHECK_WORK_NAME + visitHistoryId,
                        ExistingWorkPolicy.REPLACE,
                        request
                )
        // Actually start the work
        continuation.enqueue()
        Timber.d("ExitCheckWorkerUtil: schedule work id $visitHistoryId")
    }

    fun cancelExitSchedlue(visitHistoryId: Int) {
        val workManager = WorkManager.getInstance(ctx)
        workManager.cancelUniqueWork(EXIT_CHECK_WORK_NAME + visitHistoryId)
        Timber.d("ExitCheckWorkerUtil: cancel work id $visitHistoryId")
    }

    companion object {
        const val EXIT_CHECK_WORK_NAME = "EXIT_CHECK_WORK_"
        const val EXIT_CHECK_WORK_TAG = "EXIT_CHECK_WORK_TAG"
    }

}


