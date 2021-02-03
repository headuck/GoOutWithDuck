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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.headuck.app.gooutwithduck.proto.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val DATA_STORE_FILE_NAME = "user_prefs.pb"

/**
 * Repository module for handling data operations.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore: DataStore<UserPreferences> =
            DataStoreFactory.create(
                    serializer = PreferencesSerializer,
                    produceFile = {
                            File(context.filesDir, DATA_STORE_FILE_NAME)
                    }
            )

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Timber.e(exception, "Error reading user preferences")
                    emit(UserPreferences.getDefaultInstance())
                } else {
                    throw exception
                }
            }

    suspend fun updateFirstInstallTime(firstInstallTime: Calendar) {
        Timber.d("Update first install time")
        dataStore.updateData { preferences ->
            if (preferences.firstInstallTime == 0L) {
                preferences.toBuilder().setFirstInstallTime(firstInstallTime.timeInMillis).build()
            } else {
                preferences
            }
        }
    }

    suspend fun updateLastBatchInfo(lastDownloadBatch: Int, lastDownloadTime: Long) {
        Timber.d("Update last batch info: last batch = %d, last time = %d", lastDownloadBatch, lastDownloadTime)
        dataStore.updateData { preferences ->
            if (preferences.lastDownloadBatch < lastDownloadBatch || preferences.lastDownloadTime < lastDownloadTime) {
                preferences.toBuilder()
                        .setLastDownloadBatch(lastDownloadBatch)
                        .setLastDownloadTime(lastDownloadTime)
                        .build()
            } else {
                preferences
            }
        }
    }

    suspend fun updateLastUserDownloadTime(lastUserDownloadTime: Long) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                    .setLastUserDownloadTime(lastUserDownloadTime)
                    .build()
        }
    }

    suspend fun updateAutoCheckOut(isOn: Boolean, duration: Int) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                    .setAutoCheckoutOn(isOn)
                    .setAutoCheckoutDuration(duration)
                    .build()
        }
    }

}
