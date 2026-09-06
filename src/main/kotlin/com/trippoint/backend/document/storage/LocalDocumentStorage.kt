package com.trippoint.backend.document.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class LocalDocumentStorage(
    @Value("\${trippoint.document.storage-path:./storage/documents}")
    private val storagePath: String
) : DocumentStorageService {

    private val rootPath: Path by lazy {
        Paths.get(storagePath).toAbsolutePath().normalize()
    }

    override fun store(
        storageKey: String,
        inputStream: InputStream
    ) {
        val targetPath = resolvePath(storageKey)

        Files.createDirectories(targetPath.parent)

        inputStream.use { input ->
            Files.copy(
                input,
                targetPath
            )
        }
    }

    override fun delete(
        storageKey: String
    ) {
        val targetPath = resolvePath(storageKey)

        Files.deleteIfExists(targetPath)
    }

    override fun exists(
        storageKey: String
    ): Boolean {
        return Files.exists(resolvePath(storageKey))
    }

    private fun resolvePath(storageKey: String): Path {
        val resolved = rootPath
            .resolve(storageKey)
            .normalize()

        require(resolved.startsWith(rootPath)) {
            "Invalid storage key"
        }

        return resolved
    }

    override fun load(
        storageKey: String
    ): InputStream {

        val targetPath = resolvePath(storageKey)

        require(Files.exists(targetPath)) {
            "Document file not found"
        }

        return Files.newInputStream(targetPath)
    }
}