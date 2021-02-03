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

package com.headuck.app.gooutwithduck.di

import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

// AssistedInject puts all assisted bindings in the same module.
// We need to make a decision about where to install it.
// In this case, as we only need it in fragments, we install it there.
// source: https://gist.github.com/manuelvicnt/437668cda3a891d347e134b1de29aee1
@InstallIn(FragmentComponent::class)
@AssistedModule
@Module(includes = [AssistedInject_AssistedInjectModule::class])
// Needed until AssistedInject is incorporated into Dagger
interface AssistedInjectModule
