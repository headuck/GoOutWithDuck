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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.headuck.app.gooutwithduck.databinding.FragmentVenueDetailBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.utilities.navigateUpSafe
import com.headuck.app.gooutwithduck.utilities.setBackPressHandler
import com.headuck.app.gooutwithduck.viewmodels.VenueDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()

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
        viewModel.getVenue().observe(
                viewLifecycleOwner,
                { result ->
                        result.onSuccess {
                            context?.let { context ->
                                val lang = LocaleUtil.getDisplayLang(context)
                                binding.venueDetailVenueName.text = LocaleUtil.getVisitLocationName(lang, it.venueInfo)
                                binding.venueDetailVenueType.text = it.venueInfo.type
                                binding.venueDetailVisitDate.text = LocaleUtil.getVisitLocationDate(lang, it.startDate)
                                binding.venueDetailEntryTime.text = LocaleUtil.getVisitLocationTime(lang, it.startDate)
                                binding.venueDetailExitTimeRow.visibility = if (it.endDate != null) {View.VISIBLE} else {View.GONE}
                                it.endDate?.apply {
                                    binding.venueDetailExitTime.text = LocaleUtil.getVisitLocationTime(lang, it.endDate)
                                }
                            }
                        }.onFailure {
                            Snackbar.make(binding.root, it.message.toString(), Snackbar.LENGTH_LONG)
                                    .show()
                        }

                }
        )
        return binding.root
    }


}