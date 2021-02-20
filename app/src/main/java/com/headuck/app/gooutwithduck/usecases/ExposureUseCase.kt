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


import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.unwrap

import com.headuck.app.gooutwithduck.data.DownloadCaseMatchResult
import com.headuck.app.gooutwithduck.data.DownloadCaseRepository
import com.headuck.app.gooutwithduck.data.Inbox
import com.headuck.app.gooutwithduck.data.InboxRepository
import com.headuck.app.gooutwithduck.data.InboxWithDownload
import com.headuck.app.gooutwithduck.data.UserPreferencesRepository
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository
import com.headuck.app.gooutwithduck.utilities.countCase

import com.headuck.app.gooutwithduck.utilities.toDateStart
import kotlinx.coroutines.flow.firstOrNull

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Calendar

/**
 * Logic to handle scanning of QR code
 */
@Singleton
class ExposureUseCase @Inject constructor(private val downloadCaseRepository: DownloadCaseRepository,
                                          private val userPreferencesRepository: UserPreferencesRepository,
                                          private val visitHistoryRepository: VisitHistoryRepository,
                                          private val inboxRepository: InboxRepository) {

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

    /**
     * Main function for exposure check after new download cases.
     * Match newly downloaded cases with bookmark and visit history
     */
    suspend fun exposureCheckNewDownload() : Result<Boolean, Exception> {
        val preference = userPreferencesRepository.userPreferencesFlow.firstOrNull()
        val checkExposureDays = defaultOr(DEFAULT_EXPOSURE_CHECK_DAYS, preference?.checkExposureDays)
        val directOverlapDuration = defaultOr(OVERLAP_DURATION, preference?.directOverlapDuration).toLong()
        val indirectVehicleDuration: Long = defaultOr(INDIRECT_WITHIN_VEHICLE_DAYS, preference?.indirectVehicleDays) * 24 * 3600 * 1000L
        val checkContactParam = CheckContactParam(directOverlapDuration, indirectVehicleDuration)

        val checkInExposureResult = downloadCaseRepository.checkExposure(since =
            Calendar.getInstance().apply {
                toDateStart(this)
            }.apply {
                add(Calendar.DAY_OF_YEAR, -checkExposureDays)
            }
        )
        val checkInCheckResult = checkInExposureResult.mapBoth(
            {
                processExposure(it, checkContactParam, false)
            },
            {
                Err(it)
            }
        )
        checkInCheckResult.onFailure {
            return Err(it)
        }
        val bookmarkExposureResult = downloadCaseRepository.checkBookmarkMatch()
        val bookmarkCheckResult = bookmarkExposureResult.mapBoth(
            {
                processExposure(it, checkContactParam, true)
            },
            {
                Err(it)
            }
        )
        bookmarkCheckResult.onFailure {
            return Err(it)
        }
        downloadCaseRepository.updateAllAsMatched()
        return Ok(checkInCheckResult.unwrap() || bookmarkCheckResult.unwrap())
    }

    /**
     * Main function for exposure check after adding new bookmark
     * Match bookmark against all downloaded cases
     */
    suspend fun exposureCheckNewBookmark(visitHistoryId: Int): Result<Boolean, Exception> {
        val bookmarkExposureResult = downloadCaseRepository.checkNewBookmarkMatch(visitHistoryId)
        val bookmarkCheckResult = bookmarkExposureResult.mapBoth(
            {
                processExposure(it, null /* not needed for bookmark */, true)
            },
            {
                Err(it)
            }
        )
        bookmarkCheckResult.onFailure {
            return Err(it)
        }
        return Ok(bookmarkCheckResult.unwrap())
    }

    data class DownloadContactPair(val downloadCaseMatchResult: DownloadCaseMatchResult, val checkContactResult: CheckContactResult)

    private suspend fun processExposure(downloadCaseExposureResults: List<DownloadCaseMatchResult>,
                                        checkContactParam: CheckContactParam?,
                                        isBookmark: Boolean): Result<Boolean, Exception> {
        var hasExposure = false
        downloadCaseExposureResults.isNotEmpty().also { notEmpty ->
            if (notEmpty) {

                // Get positive exposure notifications and group by visitHistoryId
                val exposureMapByVisitHistoryId: MutableMap<Int, MutableList<DownloadContactPair>> = HashMap()

                downloadCaseExposureResults.map {
                    DownloadContactPair(it, if (isBookmark) CheckContactResult(ContactType.INDIRECT, -1) else checkContact(it, checkContactParam!!))
                }.filter {
                    it.checkContactResult.contactType != ContactType.NONE
                }.groupByTo (
                    exposureMapByVisitHistoryId,
                    { it.downloadCaseMatchResult.id }
                )

                if (exposureMapByVisitHistoryId.isNotEmpty()) hasExposure = true

                val now = Calendar.getInstance()

                // Get matching entries from inbox, do update if needed
                inboxRepository.getInboxWithHistoryIds(exposureMapByVisitHistoryId.keys).onSuccess{ inboxList ->
                        // remove each matching "exposureMapByVisitHistoryId" entry from map
                        inboxList.forEach {
                            val historyId = it.id
                            // Remove matching history id from exposureMapByVisitHistoryId exposure map
                            val downloadContactPairList = exposureMapByVisitHistoryId.remove(historyId)
                            downloadContactPairList?.apply {
                                // List of new downloadContactPair to associate with Inbox
                                updateInboxSaveExposure(it, downloadContactPairList, now)
                            }
                        }
                }.onFailure {
                    return Err(it)
                }
                // Remaining exposureMapByVisitHistoryId - create new inbox entry and save list
                for (exposureEntry in exposureMapByVisitHistoryId.entries) {
                    addInboxSaveExposure(exposureEntry.key, exposureEntry.value, now, isBookmark)
                }
            }
        }
        return Ok(hasExposure)
    }

    private suspend fun updateInboxSaveExposure(inbox: Inbox, downloadContactPairList: List<DownloadContactPair>, now: Calendar) {
        if (downloadContactPairList.isEmpty()) return
        val inboxWithDownloadList = downloadContactPairList.map {
            downloadContactPairToInboxWithDownload(inbox.id, it, now)
        }
        // Collect exposure and total count from inboxWithDownloadList, init with current inbox value
        val acc = inboxWithDownloadList.fold( Pair(inbox.exposure, inbox.count), { acc, inboxWithDownload ->
            Pair(if (inboxWithDownload.exposure == "D") "D" else acc.first,
                 acc.second + inboxWithDownload.count)
        })
        // update inbox
        val updatedInbox = Inbox(
                historyId = inbox.historyId,
                venueInfo = inbox.venueInfo,
                date = inbox.date,
                bookmark = inbox.bookmark,
                exposure = acc.first, //
                count = acc.second,
                read = inbox.read,
                lastUpdate = now
        )
        updatedInbox.id = inbox.id
        inboxRepository.saveInboxWithDownloadList(updatedInbox, inboxWithDownloadList, false)
        // Mark relevant visitHistory
        visitHistoryRepository.setExposure(updatedInbox.historyId, updatedInbox.exposure)
    }

    private suspend fun addInboxSaveExposure(visitHistoryId: Int, downloadContactPairList: List<DownloadContactPair>, now: Calendar, isBookmark: Boolean) {
        if (downloadContactPairList.isEmpty()) return
        val inboxWithDownloadList = downloadContactPairList.map {
            downloadContactPairToInboxWithDownload(-1, it, now)
        }
        // Collect exposure and total count from inboxWithDownloadList
        val acc = inboxWithDownloadList.fold( Pair("I", 0), { acc, inboxWithDownload ->
            Pair(if (inboxWithDownload.exposure == "D") "D" else acc.first,
                    acc.second + inboxWithDownload.count)
        })
        val matchResult = downloadContactPairList[0].downloadCaseMatchResult
        val matchVenueInfo = VenueInfo(
                nameEn = matchResult.nameEn,
                nameZh = matchResult.nameZh,
                licenseNo = matchResult.licenseNo,
                type = matchResult.type,
                venueId = matchResult.venueId
        )
        // new inbox
        val newInbox = Inbox(
                historyId = visitHistoryId,
                venueInfo = matchVenueInfo,
                date = matchResult.visitStartDate,
                bookmark = isBookmark,
                exposure = acc.first,
                count = acc.second,
                read = false,
                lastUpdate = now
        )
        inboxRepository.saveInboxWithDownloadList(newInbox, inboxWithDownloadList, true)
        // Mark relevant visitHistory
        visitHistoryRepository.setExposure(newInbox.historyId, newInbox.exposure)
    }

    private fun downloadContactPairToInboxWithDownload(inboxId: Int, pair: DownloadContactPair, now: Calendar) = InboxWithDownload(
            inboxId = inboxId,
            downloadCaseId = pair.downloadCaseMatchResult.downloadId,
            exposure = if (pair.checkContactResult.contactType == ContactType.DIRECT) "D" else "I",
            count = countCase(pair.downloadCaseMatchResult.downloadRandom),
            duration = pair.checkContactResult.diff,
            matchDate = now
    )

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
