package com.trippoint.backend.document.storage

import java.io.InputStream

interface DocumentStorageService {

    fun store(
        storageKey: String,
        inputStream: InputStream
    )

    fun load(
        storageKey: String
    ): InputStream

    fun delete(
        storageKey: String
    )

    fun exists(
        storageKey: String
    ): Boolean
}