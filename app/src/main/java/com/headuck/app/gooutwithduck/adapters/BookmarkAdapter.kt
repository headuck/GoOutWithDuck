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
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.MainFragment
import com.headuck.app.gooutwithduck.adapters.BookmarkDiffCallback.Companion.PAYLOAD_CHANGE_OTHER
import com.headuck.app.gooutwithduck.adapters.BookmarkDiffCallback.Companion.PAYLOAD_CHANGE_PIN
import com.headuck.app.gooutwithduck.data.BookmarkUiModel
import com.headuck.app.gooutwithduck.data.VenueInfo
import com.headuck.app.gooutwithduck.databinding.ListItemBookmarkBinding
import com.headuck.app.gooutwithduck.databinding.ListItemNotificationBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.viewmodels.MainViewModel
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.Calendar


/**
 * Adapter for the [RecyclerView] for Bookmark / Notification in [MainFragment].
 */
class BookmarkAdapter (recyclerTouchListener: RecyclerTouchListener):
    SwipePagingDataAdapter<BookmarkUiModel, RecyclerView.ViewHolder>(BookmarkDiffCallback(), recyclerTouchListener) {

    init {
        updateSwipeMenuCallback = object: UpdateSwipeMenuCallback<RecyclerView.ViewHolder> {
            override fun updateSwipeMenu(coroutineScope: CoroutineScope, holder: RecyclerView.ViewHolder) {
                if (holder is BookmarkViewHolder) {
                    holder.binding.bookmarkItem?.apply {
                        Timber.d("updateSwipeMenu $pinned")
                        val level = if (pinned) { 1 } else { 0 }
                        holder.binding.bookmarkItemImageActionPin.setImageLevel(level)
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
            BOOKMARK_ITEM ->
                BookmarkViewHolder(
                        ListItemBookmarkBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        ),
                        displayLang
                )
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
        is BookmarkUiModel.BookmarkItem -> BOOKMARK_ITEM
        is BookmarkUiModel.NotificationItem -> NOTIFICATION_ITEM
        null -> throw IllegalStateException("Unknown view")
    }

    override fun getIdFromHolder(holder: RecyclerView.ViewHolder): Int? =
        if (holder is BookmarkViewHolder) {
            holder.binding.bookmarkItem?.id
        } else {
            null
        }

    fun getBookmarkItemFromPosition(position: Int) : BookmarkUiModel.BookmarkItem? =
            when (val item = getItem(position)) {
                is BookmarkUiModel.BookmarkItem -> item
                else -> null
            }

    /**
     * Handles on bind viewholder with payloads
     * If only pinned status is changed, this would execute the change directly on the item view
     * Showing pin / unpin animation, since animate layout change is used in the layout
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty() || holder is BookmarkViewHolder) {
            return super.onBindViewHolder(holder, position, payloads)
        }
        // To ensure only pin status changed in the accumulated series of changes
        var pinChanged: Boolean = false
        var othersChanged: Boolean = false
        payloads.forEach {
            if (it is Int) {
                if (it == PAYLOAD_CHANGE_PIN) pinChanged = true
                if (it == PAYLOAD_CHANGE_OTHER) othersChanged = true
            }
        }
        if (pinChanged && !othersChanged) {
            val bookmarkUiModel = getItem(position) as BookmarkUiModel.BookmarkItem
            (holder as BookmarkViewHolder).binding.bookmarkItemImagePin.visibility =
                    if (bookmarkUiModel.pinned) {View.VISIBLE} else {View.GONE}
            // skipping normal handling of view holders
        } else {
            return super.onBindViewHolder(holder, position, payloads)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bookmarkUiModel = getItem(position)
        if (holder is BookmarkViewHolder) {
            holder.bind(bookmarkUiModel as BookmarkUiModel.BookmarkItem)
        } else if (holder is NotificationViewHolder) {
            holder.bind(bookmarkUiModel as BookmarkUiModel.NotificationItem)
        }
    }

    class BookmarkViewHolder(
            val binding: ListItemBookmarkBinding,
            private val displayLang: String
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                displayLang = this@BookmarkViewHolder.displayLang
            }
        }

        fun bind(item: BookmarkUiModel.BookmarkItem?) {
            binding.apply {
                bookmarkItem = item ?: BookmarkUiModel.BookmarkItem(
                        id = 0,
                        venueInfo = VenueInfo("None", "None", null, "IMPORT", "00000000"),
                        pinned = false
                )

                // Reset swipe menu
                if (bookmarkItemForeground.translationX != 0.0F) {
                    bookmarkItemForeground.translationX = 0.0F
                }

                bookmarkItem?.apply {
                    val level = if (pinned) {
                        1
                    } else {
                        0
                    }
                    bookmarkItemImageActionPin.setImageLevel(level)
                }
                executePendingBindings()
            }
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
        const val BOOKMARK_ITEM = 1
        const val NOTIFICATION_ITEM = 2
    }
}

private class BookmarkDiffCallback : DiffUtil.ItemCallback<BookmarkUiModel>() {

    override fun areItemsTheSame(oldItem: BookmarkUiModel, newItem: BookmarkUiModel): Boolean =
        when {
            oldItem is BookmarkUiModel.BookmarkItem && newItem is BookmarkUiModel.BookmarkItem ->
                oldItem.id == newItem.id
            oldItem is BookmarkUiModel.NotificationItem && newItem is BookmarkUiModel.NotificationItem ->
                true
            else ->
                false
        }


    override fun areContentsTheSame(oldItem: BookmarkUiModel, newItem: BookmarkUiModel): Boolean =
        when {
            oldItem is BookmarkUiModel.BookmarkItem && newItem is BookmarkUiModel.BookmarkItem ->
                oldItem.id == newItem.id &&
                oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                oldItem.venueInfo.type == newItem.venueInfo.type &&
                oldItem.exposure == newItem.exposure &&
                oldItem.pinned == newItem.pinned
            oldItem is BookmarkUiModel.NotificationItem && newItem is BookmarkUiModel.NotificationItem ->
                oldItem == newItem
            else ->
                false
        }

    override fun getChangePayload(oldItem: BookmarkUiModel, newItem: BookmarkUiModel): Any? {
        if (oldItem is BookmarkUiModel.BookmarkItem && newItem is BookmarkUiModel.BookmarkItem) {
            if (oldItem.id == newItem.id &&
                    oldItem.venueInfo.licenseNo == newItem.venueInfo.licenseNo &&
                    oldItem.venueInfo.nameEn == newItem.venueInfo.nameEn &&
                    oldItem.venueInfo.nameZh == newItem.venueInfo.nameZh &&
                    oldItem.venueInfo.type == newItem.venueInfo.type &&
                    oldItem.exposure == newItem.exposure &&
                    oldItem.pinned != newItem.pinned)  {
                return PAYLOAD_CHANGE_PIN
            }
            return PAYLOAD_CHANGE_OTHER
        }
        return super.getChangePayload(oldItem, newItem)
    }

    companion object {
        const val PAYLOAD_CHANGE_PIN = 1
        const val PAYLOAD_CHANGE_OTHER = 2
    }
}
