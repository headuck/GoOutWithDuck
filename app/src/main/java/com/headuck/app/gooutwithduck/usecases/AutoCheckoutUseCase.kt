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
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository
import com.headuck.app.gooutwithduck.workers.ExitCheckWorkerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

import java.util.Calendar

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logic to handle auto checkout
 */
@Singleton
class AutoCheckoutUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository,
                                              private val userPreferencesRepository: UserPreferencesRepository,
                                              private val exitCheckWorkerUtil: ExitCheckWorkerUtil) {

    fun autoCheckoutSetting():  Flow<Pair<Boolean, Int>> = flow {
        userPreferencesRepository.userPreferencesFlow
                .collect {
                    emit(Pair(it.autoCheckoutOn, it.autoCheckoutDuration))
                }
    }

    suspend fun saveAutoCheckoutSetting(isOn: Boolean, duration: Int) {
        userPreferencesRepository.updateAutoCheckOut(isOn, duration)
    }

    suspend fun setAutoCheckoutForVisitHistory(visitHistoryId: Int, isOn: Boolean, durationSecond: Int) {
        visitHistoryRepository.getVisitHistoryById(visitHistoryId)
                .onSuccess {
                    val autoEndDate = if (isOn) {
                        calculateEndTime(it.startDate, durationSecond)
                    } else {
                        null
                    }
                    if (autoEndDate == null) {
                        exitCheckWorkerUtil.cancelExitSchedlue(it.id)
                    } else {
                        // This would replace previous settings
                        exitCheckWorkerUtil.setExitSchedule(it.id, autoEndDate)
                    }
                    visitHistoryRepository.setAutoEnd(it.id, autoEndDate)
                }
                .onFailure {
                    Err(it)
                }
    }

    private fun calculateEndTime(start: Calendar, durationSecond: Int): Calendar {
        val millis = start.timeInMillis + durationSecond.toLong() * 1000
        return Calendar.getInstance().apply {
            timeInMillis = millis
        }
    }


}