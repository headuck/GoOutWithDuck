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


import com.github.michaelbull.result.*

import com.headuck.app.gooutwithduck.data.DownloadCaseMatchResult
import com.headuck.app.gooutwithduck.data.DownloadCaseRepository
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository

import com.headuck.app.gooutwithduck.utilities.toDateStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull

import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber
import java.util.*

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class ExposureUseCase @Inject constructor(private val downloadCaseRepository: DownloadCaseRepository,
                                        private val userPreferencesRepository: UserPreferencesRepository) {

    /**
     * Return default value if input value is 0 or null
     * @param defaultVal default value
     * @param value input value
     * @return value
     */
    private fun defaultOr(defaultVal: Int, value: Int?): Int {
        if (value == null) {
            return defaultVal
        }
        else {
            if (value == 0) return defaultVal
            return value
        }
    }

    suspend fun exposureCheck() : Boolean {
        val preference = userPreferencesRepository.userPreferencesFlow.firstOrNull()
        val checkExposureDays = defaultOr(DEFAULT_EXPOSURE_CHECK_DAYS, preference?.checkExposureDays)

        val visitExposureList = downloadCaseRepository.checkExposure(since =
            Calendar.getInstance().apply {
                toDateStart(this)
            }.apply {
                add(Calendar.DAY_OF_YEAR, -checkExposureDays)
            }
        ).unwrap()

        val bookmarkExposureList = downloadCaseRepository.checkBookmarkMatch().unwrap()

        val directOverlapDuration = defaultOr(OVERLAP_DURATION, preference?.directOverlapDuration).toLong()
        val indirectVehicleDuration : Long = defaultOr(INDIRECT_WITHIN_VEHICLE_DAYS, preference?.indirectVehicleDays) * 24*3600*1000L

        return (visitExposureList.isNotEmpty()).also {
            if (it) {
                val checkContactParam = CheckContactParam(directOverlapDuration, indirectVehicleDuration)
                //Timber.d("Exposure %s", visitExposureList.toString())
                for (ex in visitExposureList) {
                    // Check exposure
                    val contactResult = checkContact(ex, checkContactParam)
                    Timber.d("Contact result %s", contactResult.toString())
                    // Write results

                }
            }
        }
    }


    enum class ContactType {
        DIRECT, INDIRECT, NONE
    }

    data class CheckContactParam(val directOverlapDuration: Long, val indirectVehicleDuration: Long)
    data class CheckContactResult(val contactType: ContactType, val diff: Long)

    private fun checkContact(ex: DownloadCaseMatchResult, param: CheckContactParam): CheckContactResult {
        // config
        val indirectWithin = if (ex.type == "TAXI") param.indirectVehicleDuration else -1

        val visitEndDate = ex.visitEndDate ?: Calendar.getInstance()

        if (ex.downloadEndDate <= ex.visitStartDate || visitEndDate <= ex.downloadStartDate) {
            if (ex.downloadEndDate <= ex.visitStartDate) {
                val indirectDiff = diffCal(from = ex.downloadEndDate, to = ex.visitStartDate)
                if (indirectDiff <= indirectWithin) {
                    return CheckContactResult(ContactType.INDIRECT, diffCal(from = ex.visitStartDate, to = visitEndDate))
                }
            }
            return CheckContactResult(ContactType.NONE, -1)
        }

        val overlap = if (ex.downloadStartDate <= ex.visitStartDate) {
            if (ex.downloadEndDate <= visitEndDate) {
                diffCal(from = ex.visitStartDate, to = ex.downloadEndDate)
            } else {
                diffCal(from = ex.visitStartDate, to = visitEndDate)
            }
        } else {
            if (visitEndDate <= ex.downloadEndDate) {
                diffCal(from = ex.downloadStartDate, to = visitEndDate)
            } else {
                diffCal(from = ex.downloadStartDate, to = ex.downloadEndDate)
            }
        }
        // Direct contact
        if (overlap > param.directOverlapDuration) {
            return CheckContactResult(ContactType.DIRECT, overlap)
        }

        // Insufficient overlap, check fallback to INDIRECT
        return if (indirectWithin > 0) {
            CheckContactResult(ContactType.INDIRECT, diffCal(from = ex.visitStartDate, to = visitEndDate))
        } else {
            CheckContactResult(ContactType.NONE, -1)
        }
    }

    private fun diffCal(from: Calendar, to: Calendar): Long = to.timeInMillis - from.timeInMillis

    companion object {
        const val DEFAULT_EXPOSURE_CHECK_DAYS = 14
        const val INDIRECT_WITHIN_VEHICLE_DAYS = 1
        const val OVERLAP_DURATION = 60*1000
    }

}
