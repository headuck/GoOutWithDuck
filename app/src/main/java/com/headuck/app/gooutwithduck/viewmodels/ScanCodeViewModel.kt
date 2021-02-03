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

package com.headuck.app.gooutwithduck.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.headuck.app.gooutwithduck.data.VisitHistory
import com.headuck.app.gooutwithduck.usecases.ScanCodeUseCase
import com.github.michaelbull.result.Result
import com.headuck.app.gooutwithduck.ScanCodeFragment

/**
 * The ViewModel for [ScanCodeFragment].
 */
class ScanCodeViewModel @ViewModelInject internal constructor(
    private val scanCodeUseCase: ScanCodeUseCase
) : ViewModel() {
    //private var currentQueryValue: String? = null
    //private var currentSearchResult: Flow<PagingData<UnsplashPhoto>>? = null

    fun setNewScan(scanCode: String) : LiveData<Result<VisitHistory, String>> =
        scanCodeUseCase.setNewScan(scanCode).asLiveData()


    /*
    fun searchPictures(queryString: String): Flow<PagingData<UnsplashPhoto>> {
        currentQueryValue = queryString
        val newResult: Flow<PagingData<UnsplashPhoto>> =
            repository.getSearchResultStream(queryString).cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }*/
    companion object {
        const val ALREADY_CHECKED_IN = ScanCodeUseCase.ALREADY_CHECKED_IN
    }
}
