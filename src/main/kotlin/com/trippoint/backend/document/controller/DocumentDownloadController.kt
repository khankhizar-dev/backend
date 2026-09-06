package com.trippoint.backend.document.controller

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.document.service.DocumentService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
class DocumentDownloadController(
    private val documentService: DocumentService
) {

    @GetMapping("/{documentId}/download")
    fun downloadDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable documentId: UUID,
        @RequestParam tripId: UUID
    ): ResponseEntity<InputStreamResource> {

        val document = documentService.downloadDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId
        )

        val resource = InputStreamResource(
            document.inputStream
        )

        return ResponseEntity.ok()
            .contentType(
                MediaType.parseMediaType(document.contentType)
            )
            .contentLength(document.fileSize)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"${document.fileName}\""
            )
            .body(resource)
    }
}