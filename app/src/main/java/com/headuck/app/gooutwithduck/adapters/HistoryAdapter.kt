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

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.R
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.data.VisitHistoryUiModel
import com.headuck.app.gooutwithduck.databinding.ListItemDateHeaderBinding
import com.headuck.app.gooutwithduck.databinding.ListItemHistoryBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.viewmodels.HistoryListViewModel
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import kotlinx.coroutines.CoroutineScope
import java.util.*

/**
 * Adapter for the [RecyclerView] in [HistoryListFragment].
 */
class HistoryAdapter(private val viewModel: HistoryListViewModel, recyclerTouchListener: RecyclerTouchListener, lifecycleOwner: LifecycleOwner) :
        SwipePagingDataAdapter<VisitHistoryUiModel, RecyclerView.ViewHolder>(HistoryDiffCallback(), recyclerTouchListener) {

    var lastBookmarkLevel = -1

    init {
        updateSwipeMenuCallback = object: UpdateSwipeMenuCallback<RecyclerView.ViewHolder> {
            override fun updateSwipeMenu(coroutineScope: CoroutineScope, holder: RecyclerView.ViewHolder) {
                if (holder is HistoryViewHolder) {
                    holder.binding.visitHistory?.apply {
                        if (venueInfo.licenseNo == null) {
                            viewModel.getBookmarkCountByTypeAndVenueId(venueInfo.type, venueInfo.venueId)
                                    .asLiveData(coroutineScope.coroutineContext)
                                    .observe(
                                            lifecycleOwner,
                                            {
                                                val level = if (it == 0) 0 else 1
                                                holder.binding.historyItemImageActionBookmark.setImageLevel(level)
                                                lastBookmarkLevel = level
                                            }
                                    )
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        val displayLang = LocaleUtil.getDisplayLang(parent.context)

        return when (viewType) {
            VISIT_HISTORY_ITEM ->
                HistoryViewHolder(
                        ListItemHistoryBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        ),
                        displayLang
                )
            else ->
                DateHeaderViewHolder(
                        ListItemDateHeaderBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        ),
                        displayLang
                )
        }

    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is VisitHistoryUiModel.VisitHistoryItem -> VISIT_HISTORY_ITEM
        is VisitHistoryUiModel.DateHeaderItem -> VISIT_HISTORY_SEPARATOR
        null -> throw IllegalStateException("Unknown view")
    }

    override fun getIdFromHolder(holder: RecyclerView.ViewHolder): Int? =
        if (holder is HistoryViewHolder) {
            holder.binding.visitHistory?.id
        } else {
            null
        }

    fun getHistoryItemFromPosition(position: Int) : VisitHistoryUiModel.VisitHistoryItem? =
        when (val item = getItem(position)) {
            is VisitHistoryUiModel.VisitHistoryItem -> item
            else -> null
        }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val visitHistoryUiModel = getItem(position)
        if (holder is HistoryViewHolder) {
            holder.bind(visitHistoryUiModel as VisitHistoryUiModel.VisitHistoryItem)
        } else if (holder is DateHeaderViewHolder) {
            holder.bind(visitHistoryUiModel as VisitHistoryUiModel.DateHeaderItem)
        }
    }


    class HistoryViewHolder(
            val binding: ListItemHistoryBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                setClickListener {
                    binding.visitHistory?.let { visitHistory ->
                        //navigateToHistory(visitHistory, it)
                    }
                }
                displayLang = this@HistoryViewHolder.displayLang
            }
        }

        /*private fun navigateToPlant(
            plant: Plant,
            view: View
        ) {
            val direction =
                HomeViewPagerFragmentDirections.actionViewPagerFragmentToPlantDetailFragment(
                    plant.plantId
                )
            view.findNavController().navigate(direction)
        }
        */
        fun bind(item: VisitHistoryUiModel.VisitHistoryItem?) {
            binding.apply {
                visitHistory = item ?: VisitHistoryUiModel.VisitHistoryItem(
                        id = 0,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        startDate = Calendar.getInstance(),
                        endDate = Calendar.getInstance(),
                        autoEndDate = null,
                        exposure = null,
                )
                // Reset swipe menu
                if (historyItemForeground.translationX != 0.0F) {
                    historyItemForeground.translationX = 0.0F
                }
                historyItemImageActionBookmark.setImageLevel(0)
                val tint = if (item?.venueInfo?.licenseNo == null) {
                    historyItemActionBookmark.isClickable = true
                    ContextCompat.getColor(itemView.context, R.color.app_color_gray_50)

                } else {
                    historyItemActionBookmark.isClickable = false
                    ContextCompat.getColor(itemView.context, R.color.app_color_inverse_text_disabled)
                }
                ImageViewCompat.setImageTintList(historyItemImageActionBookmark, ColorStateList.valueOf(tint));
                executePendingBindings()
            }
        }
    }

    class DateHeaderViewHolder(
            private val binding: ListItemDateHeaderBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                displayLang = this@DateHeaderViewHolder.displayLang
            }
        }
        fun bind(item: VisitHistoryUiModel.DateHeaderItem?) {
            binding.apply {
                 dateHeader = item ?: VisitHistoryUiModel.DateHeaderItem(
                         Calendar.getInstance()
                 )
                executePendingBindings()
            }
        }
    }

    companion object {
        const val VISIT_HISTORY_ITEM = 1
        const val VISIT_HISTORY_SEPARATOR = 2
    }

}

class HistoryDiffCallback : DiffUtil.ItemCallback<VisitHistoryUiModel>() {

    override fun areContentsTheSame(oldItem: VisitHistoryUiModel, newItem: VisitHistoryUiModel): Boolean =
        when {
            oldItem is VisitHistoryUiModel.VisitHistoryItem && newItem is VisitHistoryUiModel.VisitHistoryItem ->
                oldItem.id == newItem.id &&
                oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                oldItem.venueInfo.type == newItem.venueInfo.type &&
                oldItem.startDate == newItem.startDate &&
                oldItem.endDate == newItem.endDate &&
                oldItem.autoEndDate == newItem.autoEndDate &&
                oldItem.exposure == newItem.exposure
            oldItem is VisitHistoryUiModel.DateHeaderItem && newItem is VisitHistoryUiModel.DateHeaderItem ->
                oldItem.date == newItem.date
            else ->
                false
        }

    override fun areItemsTheSame(oldItem: VisitHistoryUiModel, newItem: VisitHistoryUiModel): Boolean =
        when {
            oldItem is VisitHistoryUiModel.VisitHistoryItem && newItem is VisitHistoryUiModel.VisitHistoryItem ->
                oldItem.id == newItem.id
            oldItem is VisitHistoryUiModel.DateHeaderItem && newItem is VisitHistoryUiModel.DateHeaderItem ->
                oldItem.date == newItem.date
            else ->
                false
        }

}
