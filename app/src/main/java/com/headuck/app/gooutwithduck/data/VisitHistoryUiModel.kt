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

import java.util.*

sealed class VisitHistoryUiModel {
    class VisitHistoryItem(val id: Int, val venueInfo: VenueInfo, val startDate: Calendar, val endDate: Calendar?,
                           val autoEndDate: Calendar?, val exposure: String?) : VisitHistoryUiModel() {
        constructor(visitHistory: VisitHistory) : this(
                visitHistory.id, visitHistory.venueInfo, visitHistory.startDate,
                visitHistory.endDate, visitHistory.autoEndDate, visitHistory.exposure
        )
    }

    class DateHeaderItem(val date: Calendar) : VisitHistoryUiModel()

}