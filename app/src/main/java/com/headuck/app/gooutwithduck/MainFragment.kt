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
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.paging.map
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.headuck.app.gooutwithduck.adapters.BookmarkAdapter
import com.headuck.app.gooutwithduck.adapters.CheckInAdapter
import com.headuck.app.gooutwithduck.adapters.NotificationAdapter
import com.headuck.app.gooutwithduck.data.BookmarkUiModel
import com.headuck.app.gooutwithduck.data.CheckInUiModel
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VenueVisitInfo
import com.headuck.app.gooutwithduck.databinding.FragmentMainBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.utilities.SNACK_DURATION
import com.headuck.app.gooutwithduck.utilities.combineWith
import com.headuck.app.gooutwithduck.viewmodels.BottomNavSharedViewModel
import com.headuck.app.gooutwithduck.viewmodels.MainViewModel
import com.headuck.app.gooutwithduck.views.AppBarBehavior
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date


@AndroidEntryPoint
class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    // Hilt don't work for shared view model
    private val bottomNavSharedViewModel: BottomNavSharedViewModel by lazy {
        ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(BottomNavSharedViewModel::class.java)
    }
    private lateinit var binding: FragmentMainBinding

    private var searchBookmarkJob: Job? = null
    private var updateNotificationJob: Job? = null
    private var bookmarkAdapter: BookmarkAdapter? = null
    private var checkInAdapter: CheckInAdapter? = null
    private var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>? = null
    private val buttonSheetCollapseState = MutableLiveData<Boolean>()

    interface AdapterCallback {
        fun onLeave(id: Int, venueInfo: VenueInfo)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        // Bottom Sheet

        // List adapter
        checkInAdapter = CheckInAdapter(viewModel, getTouchListenerForCheckIn(binding.fragmentMainBottomSheet.checkinList),
                viewLifecycleOwner, object: AdapterCallback{
            override fun onLeave(id: Int, venueInfo: VenueInfo) {
                leaveVenue(id, venueInfo)
            }
        })
        binding.fragmentMainBottomSheet.checkinList.adapter = checkInAdapter

        // Setup bottom sheet behaviour
        val checkInButtonSheet = binding.fragmentMainBottomSheet.checkinListBottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(checkInButtonSheet).apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // hides the button on slide, or enable showing button after bottom sheet collapse
                    buttonSheetCollapseState.value = BottomSheetBehavior.STATE_COLLAPSED == newState
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

            })
        }

        // Set bottom sheet id to prevent interference from bottom sheet on scrolling
        // https://stackoverflow.com/questions/49105946/prevent-scrolls-in-bottom-sheet-ancestor-view
        AppBarBehavior.from(binding.fragmentMainAppBar.mainAppBar).apply {
            bottomSheetId = R.id.checkin_list  // this list inside bottom sheet receives touch
        }

        // subscribe checkInAdapter to list
        subscribeUi(checkInAdapter!!)

        // Bookmark list
        bookmarkAdapter = BookmarkAdapter(
                getTouchListenerForBookmark(binding.fragmentMainBookmark.bookmarkList)
        )

        val notificationAdapter = NotificationAdapter()
        val config = ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build()
        val concatAdapter = ConcatAdapter(config, notificationAdapter, bookmarkAdapter)
        binding.fragmentMainBookmark.bookmarkList.adapter = concatAdapter
        // subscribe bookmarkAdapter to list
        updateData(bookmarkAdapter!!, notificationAdapter)

        binding.fragmentMainAppBar.homeButtonLocation.setOnClickListener {
            navigateToLocationScanPage()
        }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        // Control hiding of fab: hide when either not collapsed or list empty
        binding.hideFab = buttonSheetCollapseState.combineWith(viewModel.isEmpty) { collapsed, empty -> !(collapsed!!) || (empty?:true)
        }.distinctUntilChanged()

        // fab callback
        binding.mainFab.setOnClickListener {
            val checkinList = checkInAdapter?.currentList
            if (checkinList != null && checkinList.size > 0) {
                val headerItem = checkinList.first()
                if (headerItem is CheckInUiModel.CheckInHeaderItem && headerItem.id >= 0) {
                    leaveVenue(headerItem.id, headerItem.venueInfo)
                }
            }
        }

        binding.fragmentMainAppBar.homeButtonTaxi.setOnClickListener {
            navigateToTaxiPage()
            Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Set the touch - swipe target of bottom nav to this fragment
        bottomNavSharedViewModel.setBottomNavTargetView(binding.fragmentMainCoordLayout)
        bottomNavSharedViewModel.setBottomNavHidden(false)
        return binding.root
    }

    override fun onDestroyView() {
        // Reset the touch target of bottom nav
        bottomNavSharedViewModel.setBottomNavTargetView(null)
        super.onDestroyView()

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Live data reflecting bottom sheet status
        // Set initial value. Can only be determined after restore state
        buttonSheetCollapseState.value = bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Create touch listener for swipe menu (for bookmark)
     */
    private fun getTouchListenerForBookmark(recyclerView: RecyclerView): RecyclerTouchListener {
        val touchListener = RecyclerTouchListener(activity, recyclerView)
        val itemFromPosition = { position: Int ->
            // Since the position that under the whole recyclerview (which is using ConcatAdapter)
            // we need to obtain the position in respect of the bookmark adapter, done via the
            // viewholder using bindingAdapterPosition
            val holder = recyclerView.findViewHolderForLayoutPosition(position)
            val item = holder?.let {
                bookmarkAdapter?.getBookmarkItemFromPosition(it.bindingAdapterPosition)
            }
            item
        }
        return touchListener
                .setClickable(object : RecyclerTouchListener.OnRowClickListener {
                    override fun onRowClicked(position: Int) {
                        itemFromPosition(position)?.id?.apply {
                            enterVenue(this)
                        }
                    }
                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {}
                })
                .setSwipeOptionViews(R.id.bookmark_item_action_pin, R.id.bookmark_item_action_delete)
                .setSwipeable(R.id.bookmark_item_foreground, R.id.bookmark_item_background) { viewID, position ->
                    val item = itemFromPosition(position)
                    when (viewID) {
                        R.id.bookmark_item_action_pin -> {
                            item?.apply {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    if (!pinned) {
                                        if (viewModel.pinBookmark(id)) {
                                            // Toast.makeText(context, "Bookmark pinned", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Bookmark pinned error", Toast.LENGTH_SHORT).show()
                                        }

                                    } else {
                                        if (viewModel.unpinBookmark(id)) {
                                            // Toast.makeText(context, "Bookmark unpinned", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Bookmark unpinned error", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                }

                            }

                        }
                        R.id.bookmark_item_action_delete -> {
                            item?.apply {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val result = viewModel.deleteBookmark(venueInfo)
                                    result.onSuccess {
                                        if (it > 0) {
                                            Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Not deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .setIgnoredViewTypes(NotificationAdapter.NOTIFICATION_ITEM)
    }


    /**
     * Create touch listener for swipe menu (for check in)
     */
    private fun getTouchListenerForCheckIn(recyclerView: RecyclerView): RecyclerTouchListener {
        val touchListener = RecyclerTouchListener(activity, recyclerView)
        return touchListener
                .setClickable(object : RecyclerTouchListener.OnRowClickListener {
                    override fun onRowClicked(position: Int) {
                    }
                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {}
                })
                .setSwipeOptionViews(R.id.checkin_item_action_edit, R.id.checkin_item_action_delete)
                .setSwipeable(R.id.checkin_item_foreground, R.id.checkin_item_background) { viewID, position ->
                    when (viewID) {
                        R.id.checkin_item_action_edit -> {
                            val item = checkInAdapter?.getCheckInItemFromPosition(position)
                            item?.apply {
                                // Toast.makeText(context, "Edit", Toast.LENGTH_SHORT).show()
                                navigateToVenueDetailPage(item.id)
                            }
                        }
                        R.id.checkin_item_action_delete -> {
                            val item = checkInAdapter?.getCheckInItemFromPosition(position)
                            item?.apply {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val result = viewModel.deleteVenue(item.id)
                                    result.onSuccess {
                                        if (it > 0) {
                                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Not deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .setIgnoredViewTypes(CheckInAdapter.HEADER_ITEM)
    }

    private fun enterVenue(visitHistoryId: Int) {
        val enterResult = viewModel.enterBookmarkVenue(visitHistoryId)
        enterResult.observe(this@MainFragment) { it ->
            it.onFailure { msg ->
                Timber.d("Enter failure: $msg")
                val msg1 = if (msg == MainViewModel.ALREADY_CHECKED_IN) {
                    getString(R.string.scan_already_checked_in)
                } else {
                    msg
                }
                Snackbar.make(binding.root, msg1, Snackbar.LENGTH_LONG)
                        .setDuration(SNACK_DURATION)
                        .show()
            }.onSuccess {
                navigateToScanDonePage(
                        VenueVisitInfo(it, LocaleUtil.getDisplayLang(requireActivity()))
                )
            }
        }
    }

    private fun leaveVenue(id: Int, venueInfo: VenueInfo) {
        viewModel.leaveVenueNow(id)
                .observe(viewLifecycleOwner)  {result ->
                    result.onFailure {
                        Snackbar.make(binding.root, it.localizedMessage?:"Leave Error", Snackbar.LENGTH_LONG)
                                .setDuration(SNACK_DURATION)
                                .show()
                    }
                    .onSuccess {
                        val sdf = SimpleDateFormat("HH:mm", Locale.US)
                        val timeString = sdf.format(Date())
                        val displayLang = LocaleUtil.getDisplayLang(requireContext())
                        Snackbar.make(binding.root,
                                getString(R.string.leave_success_message,
                                        timeString,
                                        LocaleUtil.getVisitLocationName(displayLang, venueInfo)
                                ),
                                Snackbar.LENGTH_LONG)
                                .setDuration(SNACK_DURATION)
                                .setAction(R.string.leave_success_edit_action){
                                    navigateToVenueDetailPage(id)
                                }
                                .show()

                    }
                }
    }

    private fun subscribeUi(adapter: CheckInAdapter) {
        viewModel.checkInList.observe(viewLifecycleOwner) { result ->
            val headerItems =
                if (result.isNotEmpty()) {
                    listOf(CheckInUiModel.CheckInHeaderItem(
                            result.first().id,
                            result.first().venueInfo,
                            if (result.size > 1) result.size - 1 else null))

                } else {
                    listOf()
                }
            adapter.submitList(
                    headerItems +
                            result.map {
                                CheckInUiModel.CheckInItem(it)
                            }
            )
        }
    }

    private fun updateData(adapter: BookmarkAdapter, notificationAdapter: NotificationAdapter) {
        // Make sure we cancel the previous job before creating a new one
        searchBookmarkJob?.cancel()
        searchBookmarkJob = lifecycleScope.launch {
            viewModel.bookmarkPagingData.map {
                    it.map { bookmark ->
                        BookmarkUiModel.BookmarkItem(bookmark) as BookmarkUiModel
                    }
            }.collect {
                adapter.submitData(it)
            }
        }

        updateNotificationJob?.cancel()
        updateNotificationJob = lifecycleScope.launch {
            combine(viewModel.lastUpdate, viewModel.unreadCount) { lastUpdate, unreadCount ->
                BookmarkUiModel.NotificationItem(lastUpdate, unreadCount)
            }.collect {
                notificationAdapter.submitList(listOf(it))
            }
        }
    }


    private fun navigateToLocationScanPage() {
        val direction =
                MainFragmentDirections.actionMainFragmentToScanCodeFragment()
        safeNavigate(direction)
    }

    private fun navigateToTaxiPage() {

    }

    private fun navigateToScanDonePage(venueVisitInfo: VenueVisitInfo) {
        val direction =
                MainFragmentDirections.actionMainFragmentToScanDoneFragment(venueVisitInfo)
        safeNavigate(direction)
    }

    private fun navigateToVenueDetailPage(visitHistoryId: Int) {
        val direction =
                MainFragmentDirections.actionMainFragmentToVenueDetailFragment(visitHistoryId)
        safeNavigate(direction)
    }


    /**
     * Check own fragment before navigate
     * See https://stackoverflow.com/questions/51060762/illegalargumentexception-navigation-destination-xxx-is-unknown-to-this-navcontr
     */
    private fun safeNavigate(direction: NavDirections) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mainFragment) {
            navController.navigate(direction)
        }
    }
}
