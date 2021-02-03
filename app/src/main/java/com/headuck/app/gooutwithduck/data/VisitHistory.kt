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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 *
 * Declaring the column info allows for the renaming of variables without implementing a
 * database migration, as the column name would not change.
 */
@Entity(
    tableName = "visit_history",
    indices = [Index("start_date"), Index("end_date"), Index("type")]
)
data class VisitHistory(
    /**
     * Group id
     */
    val groupId: String = NO_GROUP,
    /**
     * Venue info
     */
    @Embedded val venueInfo: VenueInfo,
    val scanType: String,
    /**
     * Meta field from scanned code
     */
    val meta: String?,
    /**
     * Indicates when the [VisitHistory] created, default now
     */
    @ColumnInfo(name = "scan_date")
    val scanDate: Calendar = Calendar.getInstance(),
    /**
     * Indicates when the [VisitHistory] created, default now
     */
    @ColumnInfo(name = "auto_end_date")
    val autoEndDate: Calendar? = null,
    /**
     * Indicates when the [VisitHistory] started, default now
     */
    @ColumnInfo(name = "start_date")
    val startDate: Calendar = Calendar.getInstance(),
    /**
     * Indicates when the [VisitHistory] ended
     */
    @ColumnInfo(name = "end_date")
    val endDate: Calendar? = null,
    /**
     * Reverse geocode result latitude from Google, if any
     */
    val lat: Double? = null,
    /**
     * Reverse geocode result longitude from Google, if any
     */
    val lon: Double? = null,
    /**
     * Random string generated for data to upload
     */
    val random: String? = null,
    /**
     * Record uploaded; Pinned flag for bookmark
     */
    val uploaded: Boolean = false,
    /**
     * True for bookmark
     */
    val bookmark: Boolean = false,
    /**
     * Count for bookmark
     */
    val bookmarkCount: Int = 0,
    /**
     * Exposure matched, "D" for direct, "I" for indirect
     */
    val exposure: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    companion object {
        const val NO_GROUP = "00000000"
    }
}


