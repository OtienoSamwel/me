/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.globalui.slices

import com.tunjid.me.globalui.InsetDescriptor
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.bottomNavVisible

internal data class BottomNavPositionalState(
    val insetDescriptor: InsetDescriptor,
    val bottomNavVisible: Boolean,
    val navBarSize: Int
)

internal val UiState.bottomNavPositionalState
    get() = BottomNavPositionalState(
        bottomNavVisible = bottomNavVisible,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )