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

package com.headuck.app.gooutwithduck

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.headuck.app.gooutwithduck.adapters.DownloadAdapter
import com.headuck.app.gooutwithduck.data.DownloadUiModel
import com.headuck.app.gooutwithduck.databinding.FragmentDownloadBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.utilities.SNACK_DURATION
import com.headuck.app.gooutwithduck.utilities.isDateDifferent
import com.headuck.app.gooutwithduck.utilities.toDateStart
import com.headuck.app.gooutwithduck.viewmodels.DownloadViewModel
import com.headuck.app.gooutwithduck.workers.GetBatchesWorker.Companion.KEY_BATCH_DATA
import com.headuck.app.gooutwithduck.workers.GetBatchesWorker.Companion.KEY_ERROR_MSG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class DownloadFragment : Fragment() {

    private val viewModel: DownloadViewModel by viewModels()
    private lateinit var binding: FragmentDownloadBinding

    private var searchJob: Job? = null
    private var adapter : DownloadAdapter? = null

    interface Callback {
        fun onRefresh(view: View)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDownloadBinding.inflate(inflater, container, false)
        context ?: return binding.root
        adapter = DownloadAdapter()
        binding.downloadList.adapter = adapter
        binding.callback = object : Callback {
            override fun onRefresh(view: View) {
                download()
            }
        }
        //setHasOptionsMenu(true)
        updateData()
        return binding.root
    }
/*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_history_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("Option item selected")
        return when (item.itemId) {
            R.id.filter_type -> {
                binding.downloadList.scrollToPosition(0)
                with(viewModel) {
                    if (isFiltered()) {
                        clearDownloadTypeFilter()
                    } else {
                        setDownloadTypeFilter("IMPORT")
                    }
                }
                updateData()
                // download()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
*/
    @SuppressLint("WrongConstant")
    private fun download() {
        binding.downloadFab.hide()
        val workId = viewModel.download()
        activity?.let {
            WorkManager.getInstance(it.applicationContext).getWorkInfoByIdLiveData(workId)
                    .observe(this, { info ->
                        if (info != null) {
                            if (info.state.isFinished) {
                                binding.downloadFab.show()
                                if (info.state != WorkInfo.State.SUCCEEDED) {
                                    val myResult = info.outputData.getString(KEY_ERROR_MSG)
                                    myResult?.run {
                                        Snackbar.make(
                                                binding.root,
                                                getString(R.string.download_error_message,this),
                                                Snackbar.LENGTH_LONG)
                                            .setDuration(SNACK_DURATION)
                                            .show()
                                    }
                                    Timber.e("Download Error - %s", myResult?: "Null")
                                } else {
                                    val myResult = info.outputData.getString(KEY_BATCH_DATA)
                                    Timber.d("Result %s", myResult)
                                }
                            } else {
                                // Either queued or running
                                Timber.d("Download progress %s", info.state.name)
                            }
                        }
                    })
        }
    }

    private fun updateData() {
        // Make sure we cancel the previous job before creating a new one
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            viewModel.getDownloadList().map {
                it.map { downloadCase ->
                    DownloadUiModel.DownloadItem(downloadCase)
                }.insertSeparators { before, after ->
                    when {
                        before == null && after != null ->
                            DownloadUiModel.DateHeaderItem(toDateStart(after.startDate))
                        before != null && after != null && isDateDifferent(before.startDate, after.startDate) ->
                            DownloadUiModel.DateHeaderItem(toDateStart(after.startDate))
                        else -> null
                    }
                }
            }
            .collectLatest {
                (binding.downloadList.adapter as DownloadAdapter).submitData(it)
            }
        }

    }

}
