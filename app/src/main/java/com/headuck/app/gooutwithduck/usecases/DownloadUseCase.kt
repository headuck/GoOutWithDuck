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

import at.favre.lib.crypto.HKDF
import com.github.michaelbull.result.*

import com.headuck.app.gooutwithduck.api.ApiService
import com.headuck.app.gooutwithduck.data.*
import com.headuck.app.gooutwithduck.proto.Exposure

import com.headuck.app.gooutwithduck.proto.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Logic to handle download of cases. Return Ok(true) if any new download written
 */
@Singleton
class DownloadUseCase @Inject constructor(private val downloadCaseRepository: DownloadCaseRepository,
                                          private val apiService: ApiService,
                                          private val userPreferencesRepository: UserPreferencesRepository) {

    suspend fun downloadCases() : Result<Boolean, IOException> {
        val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow
        val userPreference = userPreferencesFlow.first()

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBatches()
                val list = response?.body()
                val listFiltered = batchFilter(userPreference, list ?: ArrayList())
                Ok(listFiltered)
            } catch (e: IOException) {
                Timber.e(e, "Exception occurred: %s %s", e.javaClass.simpleName, e.message);
                Err(e)
            }
        }.andThen {batchList ->
                try {
                    // Timber.d("Batch list %s", batchList.toString())
                    Ok(getZipFilesAsByteArrays(batchList)
                            .flowOn(Dispatchers.IO)
                            .buffer(5)
                            .flatMapConcat (this::byteArrayToDownloadCaseFlow)
                            .foldConsecutive (this::mergeWithSameVenueId)
                            .map {
                                val saved = downloadCaseRepository.insertDownloadCase(it).unwrap() // raise exception
                                Triple(it.id, it.batchDate, saved)
                            }
                            .fold(Triple(userPreference.lastDownloadBatch, userPreference.lastDownloadTime, 0L)) {
                                acc, value -> Triple(
                                    kotlin.math.max(acc.first, value.first), // last downloaded batch
                                    kotlin.math.max(acc.second, value.second.timeInMillis), // last download time
                                    acc.third + value.third // sum of inserts
                                )
                            }
                            .run{
                                userPreferencesRepository.updateLastBatchInfo(this.first, this.second)
                                userPreferencesRepository.updateLastUserDownloadTime(System.currentTimeMillis())
                                this.third > 0
                            })

                } catch (e: IOException) {
                    Timber.e(e, "Exception occurred: %s %s", e.javaClass.simpleName, e.message);
                    Err(e)
                }
        }


    }

    private fun batchFilter(userPreference: UserPreferences, batchList: List<BatchModel>) : List<BatchModel> {
        val lastDlTime = userPreference.lastDownloadTime
        val lastDlBatch = userPreference.lastDownloadBatch
        return batchList.filter{
            it.batchSize > 0 && (it.id > lastDlBatch || batchFilenameLaterThan(it, lastDlTime))
        }
    }

    private fun batchFilenameLaterThan(batchModel: BatchModel, lastDlTime: Long): Boolean =
        timestampFromFilename(batchModel.filename)?.let{
            it > lastDlTime
        }?: (batchModel.updatedAt > lastDlTime) // If filename format changed, use last updated


    private fun getZipFilesAsByteArrays(batchModelList: List<BatchModel>): Flow<Triple<ByteArray, Int, Long>> {
        return batchModelList.asFlow().flatMapConcat{ batchModel ->

            val filename = batchModel.filename
            try {
                val response = apiService.downloadFileByUrl(filename)
                response?.let {
                    extractZip(it, batchModel)
                } ?: emptyFlow()
            } catch (e: IOException) {
                Timber.e(e, "Exception occurred: %s %s", e.javaClass.simpleName, e.message);
                emptyFlow()
            }
        }
    }

    private fun timestampFromFilename(filename: String): Long? {
        val idx1 = filename.indexOf('-');
        val idx2 = filename.indexOf('.');
        if (idx1 != -1 && idx2 != -1 && (idx1+1 < idx2)) {
            return filename.subSequence(idx1+1, idx2).toString().toLongOrNull()
        }
        return null
    }

    private fun extractZip(response: Response<ResponseBody?>, batchModel: BatchModel): Flow<Triple<ByteArray, Int, Long>> {

        val body = response.body() ?: throw IOException("Empty body")
        val length = body.contentLength()
        val contentType = body.contentType()?.toString()
        Timber.d("Zip file %s length %d content type %s", batchModel.filename, length, contentType)

        return flow {
            val ios = body.byteStream()
            val bufferSize = 1024 * 8
            val bufferedInputStream = BufferedInputStream(ios, bufferSize)
            val zis = ZipInputStream(bufferedInputStream);
            try {
                while (true) {
                    val ze = zis.nextEntry ?: break
                    Timber.d("Extract file : %s", ze.name)
                    emit(Triple(readContentToByteArray(zis), batchModel.id, timestampFromFilename(batchModel.filename) ?: batchModel.updatedAt))
                }

            } finally {
                zis.close()
                bufferedInputStream.close()
                ios.close()
            }
        }
    }

    private fun readContentToByteArray(zis: ZipInputStream): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024 * 8)
        var length: Int
        while (zis.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toByteArray()
    }

    private fun byteArrayToDownloadCaseFlow(arrayResults: Triple<ByteArray, Int, Long>): Flow<DownloadCase> = flow {
        val decoded = Base64.decode(arrayResults.first, Base64.DEFAULT)
        Timber.d("Decoded Array length: %d", decoded.size)
        if (decoded.isNotEmpty()) {
            val exposureKeyExport = Exposure.ExposureKeyExport.parseFrom(decoded)
            (0 until exposureKeyExport.batchSize).forEach {
                exposureKeyExport.getKeys(it).run {
                    emit(processKey(keyDataBytes.toByteArray(), keyIntervalBytes.toByteArray(), arrayResults.second, arrayResults.third))
                }
            }
        }
    }

    private fun processKey(keyData: ByteArray, keyInterval: ByteArray, batchId: Int, batchUpdateTime: Long): DownloadCase {

        // HKDF encode keyInterval
        val keyDataDecoded = Base64.decode(keyData, Base64.DEFAULT)
        val hkdf = HKDF.fromHmacSha256()
        val extracted = hkdf.extract(null as? ByteArray, keyInterval) // passing null is ambiguous
        val key = hkdf.expand(extracted, "HKEN".toByteArray(), 16)

        // AES 128 decryption
        val skeySpec = SecretKeySpec(key, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val ivSize = 16
        val iv = ByteArray(ivSize)
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec)
        val decrypted = cipher.doFinal(keyDataDecoded)

        // Parse decrypted string
        val startTsCal = Calendar.getInstance().apply {
            timeInMillis = String(decrypted, 24, 13).toLong()
        }
        val endTsCal = Calendar.getInstance().apply {
            timeInMillis = String(decrypted, 37, 13).toLong()
        }
        val random = String(decrypted, 0, 8)
        val venueId = String(decrypted, 8, 8)
        val groupId = String(decrypted, 16, 8)
        val json = String(Base64.decode(decrypted, 50, decrypted.size - 50, Base64.DEFAULT))

        // For venue: json is of format
        //   {"name_zh_hk":"安心出行咖啡廳","type":"IMPORT","name_zh_cn":"安心出行咖啡廳","name_en":"LeaveHomeSafe café"}
        // both Chinese and English name values may be null or white space e.g. \n
        // For taxi:
        //   {"type":"TAXI","taxiNo":"KB4484"}

        val downloadJsonContent = Json{ignoreUnknownKeys = true}.decodeFromString<DownloadJsonContent>(json)
        var nameEn = downloadJsonContent.name_en?.trim()
        var nameZh = downloadJsonContent.name_zh_hk?.trim()
        if (downloadJsonContent.type != "TAXI") {
            if (nameZh.isNullOrBlank()) {
                if (nameEn.isNullOrBlank()) {
                    nameEn = ""
                    Timber.w("Both English and Chinese names are empty for non-TAXI entry!")
                }
                nameZh = nameEn
            } else if (nameEn.isNullOrBlank()) {
                nameEn = nameZh
            }
        }
        val batchUpdateCal = Calendar.getInstance().apply{ timeInMillis = batchUpdateTime }

        return DownloadCase(groupId, random, null, VenueInfo(nameEn, nameZh, downloadJsonContent.taxiNo?.trim(), downloadJsonContent.type, venueId),
                startDate = startTsCal, endDate = endTsCal,
                batchId = batchId, batchDate = batchUpdateCal )
    }

    @Serializable
    data class DownloadJsonContent(val name_zh_hk: String? = null, val name_en: String? = null, val type: String,
                                   val taxiNo: String? = null)

    private fun <T> Flow<T>.foldConsecutive(merge: (T, T) -> T?): Flow<T> = flow {
        var acc: T? = null

        collect { t ->
            if (acc != null) {
                acc?.apply {
                    if (merge(this, t) == null) {
                        emit(this)
                        acc = null
                    }
                }
            } else {
                acc = t
            }
        }
        acc?.let{ emit(it) }
    }

    private fun mergeWithSameVenueId (acc: DownloadCase, t: DownloadCase): DownloadCase? {
        return if (acc.venueInfo.venueId == t.venueInfo.venueId && acc.startDate == t.startDate
                && acc.endDate == t.endDate) {
            acc.apply {
                this.random += "," + t.random
            }
        } else {
            null
        }
    }
}
