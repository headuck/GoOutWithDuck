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

package com.headuck.app.gooutwithduck.utilities

import android.content.Intent
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.MenuItem
import android.view.View
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.headuck.app.gooutwithduck.R
import com.headuck.app.gooutwithduck.views.BottomNavigationViewCust
import timber.log.Timber

/**
 * Manages the various graphs needed for a [BottomNavigationViewCust].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
fun BottomNavigationViewCust.setupWithNavController(
        navGraphId: Int,
        itemIds: List<Int>, // should be all unique
        startDestinationIds: List<Int>, // should be all unique
        fragmentManager: FragmentManager,
        containerId: Int,
        intent: Intent,
        itemSelectedListener: ((item: MenuItem) -> Unit)?,
        newFragment: Boolean
): LiveData<NavController> {

    // Map of tags
    val startDestIdToTagMap = SparseArray<String>()
    val itemIdToStartDestIdMap = SparseIntArray()
    val startDestIdToItemIdMap = SparseIntArray()


    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    var firstFragmentStartDestId = 0

    val getNavController: (navController: NavController, itemId: Int) -> NavController = { navController, itemId ->
        navController.apply {
            try {
                graph.id
            } catch (e: IllegalStateException) {
                Timber.d("Inflate navcontroller graph")
                graph = navInflater.inflate(navGraphId).apply {
                    startDestination = itemIdToStartDestIdMap[itemId]
                }
            }
        }
    }

    // First create a NavHostFragment for each NavGraph ID
    startDestinationIds.forEachIndexed { index, startDestId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                containerId,
        )

        if (index == 0) {
            firstFragmentStartDestId = startDestId
        }

        // Save to the map
        startDestIdToTagMap[startDestId] = fragmentTag
        // Create map for itemId <-> startDestId conversion
        itemIdToStartDestIdMap[itemIds[index]] = startDestId
        startDestIdToItemIdMap[startDestId] = itemIds[index]

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (this.selectedItemId == itemIds[index]) {
            // Update livedata with the selected graph
            // Timber.d("Setup")
            selectedNavController.value = getNavController(navHostFragment.navController, this.selectedItemId)
            attachNavHostFragment(fragmentManager, navHostFragment, index == 0)

        } else {
            detachNavHostFragment(fragmentManager, navHostFragment)
        }
    }

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag = startDestIdToTagMap[itemIdToStartDestIdMap[this.selectedItemId]]
    // val firstFragmentTag = startDestIdToTagMap[firstFragmentStartDestId]
    // var isOnFirstFragment = selectedItemTag == firstFragmentTag

    // When a navigation item is selected
    setOnNavigationItemSelectedListener { item ->
        // Don't do anything if the state is state has already been saved.
        if (fragmentManager.isStateSaved) {
            false
        } else {
            val newlySelectedItemTag = startDestIdToTagMap[itemIdToStartDestIdMap[item.itemId]]
            if (selectedItemTag != newlySelectedItemTag) {
                // Pop everything above the first fragment (the "fixed start destination")
                //fragmentManager.popBackStack(firstFragmentTag,
                //        FragmentManager.POP_BACK_STACK_INCLUSIVE)
                val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                        as NavHostFragment

                // Exclude the first fragment tag because it's always in the back stack.
                //if (firstFragmentTag != newlySelectedItemTag) {
                    // Commit a transaction that cleans the back stack and adds the first fragment
                    // to it, creating the fixed started destination.
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(
                                    R.anim.nav_default_enter_anim,
                                    R.anim.nav_default_exit_anim,
                                    R.anim.nav_default_pop_enter_anim,
                                    R.anim.nav_default_pop_exit_anim)
                            .setPrimaryNavigationFragment(selectedFragment)
                            .attach(selectedFragment)
                            .apply {
                                // Detach all other Fragments
                                startDestIdToTagMap.forEach { _, fragmentTagIter ->
                                    if (fragmentTagIter != newlySelectedItemTag) {
                                        Timber.d("Detach $fragmentTagIter")
                                        detach(fragmentManager.findFragmentByTag(fragmentTagIter)!!)
                                    }
                                }
                            }
                            //.addToBackStack(firstFragmentTag)
                            .setReorderingAllowed(true)
                            .commit()
                //}
                selectedItemTag = newlySelectedItemTag
                // isOnFirstFragment = selectedItemTag == firstFragmentTag
                selectedNavController.value = getNavController(selectedFragment.navController, item.itemId)

                if (itemSelectedListener != null) {
                    itemSelectedListener(item)
                }
                true
            } else {
                false
            }
        }
    }
    if (newFragment) {
        // This is needed to avoid NavHostFragment throwing error during onDestroyView
        // where it expects the parent has the navController set
        // under NavHostFragment this is set during onCreateView but this setupWithNavController
        // is called after restoring saved state when selected id is restored.
        val selectedFragment = fragmentManager.findFragmentByTag(selectedItemTag)
                    as NavHostFragment
        // Timber.d("Selected frag type = " + selectedFragment.javaClass.simpleName)
        val parentView = selectedFragment.view?.parent
        parentView?.apply {
            // Timber.d("Parent view id = " + (parentView as View).id + " frag id: "  + selectedFragment.id)
            Navigation.setViewNavController(this as View, selectedNavController.value)
        }
    }

    // Optional: on item reselected, pop back stack to the destination of the graph
    setupItemReselected(startDestIdToTagMap, itemIdToStartDestIdMap, fragmentManager)

    // Handle deep link
    setupDeepLinks(navGraphId, startDestinationIds, startDestIdToItemIdMap, fragmentManager, containerId, intent)

    // Finally, ensure that we update our BottomNavigationView when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        Timber.d("back stack changed")
        //if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
        //    this.selectedItemId = startDestIdToItemIdMap[firstFragmentStartDestId]
        //    Timber.d("set selected item id")
        //}

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                Timber.d("nav to start destination")
                controller.navigate(controller.graph.startDestination)
            }
        }
    }
    return selectedNavController
}

private fun BottomNavigationViewCust.setupDeepLinks(
        navGraphId: Int,
        startDestinationIds: List<Int>,
        startDestIdToItemIdMap: SparseIntArray,
        fragmentManager: FragmentManager,
        containerId: Int,
        intent: Intent
) {
    startDestinationIds.forEachIndexed { index, startDestinationId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                containerId
        )
        // Handle Intent
        val targetItemId = startDestIdToItemIdMap[startDestinationId]

        if (navHostFragment.navController.handleDeepLink(intent)
                && selectedItemId != targetItemId) {
            this.selectedItemId = targetItemId
        }
    }
}

private fun BottomNavigationViewCust.setupItemReselected(
        startDestIdToTagMap: SparseArray<String>,
        itemIdToStartDestIdMap: SparseIntArray,
        fragmentManager: FragmentManager
) {
    setOnNavigationItemReselectedListener { item ->
        val newlySelectedItemTag = startDestIdToTagMap[itemIdToStartDestIdMap[item.itemId]]
        val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment
        val navController = selectedFragment.navController
        // Pop the back stack to the start destination of the current navController graph
        try {
            val destination = navController.graph.startDestination
            navController.popBackStack(destination, false)
        } catch (e: IllegalStateException) {
            Timber.w("Graph not yet set for nav controller")
        }
    }
}

private fun detachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
            .detach(navHostFragment)
            .commitNow()
}

private fun attachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment,
        isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
            .attach(navHostFragment)
            .apply {
                if (isPrimaryNavFragment) {
                    setPrimaryNavigationFragment(navHostFragment)
                }
            }
            .commitNow()

}

private fun obtainNavHostFragment(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(0)

    fragmentManager.beginTransaction()
            .add(containerId, navHostFragment, fragmentTag)
            .commitNow()
    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

fun getFragmentTag(index: Int) = "bottomNavigation#$index"
