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

package com.headuck.app.gooutwithduck.usecases;

import android.content.Context
import com.headuck.app.gooutwithduck.R
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class ManualInputUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository,
                                             private val userPreferencesRepository: UserPreferencesRepository) {

    private lateinit var ctbFleetMap: HashMap<String, String>

    private suspend fun readFleet(context: Context) = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.fleet_ctb)
        val reader = BufferedReader(
                InputStreamReader(inputStream, Charset.forName("UTF-8")))
        var line = ""
        var firstLine = true
        ctbFleetMap = HashMap()
        try {
            while (reader.readLine().also { line = it } != null) {
                if (firstLine) {
                    firstLine = false
                    continue
                }
                // Split the line into different tokens (using the comma as a separator).
                val tokens = line.split(",".toRegex()).toTypedArray()
                if (tokens.size > 1) {
                    ctbFleetMap[tokens[0]] = tokens[1]
                }
            }
        } catch (e1: IOException) {
            Timber.e(e1, "Error: $line")
        }
    }

    /**
     * Convert ctb fleet no to license No
     */
    suspend fun convertCtb(context: Context, serial: String): String? {
        if (! ::ctbFleetMap.isInitialized) {
            readFleet(context)
        }
        return ctbFleetMap.get(serial)
    }

}