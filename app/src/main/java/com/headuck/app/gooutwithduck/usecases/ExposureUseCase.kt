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

import com.headuck.app.gooutwithduck.utilities.toDateStart

import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber
import java.util.*

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class ExposureUseCase @Inject constructor(private val downloadCaseRepository: DownloadCaseRepository) {

    suspend fun exposureCheck() : Boolean {
        val visitExposureList = downloadCaseRepository.checkExposure(since =
            Calendar.getInstance().apply {
                toDateStart(this)
            }.apply {
                add(Calendar.DAY_OF_YEAR, -14)
            }
        ).unwrap()

        val bookmarkExposureList = downloadCaseRepository.checkBookmarkMatch().unwrap()

        return (visitExposureList.isNotEmpty()).also {
            if (it) {
                //Timber.d("Exposure %s", visitExposureList.toString())
                for (ex in visitExposureList) {
                    // Check exposure
                    val contactResult = checkContact(ex)
                    Timber.d("Contact result %s", contactResult.toString())
                }
            }
        }
    }


    enum class ContactType {
        DIRECT, INDIRECT, NONE
    }

    data class CheckContactResult(val contactType: ContactType, val diff: Long)

    private fun checkContact(ex: DownloadCaseMatchResult): CheckContactResult {
        // config
        val indirectWithin = if (ex.type == "TAXI") INDIRECT_WITHIN_TAXI else -1
        val overlapDuration = OVERLAP_DURATION
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
        if (overlap > overlapDuration) {
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
        const val INDIRECT_WITHIN_TAXI = 24*3600*1000
        const val OVERLAP_DURATION = 60*1000
    }

}
