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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.michaelbull.result.onSuccess
import com.headuck.app.gooutwithduck.adapters.HistoryAdapter
import com.headuck.app.gooutwithduck.data.VisitHistoryUiModel
import com.headuck.app.gooutwithduck.databinding.FragmentHistoryBinding
import com.headuck.app.gooutwithduck.utilities.isDateDifferent
import com.headuck.app.gooutwithduck.utilities.toDateStart
import com.headuck.app.gooutwithduck.viewmodels.HistoryListViewModel
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import com.headuck.app.gooutwithduck.workers.GetBatchesWorker.Companion.KEY_BATCH_DATA
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HistoryListFragment : Fragment() {

    private val viewModel: HistoryListViewModel by viewModels()
    private lateinit var binding: FragmentHistoryBinding

    private var searchJob: Job? = null
    private var adapter : HistoryAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        context ?: return binding.root
        adapter = HistoryAdapter(viewModel, getTouchListener(binding.historyList), viewLifecycleOwner)
        binding.historyList.adapter = adapter
        setHasOptionsMenu(true)
        updateData()
        return binding.root
    }

    /**
     * Create touch listener for swipe menu
     */
    private fun getTouchListener(recyclerView: RecyclerView): RecyclerTouchListener {
        val touchListener = RecyclerTouchListener(activity, recyclerView)
        return touchListener
                .setClickable(object : RecyclerTouchListener.OnRowClickListener {
                    override fun onRowClicked(position: Int) {
                        //Toast.makeText(context, "Row", Toast.LENGTH_SHORT).show()
                    }
                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {}
                })
                .setSwipeOptionViews(R.id.history_item_action_bookmark, R.id.history_item_action_edit)
                .setSwipeable(R.id.history_item_foreground, R.id.history_item_background) { viewID, position ->
                    when (viewID) {
                        R.id.history_item_action_bookmark -> {
                            val item = adapter?.getHistoryItemFromPosition(position)
                            val bookmarkLevel = adapter?.lastBookmarkLevel
                            item?.apply {
                                if (venueInfo.licenseNo == null) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        if (bookmarkLevel != 1) {
                                            val result = viewModel.createBookmark(venueInfo)
                                            result.onSuccess {
                                                if (it > 0) {
                                                    Toast.makeText(context, "Bookmark created", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Bookmark not created", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            val result = viewModel.deleteBookmark(venueInfo)
                                            result.onSuccess {
                                                if (it > 0) {
                                                    Toast.makeText(context, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Bookmark not deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }

                                    }
                                }
                            }

                        }
                        R.id.history_item_action_edit -> {
                            adapter?.getHistoryItemFromPosition(position)?.apply {
                                navigateToVenueDetailPage(id)
                            }
                        }
                    }
                }
                .setIgnoredViewTypes(HistoryAdapter.VISIT_HISTORY_SEPARATOR)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_history_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("Option item selected")
        return when (item.itemId) {
            R.id.filter_type -> {
                binding.historyList.scrollToPosition(0)
                with(viewModel) {
                    if (isFiltered()) {
                        clearHistoryTypeFilter()
                    } else {
                        setHistoryTypeFilter("IMPORT")
                    }
                }
                updateData()
                // download()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun download() {
        val workId = viewModel.download()
        activity?.let {
            WorkManager.getInstance(it.applicationContext).getWorkInfoByIdLiveData(workId)
                    .observe(this, Observer { info ->
                        if (info != null && info.state.isFinished) {
                            if (info.state != WorkInfo.State.SUCCEEDED) {
                                Timber.d("Error!!")
                            } else {
                                val myResult = info.outputData.getString(KEY_BATCH_DATA)
                                Timber.d("Result %s", myResult)
                            }
                        }
                    })
        }


    }

    private fun updateData() {
        // Make sure we cancel the previous job before creating a new one
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            viewModel.getVisitHistoryList().map {
                it.map { visitHistory ->
                    VisitHistoryUiModel.VisitHistoryItem(visitHistory)
                }.insertSeparators { before, after ->
                    when {
                        before == null && after != null ->
                            VisitHistoryUiModel.DateHeaderItem(toDateStart(after.startDate))
                        before != null && after != null && isDateDifferent(before.startDate, after.startDate) ->
                            VisitHistoryUiModel.DateHeaderItem(toDateStart(after.startDate))
                        else -> null
                    }
                }
            }
            .collectLatest {
                (binding.historyList.adapter as HistoryAdapter).submitData(it)
            }
        }

    }

    private fun navigateToVenueDetailPage(visitHistoryId: Int) {
        val direction =
                HistoryListFragmentDirections.actionHistoryFragmentToVenueDetailFragment(visitHistoryId)
        safeNavigate(direction)
    }

    /**
     * Check own fragment before navigate
     * See https://stackoverflow.com/questions/51060762/illegalargumentexception-navigation-destination-xxx-is-unknown-to-this-navcontr
     */
    private fun safeNavigate(direction: NavDirections) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.historyFragment) {
            navController.navigate(direction)
        }
    }

}
