package com.trippoint.backend.document.storage

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path

class LocalDocumentStorageTest {

    private lateinit var tempDirectory: Path
    private lateinit var storage: LocalDocumentStorage

    @BeforeEach
    fun setUp() {
        tempDirectory = Files.createTempDirectory("trippoint-documents-test")

        storage = LocalDocumentStorage(
            storagePath = tempDirectory.toString()
        )
    }

    @AfterEach
    fun tearDown() {
        tempDirectory.toFile().deleteRecursively()
    }

    @Test
    fun `store and load document`() {

        val storageKey = "trips/test-trip/documents/test.pdf"
        val content = "TripPoint document content".toByteArray()

        storage.store(
            storageKey = storageKey,
            inputStream = content.inputStream()
        )

        assertTrue(storage.exists(storageKey))

        val loaded = storage.load(storageKey).use {
            it.readBytes()
        }

        assertArrayEquals(content, loaded)
    }

    @Test
    fun `delete document`() {

        val storageKey = "trips/test-trip/documents/test.pdf"
        val content = "document".toByteArray()

        storage.store(
            storageKey = storageKey,
            inputStream = content.inputStream()
        )

        assertTrue(storage.exists(storageKey))

        storage.delete(storageKey)

        assertFalse(storage.exists(storageKey))
    }

    @Test
    fun `load missing document fails`() {

        val exception = assertThrows<IllegalArgumentException> {
            storage.load("trips/test-trip/documents/missing.pdf")
        }

        assertEquals(
            "Document file not found",
            exception.message
        )
    }

    @Test
    fun `storage key cannot escape root directory`() {

        assertThrows<IllegalArgumentException> {
            storage.store(
                storageKey = "../../outside.txt",
                inputStream = "malicious".byteInputStream()
            )
        }
    }
}