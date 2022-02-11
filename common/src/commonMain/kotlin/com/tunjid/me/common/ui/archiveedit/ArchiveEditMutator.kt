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


import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.model.ArchiveId
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.model.ArchiveUpsert
import com.tunjid.me.common.data.model.Descriptor
import com.tunjid.me.common.data.model.Result
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.globalui.navBarSize
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias ArchiveEditMutator = Mutator<Action, StateFlow<State>>

fun archiveEditMutator(
    scope: CoroutineScope,
    route: ArchiveEditRoute,
    initialState: State? = null,
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    appMutator: AppMutator,
): ArchiveEditMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(
        kind = route.kind,
        upsert = ArchiveUpsert(id = route.archiveId),
        navBarSize = appMutator.globalUiMutator.state.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = { actions ->
        merge(
            appMutator.globalUiMutator.state
                .map { it.navBarSize }
                .map { Mutation { copy(navBarSize = it) } },
            authRepository.isSignedIn.map { Mutation { copy(isSignedIn = it) } },
            route.archiveId?.let {
                archiveRepository.textBodyMutations(
                    kind = route.kind,
                    archiveId = it
                )
            } ?: emptyFlow(),
            actions.toMutationStream(keySelector = Action::key) {
                when (val action = type()) {
                    is Action.TextEdit -> action.flow.textEditMutations()
                    is Action.ChipEdit -> action.flow.chipEditMutations()
                    is Action.Submit -> action.flow.submissionMutations(
                        archiveRepository = archiveRepository
                    )
                }
            }
        ).monitorWhenActive(appMutator)
    },
)

private fun ArchiveRepository.textBodyMutations(
    kind: ArchiveKind,
    archiveId: ArchiveId
): Flow<Mutation<State>> = monitorArchive(
    kind = kind,
    id = archiveId
).map { archive ->
    Mutation {
        copy(
            upsert = upsert.copy(
                title = archive.title,
                description = archive.description,
                body = archive.body,
                categories = archive.categories,
                tags = archive.tags,
            )
        )
    }
}

private fun Flow<Action.TextEdit>.textEditMutations(): Flow<Mutation<State>> =
    map { it.mutation }

private fun Flow<Action.ChipEdit>.chipEditMutations(): Flow<Mutation<State>> =
    map { (chipAction, descriptor) ->
        Mutation {
            if (descriptor.value.isBlank()) return@Mutation this
            val (updatedUpsert, updatedChipsState) = when (chipAction) {
                ChipAction.Added -> Pair(
                    upsert.copy(
                        categories = when (descriptor) {
                            is Descriptor.Category -> (upsert.categories + descriptor).distinct()
                            else -> upsert.categories
                        },
                        tags = when (descriptor) {
                            is Descriptor.Tag -> (upsert.tags + descriptor).distinct()
                            else -> upsert.tags
                        },
                    ),
                    chipsState.copy(
                        categoryText = when (descriptor) {
                            is Descriptor.Category -> Descriptor.Category(value = "")
                            else -> chipsState.categoryText
                        },
                        tagText = when (descriptor) {
                            is Descriptor.Tag -> Descriptor.Tag(value = "")
                            else -> chipsState.tagText
                        },
                    )
                )
                is ChipAction.Changed -> Pair(
                    upsert,
                    chipsState.copy(
                        categoryText = when (descriptor) {
                            is Descriptor.Category -> descriptor
                            else -> chipsState.categoryText
                        },
                        tagText = when (descriptor) {
                            is Descriptor.Tag -> descriptor
                            else -> chipsState.tagText
                        }
                    )
                )
                is ChipAction.Removed -> Pair(
                    upsert.copy(
                        categories = upsert.categories.filter { it != descriptor },
                        tags = upsert.tags.filter { it != descriptor },
                    ),
                    chipsState
                )
            }
            copy(
                upsert = updatedUpsert,
                chipsState = updatedChipsState
            )
        }
    }

private fun Flow<Action.Submit>.submissionMutations(
    archiveRepository: ArchiveRepository
): Flow<Mutation<State>> =
    debounce(200)
        .flatMapLatest { (kind, upsert) ->
            flow<Mutation<State>> {
                emit(Mutation { copy(isSubmitting = true) })
                // TODO: Show snack bar if error
                val result = archiveRepository.upsert(kind = kind, upsert = upsert)
                emit(Mutation { copy(isSubmitting = false) })

                // Start monitoring the created archive
                if (upsert.id == null && result is Result.Success) emitAll(
                    archiveRepository.textBodyMutations(
                        kind = kind,
                        archiveId = result.item
                    )
                )
            }
        }