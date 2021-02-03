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


import android.util.Base64
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.headuck.app.gooutwithduck.data.VenueInfo

import com.headuck.app.gooutwithduck.data.VisitHistory;
import com.headuck.app.gooutwithduck.data.VisitHistoryRepository;
import com.headuck.app.gooutwithduck.utilities.CITIZEN_CHECK_IN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import timber.log.Timber
import java.security.MessageDigest
import java.util.*

/**
 * Logic to handle scanning of QR code
 *
 */
@Singleton
class ScanCodeUseCase @Inject constructor(private val visitHistoryRepository: VisitHistoryRepository) {
    /**
     * Save newly scanned code
     *
     * @param scanCode scan code
     * If venue of scan code is already checked in, emit Error with "ALREADY_CHECKED_IN"
     * Emit exception message for other errors
     * Emit Ok with visit history id if success
     */
    fun setNewScan(scanCode: String): Flow<Result<VisitHistory, String>> {
        Timber.d("Set new scan")
        return flow {
            decode(scanCode)
                    .onSuccess {
                        visitHistory ->
                            Timber.d("onSuccess")
                            visitHistoryRepository.insertVisitHistory(visitHistory)
                                    .onSuccess {
                                        if (it == -1L) {
                                            emit(Err(ALREADY_CHECKED_IN))
                                        } else {
                                            emit(Ok(visitHistory.apply { id = it.toInt() }))  // Set the saved id
                                        }
                                    }
                                    .onFailure {
                                        emit(Err("Failure writing record: " + it.message))
                                    }
                    }
                    .onFailure {
                        emit(Err(it))
                    }
        }
    }

    @Serializable
    data class CodeContentMeta(val typeZh: String? = null, val typeEn: String? = null)

    @Serializable
    data class CodeContent(val nameZh: String? = null, val nameEn: String? = null, val type: String,
                           val hash: String, val metadata: CodeContentMeta? = null)


    private fun decode(scanCode: String): Result<VisitHistory, String> =
        if (!scanCode.startsWith("HKEN:0")) {
            Timber.e("Invalid prefix")
            Err("Invalid prefix")
        } else if (scanCode.length <= (6 + 8)) {
            Timber.e("Invalid code")
            Err("Invalid code")
        } else {
            val venueId = scanCode.slice(6..13)
            val base64Code = scanCode.substring(14)
            try {
                val decodedJsonBytes = Base64.decode(base64Code, Base64.DEFAULT)
                val decodedJson = String(decodedJsonBytes)
                val codeContent = Json{ignoreUnknownKeys = true}.decodeFromString<CodeContent>(decodedJson)
                if (!checkHash(venueId, codeContent.hash)) {
                    Err("Invalid code, mismatch hash")
                } else {
                    Timber.d("Decode success: $decodedJson")
                    val now = Calendar.getInstance()
                    Ok(
                            VisitHistory(
                                    venueInfo = VenueInfo(codeContent.nameEn, codeContent.nameZh, null, codeContent.type, venueId),
                                    meta = Json.encodeToString(codeContent.metadata),
                                    scanType = CITIZEN_CHECK_IN,
                                    startDate = now
                            )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception")
                Err("Error decoding: " + e.message)
            }
        }

    private fun checkHash(venueId: String, hash: String): Boolean {
        val message = "HKEN" + venueId + "2020"
        val bytes = message.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val digestHex = digest.fold("", { str, it -> str + "%02x".format(it) })
        if (hash == digestHex) return true;
        Timber.w("Hash mismatch: scanned=$hash calc=$digestHex")
        return false;
    }

     companion object {
         const val ALREADY_CHECKED_IN = "ALREADY_CHECKED_IN"
    }
}
