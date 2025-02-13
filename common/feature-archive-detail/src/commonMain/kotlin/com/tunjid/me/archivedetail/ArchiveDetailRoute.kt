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

package com.tunjid.me.archivedetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.RemoteImagePainter
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.currentUiState
import com.tunjid.me.scaffold.globalui.rememberFunction
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.LocalNavigator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveDetailRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveDetailScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }

    override fun navRailRoute(nav: MultiStackNav): AppRoute? {
        val activeStack = nav.stacks.getOrNull(nav.currentIndex) ?: return null
        val previous = activeStack.routes
            .getOrNull(activeStack.routes.lastIndex - 1) as? AppRoute
            ?: return null
        return if (previous.id == "archives/${kind.type}") previous else null
    }
}

@Composable
private fun ArchiveDetailScreen(mutator: ArchiveDetailMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    val canEdit = state.canEdit

    val navigator = LocalNavigator.current
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = state.archive?.title ?: "Detail",
            navVisibility = NavVisibility.GoneIfBottomNav,
            // Prevents UI from jittering as load starts
            fabShows = if (state.hasFetchedAuthStatus) canEdit else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Edit",
            fabIcon = Icons.Default.Edit,
            fabClickListener = rememberFunction(state.archive?.id) {
                val archiveId = state.archive?.id
                if (archiveId != null) navigator.navigate {
                    currentNav.push("archives/${state.kind.type}/${archiveId.value}/edit".toRoute)
                }
            },
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    val archive = state.archive

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState),
    ) {
        Spacer(modifier = Modifier.padding(16.dp))

        val painter = RemoteImagePainter(state.archive?.thumbnail)

        if (painter != null) Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
        )

        Chips(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            name = "Categories:",
            chips = state.archive?.categories?.map(Descriptor.Category::value) ?: listOf(),
            color = MaterialTheme.colors.primaryVariant,
        )

        Spacer(modifier = Modifier.padding(16.dp))

        MaterialRichText(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (archive != null) Markdown(
                content = archive.body
            )
        }

        Spacer(modifier = Modifier.padding(16.dp))

        Chips(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            name = "Tags:",
            chips = state.archive?.tags?.map(Descriptor.Tag::value) ?: listOf(),
            color = MaterialTheme.colors.secondary,
        )

        Spacer(modifier = Modifier.padding(64.dp + navBarSizeDp))
    }

    // Pop nav if this archive does not exist anymore
    val wasDeleted = state.wasDeleted
    LaunchedEffect(wasDeleted) {
        if (wasDeleted) navigator.navigate { currentNav.pop() }
    }
}
