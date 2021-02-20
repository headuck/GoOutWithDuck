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
import androidx.paging.PagingSource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository module for handling data operations.
 */
@Singleton
class DownloadCaseRepository @Inject constructor(private val downloadCaseDao: DownloadCaseDao) {

    fun getDownloadCaseFlow(type: String): Flow<PagingData<DownloadCase>> =
            Pager(
                    config = PagingConfig(enablePlaceholders = false, pageSize = DownloadCaseRepository.DB_PAGE_SIZE),
                    pagingSourceFactory = {
                        if (type == "")
                            downloadCaseDao.getDownloadCaseList()
                        else
                            downloadCaseDao.getDownloadCaseListByType(type)
                    }
            ).flow

    suspend fun getMaxBatchId() = try {
        Ok(downloadCaseDao.getMaxBatchId()?: -1)
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun insertDownloadCase(downloadCase: DownloadCase) = try {
        /*
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Timber.d("Thread ${Thread.currentThread().name}, venueInfo=%s start=%s end=%s",
                downloadCase.venueInfo,
                dateFormat.format(downloadCase.startDate.time),
                dateFormat.format(downloadCase.endDate.time))
        */
        Ok(downloadCaseDao.insertDownloadCase(downloadCase))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun insertDownloadCaseWithDupCheck(downloadCase: DownloadCase) = try {
        /*
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Timber.d("Thread ${Thread.currentThread().name}, venueInfo=%s start=%s end=%s",
                downloadCase.venueInfo,
                dateFormat.format(downloadCase.startDate.time),
                dateFormat.format(downloadCase.endDate.time))
        */
        Ok(downloadCaseDao.insertDownloadCaseWithDupCheck(downloadCase))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun checkExposure(since: Calendar) = try {
        Ok(downloadCaseDao.checkExposure(since.timeInMillis))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun checkBookmarkMatch() = try {
        Ok(downloadCaseDao.checkBookmarkMatch())
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun checkNewBookmarkMatch(visitHistoryId: Int) = try {
        Ok(downloadCaseDao.checkNewBookmarkMatch(visitHistoryId))
    } catch (e: Exception) {
        Err(e)
    }

    suspend fun updateAllAsMatched() =
            downloadCaseDao.updateAllAsMatched()

    suspend fun deleteAllDownloadCase() =
            downloadCaseDao.deleteAllDownloadCase()

    companion object {
        private const val DB_PAGE_SIZE = 20
    }

}