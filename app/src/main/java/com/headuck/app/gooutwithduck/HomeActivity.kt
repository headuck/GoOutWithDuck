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
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import com.headuck.app.gooutwithduck.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView<ActivityHomeBinding>(this, R.layout.activity_home)

        fragment = if (savedInstanceState != null) {
            //Restore the fragment's instance
            supportFragmentManager.getFragment(savedInstanceState, HOME_PAGER_FRAGMENT_TAG);
        } else {
            HomePagerFragment().also {
                supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_host, it, HOME_PAGER_FRAGMENT_TAG)
                        .commitNow()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Save the fragment's instance
        fragment?.apply {
            supportFragmentManager.putFragment(outState, HOME_PAGER_FRAGMENT_TAG, this)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        return (fragment as HomePagerFragment?)?.onSupportNavigateUp() ?: false
    }

    companion object {
        const val HOME_PAGER_FRAGMENT_TAG = "homePagerFragment"
    }
}
