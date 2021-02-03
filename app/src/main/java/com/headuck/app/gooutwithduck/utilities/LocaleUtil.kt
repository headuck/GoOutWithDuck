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

package com.headuck.app.gooutwithduck.utilities

import android.content.Context
import android.os.Build
import com.headuck.app.gooutwithduck.R
import com.headuck.app.gooutwithduck.data.CheckInUiModel
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistoryUiModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class LocaleUtil {

    companion object {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.US);
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        private fun getCurrentLocale(context: Context): Locale? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0)
            } else {
                context.resources.configuration.locale
            }
        }

        fun getDisplayLang(context: Context): String {
            val locale = getCurrentLocale(context)
            if (locale?.language == "zh") {
                return "zh"
            }
            return "en"
        }
        @JvmStatic
        fun getVisitLocationName(displayLang: String, venueInfo: VenueInfo?): String {
            if (venueInfo == null) {
                Timber.d("Venue info is null")
                return ""
            }
            return when {
                venueInfo.type == "TAXI" -> venueInfo.licenseNo ?: "None"
                displayLang == "zh" -> (if (venueInfo.nameZh.isNullOrBlank()) venueInfo.nameEn else venueInfo.nameZh) ?: "None"
                else -> (if (venueInfo.nameEn.isNullOrBlank()) venueInfo.nameZh else venueInfo.nameEn) ?: "None"
            }
        }
        @JvmStatic
        fun getVisitLocationOthers(context: Context, displayLang: String, others: Int): String {
            val resources = context.resources
            return when (displayLang) {
                "zh" -> resources.getQuantityString(R.plurals.checkin_others, others, others)
                else -> resources.getQuantityString(R.plurals.checkin_others, others, others)
            }
        }
        @JvmStatic
        fun getVisitLocationTime(displayLang: String, visitHistoryItem: VisitHistoryUiModel.VisitHistoryItem): String =
             when (displayLang) {
                "zh" -> timeFormat.format(visitHistoryItem.startDate.time)
                else -> timeFormat.format(visitHistoryItem.startDate.time)
            }

        @JvmStatic
        fun getNotificationDateTime(displayLang: String, date: Calendar?): String =
                date?.let {
                    when (displayLang) {
                        "zh" -> dateTimeFormat.format(it.time)
                        else -> dateTimeFormat.format(it.time)
                    }
                } ?: "-"

        @JvmStatic
        fun getVisitLocationTimeEnd(displayLang: String, visitHistoryItem: VisitHistoryUiModel.VisitHistoryItem): String =
                visitHistoryItem.endDate?.let {
                    when (displayLang) {
                        "zh" -> timeFormat.format(it.time)
                        else -> timeFormat.format(it.time)
                    }
                } ?: ""

        @JvmStatic
        fun getVisitLocationTimeAutoCheckOut(displayLang: String, visitHistoryItem: VisitHistoryUiModel.VisitHistoryItem): String =
                visitHistoryItem.autoEndDate?.let {
                    when (displayLang) {
                        "zh" -> timeFormat.format(it.time)
                        else -> timeFormat.format(it.time)
                    }
                } ?: ""

        @JvmStatic
        fun getVisitLocationDuration(displayLang: String, checkInItem: CheckInUiModel.CheckInItem): String =
                when (displayLang) {
                    "zh" -> timeFormat.format(checkInItem.startDate.time)
                    else -> timeFormat.format(checkInItem.startDate.time)
                }

        @JvmStatic
        fun getVisitLocationAutoCheckOut(displayLang: String, checkInItem: CheckInUiModel.CheckInItem): String =
                checkInItem.autoEndDate?.let {
                    when (displayLang) {
                        "zh" -> timeFormat.format(it.time)
                        else -> timeFormat.format(it.time)
                    }
                } ?: ""

        @JvmStatic
        fun getVisitLocationDate(displayLang: String, date: Calendar): String =
            when (displayLang) {
                "zh" -> dateFormat.format(date.time)
                else -> dateFormat.format(date.time)
            }

        @JvmStatic
        fun getVisitLocationTime(displayLang: String, date: Calendar): String =
            when (displayLang) {
                "zh" -> timeFormat.format(date.time)
                else -> timeFormat.format(date.time)
            }

    }
}