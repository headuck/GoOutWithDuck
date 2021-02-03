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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.MainFragment
import com.headuck.app.gooutwithduck.data.BookmarkUiModel
import com.headuck.app.gooutwithduck.databinding.ListItemNotificationBinding

import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import java.util.*

/**
 * Adapter for the [RecyclerView] for Bookmark / Notification in [MainFragment].
 */
class NotificationAdapter (): ListAdapter<BookmarkUiModel, RecyclerView.ViewHolder>(NotificationDiffCallback()) {


    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        val displayLang = LocaleUtil.getDisplayLang(parent.context)
        return when (viewType) {
            NOTIFICATION_ITEM ->
                NotificationViewHolder(
                    ListItemNotificationBinding.inflate(
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
        is BookmarkUiModel.NotificationItem -> NOTIFICATION_ITEM
        else -> throw IllegalStateException("Unknown / unsupported view")
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bookmarkUiModel = getItem(position)
        if (holder is NotificationViewHolder) {
            holder.bind(bookmarkUiModel as BookmarkUiModel.NotificationItem)
        }
    }

    class NotificationViewHolder(
            private val binding: ListItemNotificationBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                displayLang = this@NotificationViewHolder.displayLang
            }
        }

        fun bind(item: BookmarkUiModel.NotificationItem?) {
            binding.apply {
                notificationItem = item ?: BookmarkUiModel.NotificationItem(
                        outstandingInbox = 0,
                        lastUpdateDate = Calendar.getInstance()
                )
                executePendingBindings()
            }
        }
    }

    companion object {
        const val NOTIFICATION_ITEM = 3
    }
}

private class NotificationDiffCallback : DiffUtil.ItemCallback<BookmarkUiModel>() {

    override fun areItemsTheSame(oldItem: BookmarkUiModel, newItem: BookmarkUiModel): Boolean =
        when {
            oldItem is BookmarkUiModel.NotificationItem && newItem is BookmarkUiModel.NotificationItem ->
                true
            else ->
                false
        }


    override fun areContentsTheSame(oldItem: BookmarkUiModel, newItem: BookmarkUiModel): Boolean {
        return oldItem == newItem
    }
}
