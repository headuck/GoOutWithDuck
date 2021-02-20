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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.ForeignKey
import java.util.Calendar

/**
 * Inbox and Download many-to-many relations
 */
@Entity(
    tableName = "inbox_download",
    primaryKeys = ["inbox_id", "download_case_id"],
    indices = [Index("download_case_id")],
    foreignKeys = [
        ForeignKey(
            entity = Inbox::class,
            parentColumns = ["id"],
            childColumns = ["inbox_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DownloadCase::class,
            parentColumns = ["id"],
            childColumns = ["download_case_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InboxWithDownload(
    /**
     * relevant inbox id
     */
    @ColumnInfo(name = "inbox_id")
    var inboxId: Int,
    /**
     * relevant download id
     */
    @ColumnInfo(name = "download_case_id")
    val downloadCaseId: Int,
    /**
     * exposure type
     */
    val exposure: String,
    /**
     * match count
     */
    val count: Int,
    /**
     * duration
     */
    val duration: Long,
    /**
     * create date
     */
    @ColumnInfo(name = "match_date")
    val matchDate: Calendar,
)


