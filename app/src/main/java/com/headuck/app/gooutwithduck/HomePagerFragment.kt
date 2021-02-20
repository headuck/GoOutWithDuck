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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.observe
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.headuck.app.gooutwithduck.databinding.FragmentHomePagerBinding
import com.headuck.app.gooutwithduck.utilities.setupWithNavController
import com.headuck.app.gooutwithduck.viewmodels.BottomNavSharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomePagerFragment : Fragment() {

    lateinit var binding: FragmentHomePagerBinding
    private var currentNavController: LiveData<NavController>? = null
    private var backToMainEnabled = false
    // Hilt don't work for shared view model
    private val bottomNavSharedViewModel: BottomNavSharedViewModel by lazy {
        ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(BottomNavSharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomePagerBinding.inflate(inflater, container, false)

        bottomNavSharedViewModel.targetView.observe(viewLifecycleOwner) {
            binding.homePagerBottomNavigation.setTouchTarget(it)
        }

        bottomNavSharedViewModel.hidden.distinctUntilChanged().observe(viewLifecycleOwner) {
            it?.apply {
                if (this) {
                    // hide
                    slideDown(binding.homePagerBottomNavigation)
                } else {
                    slideUp(binding.homePagerBottomNavigation)
                }
            }
        }

        if (savedInstanceState != null) {
            if (bottomNavSharedViewModel.hidden.value == true) {
                binding.homePagerBottomNavigation.visibility = View.GONE
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if (backToMainEnabled) {
                    binding.homePagerBottomNavigation.selectedItemId = R.id.page_main
                } else {
                    requireActivity().finish()
                }
            }
        })

        return binding.root
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar(newFragment: Boolean) {
        val bottomNavigation = binding.homePagerBottomNavigation

        val graphId = R.navigation.nav_main
        val itemIds = listOf(R.id.page_main, R.id.page_inbox, R.id.page_history , R.id.page_download)
        val startIds = listOf(R.id.mainFragment, R.id.inboxFragment, R.id.historyFragment, R.id.downloadFragment)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigation.setupWithNavController(
                navGraphId = graphId,
                itemIds = itemIds,
                startDestinationIds = startIds,
                fragmentManager = childFragmentManager,
                containerId = R.id.home_pager_fragment_container,
                intent = requireActivity().intent,
                itemSelectedListener = { item ->
                    showHideAppBar(item.itemId)
                },
                newFragment = newFragment
        )

        currentNavController = controller
    }


    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar(savedInstanceState == null)

        val bottomNavigation = binding.homePagerBottomNavigation
        showHideAppBar(bottomNavigation.selectedItemId)
    }
    private fun showHideAppBar(itemId: Int) {
        backToMainEnabled = itemId != R.id.page_main
    }

    private fun slideUp(child: BottomNavigationView) {
        val height = child.measuredHeight
        child.translationY = height.toFloat()
        child.visibility = View.VISIBLE
        child.clearAnimation()
        child.animate().translationY(0f).duration = 250
    }

    private fun slideDown(child: BottomNavigationView) {
        child.clearAnimation()
        val height = child.measuredHeight
        child.animate().withEndAction { child.visibility = View.GONE  }.translationY(height.toFloat()).duration = 250
    }

    fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

}
