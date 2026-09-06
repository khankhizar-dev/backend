package com.trippoint.backend.document.service

import com.trippoint.backend.document.domain.Document
import com.trippoint.backend.document.domain.DocumentCategory
import com.trippoint.backend.document.domain.DocumentSource
import com.trippoint.backend.document.domain.DocumentStatus
import com.trippoint.backend.document.dto.DocumentDownload
import com.trippoint.backend.document.repository.DocumentRepository
import com.trippoint.backend.document.storage.DocumentStorageService
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentStorageService: DocumentStorageService,
    private val tripAccessService: TripAccessService
) {

    companion object {
        private const val MAX_FILE_SIZE = 20L * 1024L * 1024L

        private val ALLOWED_MIME_TYPES = setOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }

    @Transactional
    fun uploadDocument(
        userId: UUID,
        tripId: UUID,
        name: String,
        originalFileName: String,
        mimeType: String,
        fileSize: Long,
        category: DocumentCategory,
        source: DocumentSource,
        description: String?,
        documentNumber: String?,
        issuedBy: String?,
        issuedDate: LocalDate?,
        expiryDate: LocalDate?,
        inputStream: InputStream
    ): Document {

        tripAccessService.requireMemberAccess(tripId, userId)

        validateFile(
            originalFileName = originalFileName,
            mimeType = mimeType,
            fileSize = fileSize
        )

        val normalizedName = name.trim()

        require(normalizedName.isNotEmpty()) {
            "Document name cannot be empty"
        }

        require(normalizedName.length <= 255) {
            "Document name cannot exceed 255 characters"
        }

        require(expiryDate == null || issuedDate == null || !expiryDate.isBefore(issuedDate)) {
            "Expiry date cannot be before issued date"
        }

        val documentId = UUID.randomUUID()

        val storageKey = buildStorageKey(
            tripId = tripId,
            documentId = documentId,
            originalFileName = originalFileName
        )

        documentStorageService.store(
            storageKey = storageKey,
            inputStream = inputStream
        )

        try {
            val document = Document(
                id = documentId,
                tripId = tripId,
                uploadedBy = userId,
                name = normalizedName,
                originalFileName = originalFileName,
                mimeType = mimeType,
                fileSize = fileSize,
                storageKey = storageKey,
                category = category,
                source = source,
                status = DocumentStatus.ACTIVE,
                description = description?.trim(),
                documentNumber = documentNumber?.trim(),
                issuedBy = issuedBy?.trim(),
                issuedDate = issuedDate,
                expiryDate = expiryDate,
                favorite = false
            )

            return documentRepository.save(document)
        } catch (exception: Exception) {
            documentStorageService.delete(storageKey)
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun getDocuments(
        userId: UUID,
        tripId: UUID,
        category: DocumentCategory? = null,
        includeTrashed: Boolean = false,
        onlyFavorites: Boolean = false,
        onlyMine: Boolean = false
    ): List<Document> {

        tripAccessService.requireMemberAccess(tripId, userId)

        val status = if (includeTrashed) {
            DocumentStatus.TRASHED
        } else {
            DocumentStatus.ACTIVE
        }

        return when {
            onlyFavorites -> {
                documentRepository
                    .findAllByTripIdAndFavoriteTrueAndStatusOrderByCreatedAtDesc(
                        tripId,
                        status
                    )
            }

            category != null -> {
                documentRepository
                    .findAllByTripIdAndCategoryAndStatusOrderByCreatedAtDesc(
                        tripId,
                        category,
                        status
                    )
            }

            onlyMine -> {
                documentRepository
                    .findAllByTripIdAndUploadedByAndStatusOrderByCreatedAtDesc(
                        tripId,
                        userId,
                        status
                    )
            }

            else -> {
                documentRepository
                    .findAllByTripIdAndStatusOrderByCreatedAtDesc(
                        tripId,
                        status
                    )
            }
        }
    }

    @Transactional(readOnly = true)
    fun getDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID
    ): Document {

        tripAccessService.requireMemberAccess(tripId, userId)

        return documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")
    }

    @Transactional
    fun updateFavorite(
        userId: UUID,
        tripId: UUID,
        documentId: UUID,
        favorite: Boolean
    ): Document {

        tripAccessService.requireMemberAccess(tripId, userId)

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        require(document.status == DocumentStatus.ACTIVE) {
            "Trashed document cannot be modified"
        }

        document.favorite = favorite

        return documentRepository.save(document)
    }

    @Transactional
    fun trashDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID
    ): Document {

        tripAccessService.requireMemberAccess(tripId, userId)

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        if (document.uploadedBy != userId) {
            tripAccessService.requireOwnerAccess(tripId, userId)
        }

        require(document.status == DocumentStatus.ACTIVE) {
            "Document is already in trash"
        }

        document.status = DocumentStatus.TRASHED

        return documentRepository.save(document)
    }

    @Transactional
    fun restoreDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID
    ): Document {

        tripAccessService.requireMemberAccess(tripId, userId)

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        if (document.uploadedBy != userId) {
            tripAccessService.requireOwnerAccess(tripId, userId)
        }

        require(document.status == DocumentStatus.TRASHED) {
            "Document is not in trash"
        }

        document.status = DocumentStatus.ACTIVE

        return documentRepository.save(document)
    }

    @Transactional
    fun permanentlyDeleteDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID
    ) {

        tripAccessService.requireMemberAccess(tripId, userId)

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        if (document.uploadedBy != userId) {
            tripAccessService.requireOwnerAccess(tripId, userId)
        }

        documentStorageService.delete(document.storageKey)

        documentRepository.delete(document)
    }

    private fun validateFile(
        originalFileName: String,
        mimeType: String,
        fileSize: Long
    ) {
        require(originalFileName.isNotBlank()) {
            "Original file name cannot be empty"
        }

        require(originalFileName.length <= 255) {
            "Original file name cannot exceed 255 characters"
        }

        require(fileSize > 0) {
            "File must not be empty"
        }

        require(fileSize <= MAX_FILE_SIZE) {
            "File size cannot exceed 20 MB"
        }

        require(mimeType in ALLOWED_MIME_TYPES) {
            "Unsupported document type: $mimeType"
        }
    }

    private fun buildStorageKey(
        tripId: UUID,
        documentId: UUID,
        originalFileName: String
    ): String {

        val extension = originalFileName
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.isNotBlank() }

        return if (extension != null) {
            "trips/$tripId/documents/$documentId.$extension"
        } else {
            "trips/$tripId/documents/$documentId"
        }
    }

    @Transactional(readOnly = true)
    fun downloadDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID
    ): DocumentDownload {

        tripAccessService.requireMemberAccess(
            tripId,
            userId
        )

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        require(document.status == DocumentStatus.ACTIVE) {
            "Document is in trash"
        }

        require(documentStorageService.exists(document.storageKey)) {
            "Document file not found"
        }

        return DocumentDownload(
            fileName = document.originalFileName,
            contentType = document.mimeType,
            fileSize = document.fileSize,
            inputStream = documentStorageService.load(document.storageKey)
        )
    }

    @Transactional
    fun updateDocument(
        userId: UUID,
        tripId: UUID,
        documentId: UUID,
        name: String?,
        category: DocumentCategory?,
        description: String?,
        documentNumber: String?,
        issuedBy: String?,
        issuedDate: LocalDate?,
        expiryDate: LocalDate?
    ): Document {

        tripAccessService.requireMemberAccess(
            tripId,
            userId
        )

        val document = documentRepository.findByIdAndTripId(
            documentId,
            tripId
        ) ?: throw IllegalArgumentException("Document not found")

        require(document.status == DocumentStatus.ACTIVE) {
            "Trashed document cannot be modified"
        }

        if (document.uploadedBy != userId) {
            tripAccessService.requireOwnerAccess(
                tripId,
                userId
            )
        }

        name?.let {
            val normalizedName = it.trim()

            require(normalizedName.isNotEmpty()) {
                "Document name cannot be empty"
            }

            require(normalizedName.length <= 255) {
                "Document name cannot exceed 255 characters"
            }

            document.name = normalizedName
        }

        category?.let {
            document.category = it
        }

        description?.let {
            document.description = it.trim().ifEmpty { null }
        }

        documentNumber?.let {
            document.documentNumber = it.trim().ifEmpty { null }
        }

        issuedBy?.let {
            document.issuedBy = it.trim().ifEmpty { null }
        }

        issuedDate?.let {
            document.issuedDate = it
        }

        expiryDate?.let {
            document.expiryDate = it
        }

        require(
            document.expiryDate == null ||
                    document.issuedDate == null ||
                    !document.expiryDate!!.isBefore(document.issuedDate)
        ) {
            "Expiry date cannot be before issued date"
        }

        return documentRepository.save(document)
    }
}