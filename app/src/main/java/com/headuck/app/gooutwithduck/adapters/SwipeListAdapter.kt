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


import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.headuck.app.gooutwithduck.views.RecyclerTouchListener
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import timber.log.Timber

abstract class SwipeListAdapter<T : Any, VH: RecyclerView.ViewHolder>(diffCallback: DiffUtil.ItemCallback<T>,
                                                                      private val recyclerTouchListener: RecyclerTouchListener)

    : ListAdapter<T, VH>(diffCallback) {

    interface UpdateSwipeMenuCallback<VH> {
        fun updateSwipeMenu (coroutineScope: CoroutineScope, holder: VH)
    }

    var updateSwipeMenuCallback: UpdateSwipeMenuCallback<VH>? = null

    private var job: Job? = null
    private var jobItemId: Int? = null

    abstract fun getIdFromHolder(holder: VH): Int?


    fun setRecyclerTouchListener(recyclerTouchListener: RecyclerTouchListener) {
        recyclerTouchListener.apply {
            registerAdapterDataObserver(dataObserver)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerTouchListener.setOnSwipeOptionRevealListener(object: RecyclerTouchListener.OnSwipeOptionRevealListener {
            override fun onSwipeOptionsReveal(bgView: View, position: Int) {
                recyclerView.findContainingViewHolder(bgView)?.apply {
                    listenBgView(this as VH)

                }
            }

            override fun onSwipeOptionsHide(bgView: View, position: Int) {
                recyclerView.findContainingViewHolder(bgView)?.apply {
                    stopBgView(this as VH)
                }
            }

        })
        recyclerView.addOnItemTouchListener(recyclerTouchListener)

    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        recyclerTouchListener.apply {
            if (!ignoredViewTypes.contains(holder.itemViewType)) {
                if (getIdFromHolder(holder) == jobItemId) {
                    job?.cancel()
                    job = null
                    if (jobItemId != null) {
                        Timber.d("Job Cancel due to recycled: %s", jobItemId)
                        jobItemId = null
                    }
                }
            }
        }

    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnItemTouchListener(recyclerTouchListener)
        job?.cancel()
        job = null
        if (jobItemId != null) {
            Timber.d("Job Cancel due to detach: %s", jobItemId)
            jobItemId = null
        }
    }

    fun listenBgView(holder: VH) {
        recyclerTouchListener.apply {
            if (!ignoredViewTypes.contains(holder.itemViewType)) {
                updateSwipeMenuCallback?.apply {
                    job?.cancel()
                    if (jobItemId != null) {
                        Timber.d("Job Cancel due to set another: %s", jobItemId)
                    }
                    job = Job()
                    jobItemId = getIdFromHolder(holder)
                    this.updateSwipeMenu(CoroutineScope(job as CompletableJob), holder);
                }
            }
        }
    }

    fun stopBgView(holder: VH) {
        recyclerTouchListener.apply {
            if (!ignoredViewTypes.contains(holder.itemViewType)) {
                job?.cancel()
                job = null
                if (jobItemId != null) {
                    Timber.d("Job Cancel due to close: %s", jobItemId)
                    jobItemId = null
                }
            }
        }
    }
}