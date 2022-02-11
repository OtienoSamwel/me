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

package com.tunjid.me.common.ui.archiveedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.common.app.LocalAppDependencies
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.globalui.InsetFlags
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.archivelist.ArchiveRoute
import com.tunjid.me.common.ui.utilities.InitialUiState
import com.tunjid.treenav.MultiStackNav
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveEditRoute(
    val kind: ArchiveKind,
    val archiveId: String?
) : AppRoute<ArchiveEditMutator> {
    override val id: String
        get() = "archive-edit-$kind-$archiveId"

    @Composable
    override fun Render() {
        ArchiveEditScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }

    override fun navRailRoute(nav: MultiStackNav): AppRoute<*>? {
        val activeStack = nav.stacks.getOrNull(nav.currentIndex) ?: return null
        val previous = activeStack.routes.getOrNull(activeStack.routes.lastIndex - 1)
        return if (previous is ArchiveRoute) previous else null
    }
}

@Composable
private fun ArchiveEditScreen(mutator: ArchiveEditMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Archive Edit",
            navVisibility = NavVisibility.GoneIfBottomNav,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )


    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState),
    ) {
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(
            value = state.title,
            maxLines = 2,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontSize = 24.sp
            ),
            label = { Text(text = "Title", fontSize = 24.sp) },
            onValueChange = { mutator.accept(Action.TextEdit.Title(it)) }
        )
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(
            value = state.description,
            maxLines = 2,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontSize = 18.sp
            ),
            label = { Text(text = "Description", fontSize = 18.sp) },
            onValueChange = { mutator.accept(Action.TextEdit.Description(it)) }
        )

        TextField(
            value = state.body,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            label = { Text(text = "Body") },
            onValueChange = { mutator.accept(Action.TextEdit.Body(it)) }
        )
        Spacer(modifier = Modifier.padding(8.dp + navBarSizeDp))
    }
}

@Composable
private fun Unstyled() = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colors.onSurface,
)