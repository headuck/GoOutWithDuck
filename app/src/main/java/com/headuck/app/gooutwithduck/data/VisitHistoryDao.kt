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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.Calendar

/**
 * The Data Access Object for the [VisitHistory] class.
 */
@Dao
abstract class VisitHistoryDao {
    @Query("SELECT * FROM visit_history WHERE bookmark = 0 ORDER BY start_date DESC, id DESC")
    abstract fun getVisitHistoryList(): PagingSource<Int, VisitHistory>

    @Query("SELECT * FROM visit_history WHERE bookmark = 0 AND end_date IS NULL ORDER BY start_date DESC, id DESC")
    abstract fun getCheckInVisitHistoryList(): Flow<List<VisitHistory>>

    @Query("SELECT * FROM visit_history WHERE bookmark = 0 AND end_date IS NULL AND venueId = :venueId AND type = :type ORDER BY start_date DESC, id DESC")
    abstract suspend fun getCheckInVisitHistoryListForVenue(type: String, venueId: String): List<VisitHistory>

    @Query("SELECT * FROM visit_history WHERE type = :type AND bookmark = 0 ORDER BY start_date DESC, id DESC")
    abstract fun getVisitHistoryListByType(type: String): PagingSource<Int, VisitHistory>

    @Query("SELECT * FROM visit_history WHERE bookmark = 1 ORDER BY uploaded DESC, nameZh")
    abstract fun getBookmarkListZh(): PagingSource<Int, VisitHistory>

    @Query("SELECT * FROM visit_history WHERE bookmark = 1 ORDER BY uploaded DESC, nameEn")
    abstract fun getBookmarkListEn(): PagingSource<Int, VisitHistory>

    @Query("SELECT * FROM visit_history WHERE id = :id")
    abstract suspend fun getVisitHistory(id: Int): VisitHistory?

    @Query("SELECT * FROM visit_history WHERE id = :id")
    abstract fun getVisitHistoryFlow(id: Int): Flow<VisitHistory>

    @Query("SELECT COUNT(*) FROM visit_history WHERE bookmark = 1 AND type = :type AND venueId = :venueId")
    abstract fun getBookmarkCountByTypeAndVenueId(type: String, venueId: String): Flow<Int>

    @Query("DELETE FROM visit_history WHERE bookmark = 1 AND type = :type AND venueId = :venueId")
    abstract suspend fun deleteBookmarkByTypeAndVenueId(type: String, venueId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVisitHistory(visitHistory: VisitHistory): Long

    @Query("DELETE FROM visit_history WHERE id = :id")
    abstract suspend fun deleteVisitHistoryById(id: Int): Int

    @Update
    abstract suspend fun updateVisitHistory(visitHistory: VisitHistory)

    @Delete
    abstract suspend fun deleteVisitHistory(visitHistory: VisitHistory)

    @Query("UPDATE visit_history SET start_date = :startDate WHERE id = :id")
    abstract suspend fun updateVisitHistoryStartDate(id: Int, startDate: Calendar)

    @Query("UPDATE visit_history SET end_date = :endDate, auto_end_date = NULL WHERE id = :id")
    abstract suspend fun updateVisitHistoryEndDate(id: Int, endDate: Calendar)

    @Query("UPDATE visit_history SET auto_end_date = :autoEndDate WHERE id = :id")
    abstract suspend fun updateVisitHistoryAutoEndDate(id: Int, autoEndDate: Calendar?)

    @Query("UPDATE visit_history SET uploaded = :uploaded WHERE id = :id")
    abstract suspend fun updateVisitHistoryUploaded(id: Int, uploaded: Boolean)

    @Query("DELETE FROM visit_history WHERE start_date < :startDate AND bookmark = 0")
    abstract suspend fun deleteVisitHistoryBefore(startDate: Calendar)

    @Query("UPDATE visit_history SET exposure = :exposure WHERE id = :id")
    abstract suspend fun updateVisitHistoryExposure(id: Int, exposure: String?)

    @Transaction
    open suspend fun leaveVenue(visitHistoryId: Int, time: Calendar): Boolean {
        val visitHistory = getVisitHistory(visitHistoryId)
        Timber.d("Get history id %d", visitHistoryId)
        return visitHistory?.run{
            if (this.endDate == null) {
                Timber.d("Update enddate")
                updateVisitHistoryEndDate(visitHistoryId, time)
                true
            } else {
                Timber.d("Enddate already set")
                false
            }
        } ?: false
    }

    @Transaction
    open suspend fun setAutoEnd(visitHistoryId: Int, autoEndDate: Calendar?): Boolean {
        val visitHistory = getVisitHistory(visitHistoryId)
        Timber.d("Get history id %d", visitHistoryId)
        return visitHistory?.run{
            if (this.autoEndDate == null && autoEndDate != null) {
                Timber.d("set auto end date")
                updateVisitHistoryAutoEndDate(visitHistoryId, autoEndDate)
                true
            } else if (this.autoEndDate != null && autoEndDate == null) {
                Timber.d("clear auto end date")
                updateVisitHistoryAutoEndDate(visitHistoryId, null)
                true
            } else if (this.autoEndDate != null && this.autoEndDate != autoEndDate) {
                Timber.d("update auto end date")
                updateVisitHistoryAutoEndDate(visitHistoryId, autoEndDate)
                true
            } else false
        } ?: false
    }

    @Transaction
    open suspend fun setExposure(visitHistoryId: Int, exposureIn: String?): Boolean {
        val visitHistory = getVisitHistory(visitHistoryId)
        Timber.d("Get history id %d", visitHistoryId)
        return visitHistory?.run{
            if (exposure != exposureIn) {
                updateVisitHistoryExposure(visitHistoryId, exposureIn)
                true
            } else {
                Timber.d("No need to update exposure")
                false
            }
        } ?: false
    }

    /**
     * Set the uploaded flag of visit history. True if updated, false if no need to update or error
     */
    @Transaction
    open suspend fun setUploaded(visitHistoryId: Int, value: Boolean): Boolean {
        val visitHistory = getVisitHistory(visitHistoryId)
        return visitHistory?.run {
            if (uploaded != value) {
                updateVisitHistoryUploaded(visitHistoryId, value)
                true
            } else {
                Timber.d("No need to update uploaded")
                false
            }
        } ?: false
    }
}
