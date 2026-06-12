/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.storage

import io.element.android.libraries.core.data.ByteUnit
import io.element.android.libraries.core.data.megaBytes
import io.element.android.libraries.matrix.impl.paths.SessionPaths
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.SqliteStoreBuilder as SdkSqliteStoreBuilder

interface SqliteStoreBuilder {
    fun passphrase(passphrase: String?): SqliteStoreBuilder
    fun setupClientBuilder(clientBuilder: ClientBuilder): ClientBuilder
}

class RustSqliteStoreBuilder(
    sessionPaths: SessionPaths,
) : SqliteStoreBuilder {
    // 32MB SQLite page cache: positive value = number of 4KB pages (8192 pages × 4KB = 32MB)
    private val cacheSizePages = 8192u
    // Connection pool matches the session dispatcher parallelism
    private val poolMaxSize = 4u

    private var inner = SdkSqliteStoreBuilder(
        dataPath = sessionPaths.fileDirectory.absolutePath,
        cachePath = sessionPaths.cacheDirectory.absolutePath,
    )
        .journalSizeLimit(25.megaBytes.into(ByteUnit.BYTES).toUInt())
        .cacheSize(cacheSizePages)
        .poolMaxSize(poolMaxSize)

    override fun passphrase(passphrase: String?): SqliteStoreBuilder {
        inner = inner.passphrase(passphrase)
        return this
    }

    override fun setupClientBuilder(clientBuilder: ClientBuilder): ClientBuilder {
        return clientBuilder.sqliteStore(this.inner)
    }
}
