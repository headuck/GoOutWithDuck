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
import java.util.Calendar

/**
 *
 * Declaring the column info allows for the renaming of variables without implementing a
 * database migration, as the column name would not change.
 */
@Entity(
    tableName = "download_case",
    indices = [Index("start_date"), Index(value = ["matched", "type", "venueId"])]
)
data class DownloadCase(
    /**
     * Group id
     */
    val groupId: String = NO_GROUP,
    /**
     * Random part string which comes with download
     * (to recognise self upload)
     * delimited with ","
     */
    var random: String,
    /**
     * Case numbers (reserved)
     * delimited with ","
     */
    var caseNums: String? = null,
    /**
     * Venue info
     */
    @Embedded val venueInfo: VenueInfo,

    /**
     * Type of source
     */
    val sourceType: String = SOURCE_HKEN,
    /**
     * Indicates when the [DownloadCase] started
     */
    @ColumnInfo(name = "start_date")
    val startDate: Calendar,
    /**
     * Indicates when the [DownloadCase] ended
     */
    @ColumnInfo(name = "end_date")
    val endDate: Calendar,
    /**
     * Reverse geocode result latitude from Google, if any
     */
    val lat: Double? = null,
    /**
     * Reverse geocode result longitude from Google, if any
     */
    val lon: Double? = null,
    /**
     * Record already used for matching
     */
    val matched: Boolean = false,
    /**
     * Batch id
     */
    val batchId: Int,
    /**
     * Batch date
     */
    @ColumnInfo(name = "batch_date")
    val batchDate: Calendar,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    companion object {
        const val NO_GROUP = "00000000"
        const val SOURCE_HKEN = "HKEN"
    }
}


