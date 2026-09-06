package com.trippoint.backend.document.repository

import com.trippoint.backend.document.domain.Document
import com.trippoint.backend.document.domain.DocumentCategory
import com.trippoint.backend.document.domain.DocumentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentRepository : JpaRepository<Document, UUID> {

    fun findAllByTripIdAndStatusOrderByCreatedAtDesc(
        tripId: UUID,
        status: DocumentStatus
    ): List<Document>

    fun findAllByTripIdAndCategoryAndStatusOrderByCreatedAtDesc(
        tripId: UUID,
        category: DocumentCategory,
        status: DocumentStatus
    ): List<Document>

    fun findAllByTripIdAndUploadedByAndStatusOrderByCreatedAtDesc(
        tripId: UUID,
        uploadedBy: UUID,
        status: DocumentStatus
    ): List<Document>

    fun findAllByTripIdAndFavoriteTrueAndStatusOrderByCreatedAtDesc(
        tripId: UUID,
        status: DocumentStatus
    ): List<Document>

    fun findByIdAndTripId(
        id: UUID,
        tripId: UUID
    ): Document?

    fun findByIdAndUploadedBy(
        id: UUID,
        uploadedBy: UUID
    ): Document?

    fun existsByStorageKey(
        storageKey: String
    ): Boolean
}