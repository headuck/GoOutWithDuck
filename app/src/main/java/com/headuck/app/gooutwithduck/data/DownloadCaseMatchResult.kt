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
import java.text.SimpleDateFormat
import java.util.*

data class DownloadCaseMatchResult (
    val id: Int,
    val type: String,
    val venueId: String,
    val nameEn: String?,
    val nameZh: String?,
    val licenseNo: String?,
    @ColumnInfo(name = "visit_random")
    val visitRandom: String?,
    @ColumnInfo(name = "visit_start_date")
    val visitStartDate: Calendar,
    @ColumnInfo(name = "visit_end_date")
    val visitEndDate: Calendar?,
    @ColumnInfo(name = "download_start_date")
    val downloadStartDate: Calendar,
    @ColumnInfo(name = "download_end_date")
    val downloadEndDate: Calendar,
    @ColumnInfo(name = "download_random")
    val downloadRandom: String
) {
    override fun toString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return "DownloadCaseMatchResult(id=$id, type='$type', venueId='$venueId', nameEn=$nameEn, nameZh=$nameZh, licenseNo=$licenseNo, " +
                "visit_random=$visitRandom, visit_start_date=${dateFormat.format(visitStartDate.time)}, " +
                "visit_end_date=${visitEndDate?.let{dateFormat.format(it.time)}}, " +
                "download_start_date=${dateFormat.format(downloadStartDate.time)}, download_end_date=${dateFormat.format(downloadEndDate.time)}, " +
                "download_random='$downloadRandom')"
    }
}