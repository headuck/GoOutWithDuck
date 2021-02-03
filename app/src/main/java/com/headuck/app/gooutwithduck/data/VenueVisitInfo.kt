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

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class VenueVisitInfo (
        val id: Int,
        val type: String,
        val taxiNo: String?,
        val name: String?,
        val inTimeStr: String
): Parcelable {
    constructor(visitHistory: VisitHistory, displayLang: String) : this(
            visitHistory.id,
            visitHistory.venueInfo.type,
            visitHistory.venueInfo.licenseNo,
            if (displayLang == "zh") visitHistory.venueInfo.nameZh else visitHistory.venueInfo.nameEn,
            visitHistory.let {
                val dateTimeFormat = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm", Locale.ENGLISH
                )
                dateTimeFormat.timeZone = TimeZone.getDefault()
                dateTimeFormat.format(it.startDate.time)
            }
    )
}
