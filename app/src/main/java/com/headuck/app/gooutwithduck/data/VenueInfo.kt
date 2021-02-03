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

data class VenueInfo(
    /**
     * English name if any
     */
    val nameEn: String?,
    /**
     * Chinese name if any
     */
    val nameZh: String?,
    /**
     * License no for taxi
     */
    val licenseNo: String?,
    /**
     * Type of venue from code or "TAXI"
     */
    val type: String,
    /**
     * Venue Id from scan code
     */
    val venueId: String
)

