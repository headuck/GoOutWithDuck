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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.headuck.app.gooutwithduck.BuildConfig
import com.headuck.app.gooutwithduck.utilities.CITIZEN_CHECK_IN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
class VisitHistoryRepository @Inject constructor(private val visitHistoryDao: VisitHistoryDao) {


    fun getVisitHistoryFlow(type: String): Flow<PagingData<VisitHistory>> =
        Pager(
                config = PagingConfig(enablePlaceholders = false, pageSize = DB_PAGE_SIZE),
                pagingSourceFactory = {
                    if (type == "")
                        visitHistoryDao.getVisitHistoryList()
                    else
                        visitHistoryDao.getVisitHistoryListByType(type)
                }
        ).flow

    fun getCheckInVisitHistoryFlow(): Flow<List<VisitHistory>> = visitHistoryDao.getCheckInVisitHistoryList()

    fun getBookmarkFlow(lang: String): Flow<PagingData<VisitHistory>> =
        Pager(
                config = PagingConfig(enablePlaceholders = false, pageSize = DB_PAGE_SIZE),
                pagingSourceFactory = {
                    when (lang) {
                        "zh" -> visitHistoryDao.getBookmarkListZh()
                        else -> visitHistoryDao.getBookmarkListEn()
                    }
                }
        ).flow

    /**
     * Get by VisitHistory Record by id
     */
    suspend fun getVisitHistoryById(visitHistoryId: Int) = try {
        val visitHistory = visitHistoryDao.getVisitHistory(visitHistoryId)
        if (visitHistory != null) Ok(visitHistory) else Err(IllegalArgumentException("Visit history id $visitHistoryId not found"))
    } catch (e: Exception) {
        Err(e)
    }

    fun getVisitHistoryByIdFlow(visitHistoryId: Int) =
        visitHistoryDao.getVisitHistoryFlow(visitHistoryId).distinctUntilChanged().mapLatest {
            Ok(it) as Result<VisitHistory, Exception>
        }.catch {
            e ->
                if (e !is Exception) throw e
                emit(Err(e))
        }


    fun getBookmarkCountByTypeAndVenueId(type: String, venueId: String): Flow<Int> =
        visitHistoryDao.getBookmarkCountByTypeAndVenueId(type, venueId).distinctUntilChanged()

    /**
     * Insert visit history and return Id. If the venue is already checked in, return -1.
     */
    suspend fun insertVisitHistory(visitHistory: VisitHistory) = try {
        Timber.d("Thread ${Thread.currentThread().name}")
        if (BuildConfig.DEBUG && visitHistory.autoEndDate != null) {
            error("Auto End date should not be set initially") // Or should call WorkManager to schedule
        }
        val venueCheckIn = visitHistoryDao.getCheckInVisitHistoryListForVenue(venueId = visitHistory.venueInfo.venueId, type = visitHistory.venueInfo.type)
        if (venueCheckIn.isEmpty()) {
            Ok(visitHistoryDao.insertVisitHistory(visitHistory))
        } else {
            Ok(-1L)
        }
    } catch (e: Exception) {
        Err(e)
    }

    /**
     * Insert bookmark. If bookmark already exists return -1 wrapped in result
     */
    suspend fun insertBookmark(venueInfo: VenueInfo) = try {
        val bookmarkCount = visitHistoryDao.getBookmarkCountByTypeAndVenueId(venueInfo.type, venueInfo.venueId).first()
        if (bookmarkCount == 0) {
            Ok(visitHistoryDao.insertVisitHistory(VisitHistory(
                    venueInfo = venueInfo,
                    scanType = CITIZEN_CHECK_IN,
                    bookmark = true,
                    meta = null,
            )))
        } else {
            Ok(-1L)
        }
    } catch (e: Exception) {
        Err(e)
    }

    /**
     * Delete bookmark, return number of items deleted
     */
    suspend fun deleteBookmark(venueInfo: VenueInfo) = try {
        Ok(visitHistoryDao.deleteBookmarkByTypeAndVenueId(venueInfo.type, venueInfo.venueId))
    } catch (e: Exception) {
        Err(e)
    }

    /**
     * Delete history by id, return number of items deleted
     */
    suspend fun deleteVisitHistoryById(id: Int) = try {
        Ok(visitHistoryDao.deleteVisitHistoryById(id))
    } catch (e: Exception) {
        Err(e)
    }

    /**
     *
     */
    suspend fun setUploaded(visitHistoryId: Int, value: Boolean): Result<Boolean, Exception> = try {
        Ok(visitHistoryDao.setUploaded(visitHistoryId, value))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun leaveVenue(visitHistoryId: Int, time: Calendar) = try {
        visitHistoryDao.leaveVenue(visitHistoryId, time)
        Ok(true)
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun setAutoEnd(visitHistoryId: Int, autoEndDate: Calendar?) = try {
        visitHistoryDao.setAutoEnd(visitHistoryId, autoEndDate)
        Ok(true)
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun deleteVisitHistory(visitHistory: VisitHistory) = try {
        visitHistoryDao.deleteVisitHistory(visitHistory)
        Ok(true)
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun deleteVisitHistoryBefore(startDate: Calendar) = try {
        visitHistoryDao.deleteVisitHistoryBefore(startDate)
        Ok(true)
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun updateVisitHistory(visitHistory: VisitHistory) = try {
        Ok(visitHistoryDao.updateVisitHistory(visitHistory))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun updateVisitHistoryTime(visitHistoryId: Int, timeVal: Calendar, isEntryTime: Boolean) = try {
        if (isEntryTime) {
            Ok(visitHistoryDao.updateVisitHistoryStartDate(visitHistoryId, timeVal))
        } else {
            Ok(visitHistoryDao.updateVisitHistoryEndDate(visitHistoryId, timeVal))
        }
    } catch (e: Exception) {
        Err(e)
    }

    companion object {
        private const val DB_PAGE_SIZE = 10
    }

}