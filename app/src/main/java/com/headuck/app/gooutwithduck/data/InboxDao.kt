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
import java.util.*


/**
 * The Data Access Object for the Inbox class.
 */
@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox ORDER BY last_update DESC")
    abstract fun getInbox(): PagingSource<Int, Inbox>

    @Query("SELECT * FROM inbox WHERE id = :id")
    abstract suspend fun getInbox(id: Int): Inbox?

    @Query("SELECT COUNT(*) FROM inbox WHERE read = 0")
    abstract fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(inbox: Inbox): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(inbox: List<Inbox>)

    @Update
    abstract suspend fun updateAll(inbox: List<Inbox>): Int

    @Delete
    abstract suspend fun deleteInbox(inbox: Inbox)

    @Query("DELETE FROM inbox WHERE date < :date")
    abstract suspend fun deleteInboxBefore(date: Calendar)

    @Query("UPDATE inbox SET read = 1 WHERE read = 0")
    abstract suspend fun updateAllAsRead()

    @Query("SELECT * FROM inbox WHERE history_id IN (:historyIds)")
    abstract suspend fun getInboxWithHistoryIds(historyIds: Set<Int>): List<Inbox>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addInboxWithDownloadList(inboxWithDownloadList: List<InboxWithDownload>)

    @Update
    abstract suspend fun updateInboxWithDownloadList(inboxWithDownloadList: List<InboxWithDownload>): Int

    @Transaction
    suspend fun saveInboxWithDownloadList(inbox: Inbox, inboxWithDownloadList: List<InboxWithDownload>, newInbox: Boolean)  {
        if (newInbox) {
            val inboxId = insert(inbox).toInt()
            inboxWithDownloadList.forEach{
                it.inboxId = inboxId
            }
        } else {
            updateAll(listOf(inbox))
        }
        addInboxWithDownloadList(inboxWithDownloadList)
    }

}
