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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.akexorcist.snaptimepicker.SnapTimePickerDialog
import com.akexorcist.snaptimepicker.TimeValue
import com.akexorcist.snaptimepicker.extension.SnapTimePickerUtil
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.headuck.app.gooutwithduck.databinding.FragmentVenueDetailBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.utilities.navigateUpSafe
import com.headuck.app.gooutwithduck.utilities.setBackPressHandler
import com.headuck.app.gooutwithduck.viewmodels.VenueDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject


@AndroidEntryPoint
class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()

    interface Callback {
        fun onTimePickerClick(view: View)
    }

    @Inject
    lateinit var venueDetailViewModelFactory: VenueDetailViewModel.AssistedFactory

    private val viewModel: VenueDetailViewModel by viewModels {
        VenueDetailViewModel.provideFactory(
                venueDetailViewModelFactory,
                args.visitHistoryId
        )
    }

    private lateinit var binding: FragmentVenueDetailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentVenueDetailBinding.inflate(inflater, container, false)
        binding.venueDetailToolbar.setNavigationOnClickListener {
            navigateUpSafe()
        }
        setBackPressHandler()
        context?.let { context ->
            val lang = LocaleUtil.getDisplayLang(context)
            binding.displayLang = lang
        }
        binding.callback = object: Callback {
            override fun onTimePickerClick(view: View) {
                if (view.id == R.id.venue_detail_entry_time_button) {
                    binding.visitHistory?.startDate?.apply {
                        showTimePickerDialog(this, true)
                    }
                } else if (view.id == R.id.venue_detail_exit_time_button) {
                    binding.visitHistory?.endDate?.apply {
                        showTimePickerDialog(this, false)
                    }
                } else {
                    Timber.w("View Not found")
                }
            }

        }
        viewModel.getVenue().observe(
                viewLifecycleOwner,
                { result ->
                    result.onSuccess {
                        binding.visitHistory = it
                    }.onFailure {
                        Snackbar.make(binding.root, it.message.toString(), Snackbar.LENGTH_LONG)
                                .show()
                    }

                }
        )

        SnapTimePickerUtil.observe(this) { selectedHour: Int, selectedMinute: Int ->
            onTimePicked(selectedHour, selectedMinute)
        }

        binding.venueDetailToolbar.inflateMenu(R.menu.menu_venue_detail)
        binding.venueDetailToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_delete_venue -> {
                    binding.visitHistory?.let { visitHistory ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.deleteVenue(visitHistory.id, visitHistory.autoEndDate)
                            navigateUpSafe()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        return binding.root
    }


    private fun showTimePickerDialog(time: Calendar, isEntryTime: Boolean) {
        SnapTimePickerDialog.Builder().apply {
            setPreselectedTime(TimeValue(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)))
            setThemeColor(R.color.app_theme_primary)
            setTitle(if (isEntryTime) R.string.details_entry_time else R.string.details_exit_time)
            useViewModel()
        }.build().show(childFragmentManager, SnapTimePickerDialog.TAG)
        viewModel.lastDialog = if (isEntryTime) {
            VenueDetailViewModel.ENTRY_TIME
        } else {
            VenueDetailViewModel.EXIT_TIME
        }
    }

    private fun onTimePicked(hour: Int, minute: Int) {
        binding.visitHistory?.let { visitHistory ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.changeTime(visitHistory, hour, minute)
            }
        }
    }


}