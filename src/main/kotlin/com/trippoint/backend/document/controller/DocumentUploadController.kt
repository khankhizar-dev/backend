package com.trippoint.backend.document.controller

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.document.domain.Document
import com.trippoint.backend.document.domain.DocumentCategory
import com.trippoint.backend.document.domain.DocumentSource
import com.trippoint.backend.document.service.DocumentService
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
class DocumentUploadController(
    private val documentService: DocumentService
) {

    @PostMapping(
        "/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadDocument(
        @AuthenticationPrincipal principal: UserPrincipal,

        @RequestParam
        tripId: UUID,

        @RequestParam
        name: String,

        @RequestParam
        category: DocumentCategory,

        @RequestParam
        source: DocumentSource,

        @RequestPart("file")
        file: MultipartFile,

        @RequestParam(required = false)
        description: String?,

        @RequestParam(required = false)
        documentNumber: String?,

        @RequestParam(required = false)
        issuedBy: String?,

        @RequestParam(required = false)
        issuedDate: String?,

        @RequestParam(required = false)
        expiryDate: String?
    ): Document {

        return documentService.uploadDocument(
            userId = principal.userId,
            tripId = tripId,
            name = name,
            originalFileName = file.originalFilename ?: "document",
            mimeType = file.contentType ?: "application/octet-stream",
            fileSize = file.size,
            category = category,
            source = source,
            description = description,
            documentNumber = documentNumber,
            issuedBy = issuedBy,
            issuedDate = issuedDate?.let { LocalDate.parse(it) },
            expiryDate = expiryDate?.let { LocalDate.parse(it) },
            inputStream = file.inputStream
        )
    }
}