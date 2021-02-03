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

import com.headuck.app.gooutwithduck.data.UserPreferencesRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.count
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logic to handle first run
 */
@Singleton
class CheckFirstRunUseCase @Inject constructor(
        private val userPreferencesRepository: UserPreferencesRepository
    ) {
    suspend fun firstRunCheck() : Boolean {
        val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow
        return userPreferencesFlow.filter {
            it.firstInstallTime == 0L
        }.map {
            userPreferencesRepository.updateFirstInstallTime(Calendar.getInstance())
        }.count() > 0
     }
}
