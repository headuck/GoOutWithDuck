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
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ForeignKey
import java.util.Calendar

/**
 *
 * Declaring the column info allows for the renaming of variables without implementing a
 * database migration, as the column name would not change.
 */
@Entity(
    tableName = "inbox",
    indices = [Index("last_update"), Index("history_id")],
    foreignKeys = [ForeignKey(
            entity = VisitHistory::class,
            parentColumns = ["id"],
            childColumns = ["history_id"],
            onDelete = ForeignKey.CASCADE
    )]
)
data class Inbox(
    /**
     * relevant history id (one to one)
     */
    @ColumnInfo(name = "history_id")
    val historyId: Int,
    /**
     * Venue info
     */
    @Embedded
    val venueInfo: VenueInfo,
    /**
     * Indicates when the date
     */
    val date: Calendar,
    /**
     * True for bookmark
     */
    val bookmark: Boolean = false,
    /**
     * Exposure matched, "D" for direct, "I" for indirect
     */
    val exposure: String? = null,
    /**
     * Exposure count
     */
    val count: Int,
    /**
     * True for read message
     */
    val read: Boolean = false,
    /**
     * Last update date
     */
    @ColumnInfo(name = "last_update")
    val lastUpdate: Calendar
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}


