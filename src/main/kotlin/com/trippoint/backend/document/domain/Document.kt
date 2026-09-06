package com.trippoint.backend.document.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "documents",
    indexes = [
        Index(
            name = "idx_documents_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_documents_uploaded_by",
            columnList = "uploaded_by"
        ),
        Index(
            name = "idx_documents_trip_status",
            columnList = "trip_id,status"
        ),
        Index(
            name = "idx_documents_trip_category",
            columnList = "trip_id,category"
        ),
        Index(
            name = "idx_documents_trip_favorite",
            columnList = "trip_id,is_favorite"
        ),
        Index(
            name = "idx_documents_expiry_date",
            columnList = "expiry_date"
        ),
        Index(
            name = "idx_documents_created_at",
            columnList = "created_at"
        )
    ]
)
class Document(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "uploaded_by", nullable = false)
    var uploadedBy: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "original_file_name", nullable = false, length = 255)
    var originalFileName: String,

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    var storageKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: DocumentCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var source: DocumentSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DocumentStatus = DocumentStatus.ACTIVE,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "document_number", length = 100)
    var documentNumber: String? = null,

    @Column(name = "issued_by", length = 255)
    var issuedBy: String? = null,

    @Column(name = "issued_date")
    var issuedDate: LocalDate? = null,

    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null,

    @Column(name = "is_favorite", nullable = false)
    var favorite: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}