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

package com.tunjid.me.common.data.archive

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ArchiveEntity
import com.tunjid.me.common.data.suspendingTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

internal class LocalArchiveRepository(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) {

    private val archiveQueries = database.archiveEntityQueries
    private val archiveTagQueries = database.archiveTagEntityQueries
    private val archiveCategoryQueries = database.archiveCategoryEntityQueries
    private val archiveAuthorQueries = database.userEntityQueries

    fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        archiveQueries.find(
            kind = query.kind.type,
            limit = query.limit.toLong(),
            offset = query.offset.toLong()
        )
            .asFlow()
            .mapToList(context = dispatcher)
            .flatMapLatest { archiveEntities -> archivesFlow(archiveEntities) }

    fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive?> =
        archiveQueries.get(
            id = id,
            kind = kind.type
        )
            .asFlow()
            .mapToOneOrNull(context = dispatcher)
            .flatMapLatest { it?.let(::archiveFlow) ?: flowOf(null) }

    suspend fun saveArchives(
        archives: List<Archive>
    ) = archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
        archives
            .map(::saveArchive)
    }

    fun saveArchive(archive: Archive) {
        val userEntity = archive.author.toEntity
        val archiveEntity = archive.toEntity

        archiveAuthorQueries.upsert(
            id = userEntity.id,
            first_name = userEntity.first_name,
            last_name = userEntity.last_name,
            full_name = userEntity.full_name,
            image_url = userEntity.image_url
        )
        archiveQueries.upsert(
            id = archiveEntity.id,
            title = archiveEntity.title,
            description = archiveEntity.description,
            thumbnail = archiveEntity.thumbnail,
            body = archiveEntity.body,
            created = archiveEntity.created,
            link = archiveEntity.link,
            author = userEntity.id,
            kind = archiveEntity.kind,
        )
        archive.tags.forEach { tag ->
            archiveTagQueries.upsert(
                archive_id = archiveEntity.id,
                tag = tag,
            )
        }
        archive.categories.forEach { category ->
            archiveCategoryQueries.upsert(
                archive_id = archiveEntity.id,
                category = category,
            )
        }
    }

    private fun archivesFlow(list: List<ArchiveEntity>): Flow<List<Archive>> =
        if (list.isEmpty()) flowOf(listOf()) else combine(
            flows = list.map(::archiveFlow),
            transform = Array<Archive>::toList
        )

    private fun archiveFlow(archiveEntity: ArchiveEntity): Flow<Archive> =
        combine(
            flow = this.archiveTagQueries.find(archive_id = archiveEntity.id)
                .asFlow()
                .mapToList(context = this.dispatcher),
            flow2 = this.archiveCategoryQueries.find(archive_id = archiveEntity.id)
                .asFlow()
                .mapToList(context = this.dispatcher),
            flow3 = this.archiveAuthorQueries.find(id = archiveEntity.author)
                .asFlow()
                .mapToOne(context = this.dispatcher),
        ) { tags, categories, author ->
            Archive(
                id = archiveEntity.id,
                link = archiveEntity.link,
                title = archiveEntity.title,
                description = archiveEntity.description,
                thumbnail = archiveEntity.thumbnail,
                kind = ArchiveKind.values().first { it.type == archiveEntity.kind },
                created = Instant.fromEpochMilliseconds(archiveEntity.created),
                body = archiveEntity.body,
                author = author.toUser,
                tags = tags,
                categories = categories,
            )
        }

}