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

import com.headuck.app.gooutwithduck.utilities.countCase
import java.util.Calendar

sealed class InboxUiModel {
    class InboxItem(val id: Int, val venueInfo: VenueInfo, val date: Calendar, val lastUpdate: Calendar, val bookmark: Boolean,
                           val count: Int, val exposure: String?, val read: Boolean, val historyId: Int) : InboxUiModel() {
        constructor(inbox: Inbox) : this(
                inbox.id, inbox.venueInfo, inbox.date, inbox.lastUpdate,
                inbox.bookmark, inbox.count, inbox.exposure, inbox.read, inbox.historyId
        )
    }

    class DateHeaderItem(private val date: Calendar) : InboxUiModel(), DateHeader {
        override fun getDate(): Calendar = date
    }

}