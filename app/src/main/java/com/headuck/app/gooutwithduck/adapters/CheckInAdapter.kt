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

package com.headuck.app.gooutwithduck.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.MainFragment
import com.headuck.app.gooutwithduck.data.CheckInUiModel
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.databinding.ListItemCheckinBinding
import com.headuck.app.gooutwithduck.databinding.ListItemCheckinHeaderBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.viewmodels.MainViewModel
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import timber.log.Timber
import java.util.*

/**
 * Adapter for the [RecyclerView] for Check in locations in [MainFragment].
 */
class CheckInAdapter (private val viewModel: MainViewModel,
                      recyclerTouchListener: RecyclerTouchListener,
                      private val parentLifecycleOwner: LifecycleOwner, // The parent lifecycleOwner is used
                      private val callback: MainFragment.AdapterCallback
                      ): //ListAdapter<CheckInUiModel, RecyclerView.ViewHolder>(CheckInDiffCallback()) {
                      SwipeListAdapter<CheckInUiModel, RecyclerView.ViewHolder>(CheckInDiffCallback(), recyclerTouchListener) {

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        val displayLang = LocaleUtil.getDisplayLang(parent.context)
        return when (viewType) {
            CHECK_IN_ITEM ->
                 CheckInViewHolder(
                     ListItemCheckinBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                    ),
                    displayLang,
                    callback
                )
            HEADER_ITEM ->
                HeaderViewHolder(
                    ListItemCheckinHeaderBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                    ),
                    displayLang
                )
            else ->
               throw IllegalStateException("Unsupported type $viewType")
        }

    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is CheckInUiModel.CheckInItem -> CHECK_IN_ITEM
        is CheckInUiModel.CheckInHeaderItem -> HEADER_ITEM
        null -> throw IllegalStateException("Unknown view")
    }

    override fun getIdFromHolder(holder: RecyclerView.ViewHolder): Int? =
            if (holder is CheckInViewHolder) {
                holder.binding.checkInItem?.id
            } else {
                null
            }

    fun getCheckInItemFromPosition(position: Int) : CheckInUiModel.CheckInItem? =
            when (val item = getItem(position)) {
                is CheckInUiModel.CheckInItem -> item
                else -> null
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val checkInUiModel = getItem(position)
        if (holder is CheckInViewHolder) {
            holder.bind(checkInUiModel as CheckInUiModel.CheckInItem)
        } else if (holder is HeaderViewHolder) {
            holder.bind(checkInUiModel as CheckInUiModel.CheckInHeaderItem)
        }
    }

    class CheckInViewHolder(
        val binding: ListItemCheckinBinding,
        private val displayLang: String,
        private val callback: MainFragment.AdapterCallback
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                displayLang = this@CheckInViewHolder.displayLang
            }
        }

        fun bind(item: CheckInUiModel.CheckInItem?) {
            binding.apply {
                checkInItem = item ?: CheckInUiModel.CheckInItem(
                        id = 0,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        startDate = Calendar.getInstance(),
                        autoEndDate = null,

                )

                // Note: in this case the parent lifecycle owner is used - the save action would not cancel
                // when the check in item scrolls out and the item would not render feedback directly.
                // In general the viewholder may be made the lifecycle owner.
                setClickListener { view ->
                    binding.clickListener?.let {
                        item?.apply {
                            callback.onLeave(item.id, item.venueInfo)
                        }

                    }
                }
                executePendingBindings()
            }
        }
    }

    class HeaderViewHolder(
            private val binding: ListItemCheckinHeaderBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                displayLang = this@HeaderViewHolder.displayLang
            }
        }

        fun bind(item: CheckInUiModel.CheckInHeaderItem?) {
            binding.apply {
                Timber.d("Item : venueinfo  %s", item?.venueInfo)
                checkInHeaderItem = item ?: CheckInUiModel.CheckInHeaderItem(
                        id = -1,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        others = null
                )
                executePendingBindings()
            }
        }
    }

    companion object {
        const val CHECK_IN_ITEM = 1
        const val HEADER_ITEM = 2
    }
}

private class CheckInDiffCallback : DiffUtil.ItemCallback<CheckInUiModel>() {

    override fun areItemsTheSame(oldItem: CheckInUiModel, newItem: CheckInUiModel): Boolean =
        when {
            oldItem is CheckInUiModel.CheckInItem && newItem is CheckInUiModel.CheckInItem ->
                oldItem.id == newItem.id
            oldItem is CheckInUiModel.CheckInHeaderItem && newItem is CheckInUiModel.CheckInHeaderItem ->
                true
            else ->
                false
        }


    override fun areContentsTheSame(oldItem: CheckInUiModel, newItem: CheckInUiModel): Boolean =
        when {
            oldItem is CheckInUiModel.CheckInItem && newItem is CheckInUiModel.CheckInItem ->
                oldItem.id == newItem.id &&
                        oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                        oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                        oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                        oldItem.venueInfo.type == newItem.venueInfo.type &&
                        oldItem.startDate == newItem.startDate &&
                        oldItem.autoEndDate == newItem.autoEndDate
            oldItem is CheckInUiModel.CheckInHeaderItem && newItem is CheckInUiModel.CheckInHeaderItem ->
                oldItem.id == newItem.id &&
                        oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                        oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                        oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh
            else ->
                false
        }

}
