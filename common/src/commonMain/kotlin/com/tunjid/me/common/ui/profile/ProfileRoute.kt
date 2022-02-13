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

package com.tunjid.me.common.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.LocalAppDependencies
import com.tunjid.me.common.globalui.InsetFlags
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.common.RemoteImagePainter
import com.tunjid.me.common.globalui.ScreenUiState
import com.tunjid.me.common.ui.common.FormField
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute : AppRoute<ProfileMutator> {
    override val id: String
        get() = "Profile"

    @Composable
    override fun Render() {
        ProfileScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
private fun ProfileScreen(mutator: ProfileMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()

    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Profile",
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontallydd,
    ) {
        val user = state.signedInUser
        val painter = if (user != null) RemoteImagePainter(user.imageUrl) else null

        Spacer(modifier = Modifier.height(32.dp))
        val modifier = Modifier
            .padding(horizontal = 16.dp)
            .size(96.dp)

        if (user != null && painter != null) Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        ) else Image(
            imageVector = Icons.Default.Person,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )

        state.fields.forEach { field ->
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                modifier = Modifier
                    .fillMaxWidth(0.6f),
                field = field,
                onValueChange = {
                    mutator.accept(Action.FieldChanged(field = field.copy(value = it)))
                }
            )
        }
    }
}
