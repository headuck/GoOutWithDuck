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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.headuck.app.gooutwithduck.data.Inbox
import com.headuck.app.gooutwithduck.data.InboxRepository
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import java.util.*

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class InboxUseCase @Inject constructor(private val inboxRepository: InboxRepository,
                                       private val userPreferencesRepository: UserPreferencesRepository) {

    fun getInboxList():  Flow<PagingData<Inbox>> =
        inboxRepository.getInboxFlow()

    fun getInboxUnreadCount(): Flow<Result<Int, Exception>> =
            inboxRepository.getUnreadCount()

    fun getLastUpdate(): Flow<Result<Calendar?, Exception>> =
        userPreferencesRepository.userPreferencesFlow
                .mapLatest {
                    it.lastUserDownloadTime.let{ timeMillis ->
                        when {
                            timeMillis > 0 -> {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = timeMillis
                                Ok(calendar)
                            }
                            else -> Ok(null)
                        }

                    }
                }.catch {
                    Err(it)
                }
}