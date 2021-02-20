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
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.databinding.ListItemDateHeaderBinding
import com.headuck.app.gooutwithduck.databinding.ListItemDownloadBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import java.util.Calendar

/**
 * Adapter for the [RecyclerView] in [DownloadFragment].
 */
class DownloadAdapter() :
        PagingDataAdapter<DownloadUiModel, RecyclerView.ViewHolder>(DownloadDiffCallback()) {


    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        val displayLang = LocaleUtil.getDisplayLang(parent.context)

        return when (viewType) {
            DOWNLOAD_ITEM ->
                DownloadViewHolder(
                        ListItemDownloadBinding.inflate(
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
        is DownloadUiModel.DownloadItem -> DOWNLOAD_ITEM
        is DownloadUiModel.DateHeaderItem -> DOWNLOAD_SEPARATOR
        null -> throw IllegalStateException("Unknown view")
    }

    fun getDownloadItemFromPosition(position: Int) : DownloadUiModel.DownloadItem? =
        when (val item = getItem(position)) {
            is DownloadUiModel.DownloadItem -> item
            else -> null
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val downloadUiModel = getItem(position)
        if (holder is DownloadViewHolder) {
            holder.bind(downloadUiModel as DownloadUiModel.DownloadItem)
        } else if (holder is DateHeaderViewHolder) {
            holder.bind(downloadUiModel as DownloadUiModel.DateHeaderItem)
        }
    }


    class DownloadViewHolder(
            val binding: ListItemDownloadBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                setClickListener {
                    binding.downloadItem?.let { downloadItem ->
                        //navigateToHistory(visitHistory, it)
                    }
                }
                displayLang = this@DownloadViewHolder.displayLang
            }
        }

        fun bind(item: DownloadUiModel.DownloadItem?) {
            binding.apply {
                downloadItem = item ?: DownloadUiModel.DownloadItem(
                        id = 0,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        startDate = Calendar.getInstance(),
                        endDate = Calendar.getInstance(),
                        batchDate = Calendar.getInstance(),
                        numCase = 0,
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
        fun bind(item: DownloadUiModel.DateHeaderItem?) {
            binding.apply {
                 dateHeader = item ?: DownloadUiModel.DateHeaderItem(
                         Calendar.getInstance()
                 )
                executePendingBindings()
            }
        }
    }

    companion object {
        const val DOWNLOAD_ITEM = 1
        const val DOWNLOAD_SEPARATOR = 2
    }

}

class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadUiModel>() {

    override fun areContentsTheSame(oldItem: DownloadUiModel, newItem: DownloadUiModel): Boolean =
        when {
            oldItem is DownloadUiModel.DownloadItem && newItem is DownloadUiModel.DownloadItem ->
                oldItem.id == newItem.id &&
                oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                oldItem.venueInfo.type == newItem.venueInfo.type &&
                oldItem.startDate == newItem.startDate &&
                oldItem.endDate == newItem.endDate &&
                oldItem.batchDate == newItem.batchDate &&
                oldItem.numCase == newItem.numCase
            oldItem is DownloadUiModel.DateHeaderItem && newItem is DownloadUiModel.DateHeaderItem ->
                oldItem.getDate() == newItem.getDate()
            else ->
                false
        }

    override fun areItemsTheSame(oldItem: DownloadUiModel, newItem: DownloadUiModel): Boolean =
        when {
            oldItem is DownloadUiModel.DownloadItem && newItem is DownloadUiModel.DownloadItem ->
                oldItem.id == newItem.id
            oldItem is DownloadUiModel.DateHeaderItem && newItem is DownloadUiModel.DateHeaderItem ->
                oldItem.getDate() == newItem.getDate()
            else ->
                false
        }

}
