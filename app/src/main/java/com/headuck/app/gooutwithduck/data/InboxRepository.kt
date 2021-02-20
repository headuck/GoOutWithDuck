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

package com.headuck.app.gooutwithduck.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.Query
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository module for handling data operations.
 */
@Singleton
class InboxRepository @Inject constructor(private val inboxDao: InboxDao) {


    fun getInboxFlow(): Flow<PagingData<Inbox>> =
        Pager(
                config = PagingConfig(enablePlaceholders = false, pageSize = DB_PAGE_SIZE),
                pagingSourceFactory = {
                    inboxDao.getInbox()
                }
        ).flow

    fun getUnreadCount(): Flow<Result<Int, Exception>> =
        inboxDao.getUnreadCount().mapLatest {
            Ok(it)
        }.catch {
            Err(it)
        }

    /**
     * Get by Inbox Record by id
     */
    suspend fun getInboxById(inboxId: Int) = try {
        val visitHistory = inboxDao.getInbox(inboxId)
        if (visitHistory != null) Ok(visitHistory) else Err(IllegalArgumentException("Inbox id $inboxId not found"))
    } catch (e: Exception) {
        Err(e)
    }

    /**
     * Get Inbox Record for history ids
     */
    suspend fun getInboxWithHistoryIds(historyIds: Set<Int>) = try {
        val inboxList = inboxDao.getInboxWithHistoryIds(historyIds)
        if (inboxList != null) Ok(inboxList) else Err(IllegalArgumentException("getInboxWithHistoryIds error"))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun saveInboxWithDownloadList(inbox: Inbox, inboxWithDownloadList: List<InboxWithDownload>, newInbox: Boolean) = try {
        Ok(inboxDao.saveInboxWithDownloadList(inbox, inboxWithDownloadList, newInbox))
    } catch (e: Exception) {
        Err(e)
    }

    /**
     * Insert visit history and return Id. If the venue is already checked in, return -1.
     */
    suspend fun insertInboxList(inboxList: List<Inbox>) = try {
        Timber.d("Thread ${Thread.currentThread().name}")
        Ok(inboxDao.insertAll(inboxList))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun updateAllAsRead() = try {
        Ok(inboxDao.updateAllAsRead())
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun deleteInbox(inbox: Inbox) =
            inboxDao.deleteInbox(inbox)

    suspend fun deleteInboxBefore(startDate: Calendar) =
            inboxDao.deleteInboxBefore(startDate)

    companion object {
        private const val DB_PAGE_SIZE = 10
    }

}