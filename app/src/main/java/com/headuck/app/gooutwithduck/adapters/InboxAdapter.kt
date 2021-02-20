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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.data.DownloadUiModel
import com.headuck.app.gooutwithduck.data.InboxUiModel
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.databinding.ListItemDateHeaderBinding
import com.headuck.app.gooutwithduck.databinding.ListItemDownloadBinding
import com.headuck.app.gooutwithduck.databinding.ListItemInboxBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import java.util.Calendar

/**
 * Adapter for the [RecyclerView] in [InboxFragment].
 */
class InboxAdapter() :
        PagingDataAdapter<InboxUiModel, RecyclerView.ViewHolder>(InboxDiffCallback()) {


    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        val displayLang = LocaleUtil.getDisplayLang(parent.context)

        return when (viewType) {
            INBOX_ITEM ->
                InboxViewHolder(
                        ListItemInboxBinding.inflate(
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
        is InboxUiModel.InboxItem -> INBOX_ITEM
        is InboxUiModel.DateHeaderItem -> DOWNLOAD_SEPARATOR
        null -> throw IllegalStateException("Unknown view")
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val inboxUiModel = getItem(position)
        if (holder is InboxViewHolder) {
            holder.bind(inboxUiModel as InboxUiModel.InboxItem)
        } else if (holder is DateHeaderViewHolder) {
            holder.bind(inboxUiModel as InboxUiModel.DateHeaderItem)
        }
    }


    class InboxViewHolder(
            val binding: ListItemInboxBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                setClickListener {
                    binding.inboxItem?.let { inboxItem ->
                        //navigateToHistory(visitHistory, it)
                    }
                }
                displayLang = this@InboxViewHolder.displayLang
            }
        }

        fun bind(item: InboxUiModel.InboxItem?) {
            binding.apply {
                inboxItem = item ?: InboxUiModel.InboxItem(
                        id = 0,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        date = Calendar.getInstance(),
                        lastUpdate = Calendar.getInstance(),
                        bookmark = false,
                        count = 0,
                        exposure = null,
                        read = false,
                        historyId = -1
                )
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
        fun bind(item: InboxUiModel.DateHeaderItem?) {
            binding.apply {
                 dateHeader = item ?: InboxUiModel.DateHeaderItem(
                         Calendar.getInstance()
                 )
                executePendingBindings()
            }
        }
    }

    companion object {
        const val INBOX_ITEM = 1
        const val DOWNLOAD_SEPARATOR = 2
    }

}

class InboxDiffCallback : DiffUtil.ItemCallback<InboxUiModel>() {

    override fun areContentsTheSame(oldItem: InboxUiModel, newItem: InboxUiModel): Boolean =
        when {
            oldItem is InboxUiModel.InboxItem && newItem is InboxUiModel.InboxItem ->
                oldItem.id == newItem.id &&
                oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                oldItem.venueInfo.type == newItem.venueInfo.type &&
                oldItem.date == newItem.date &&
                oldItem.bookmark == newItem.bookmark &&
                oldItem.count == newItem.count &&
                oldItem.exposure == newItem.exposure
            oldItem is InboxUiModel.DateHeaderItem && newItem is InboxUiModel.DateHeaderItem ->
                oldItem.getDate() == newItem.getDate()
            else ->
                false
        }

    override fun areItemsTheSame(oldItem: InboxUiModel, newItem: InboxUiModel): Boolean =
        when {
            oldItem is InboxUiModel.InboxItem && newItem is InboxUiModel.InboxItem ->
                oldItem.id == newItem.id
            oldItem is InboxUiModel.DateHeaderItem && newItem is InboxUiModel.DateHeaderItem ->
                oldItem.getDate() == newItem.getDate()
            else ->
                false
        }

}
