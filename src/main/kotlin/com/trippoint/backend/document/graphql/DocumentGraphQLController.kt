package com.trippoint.backend.document.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.document.domain.Document
import com.trippoint.backend.document.domain.DocumentCategory
import com.trippoint.backend.document.dto.UpdateDocumentInput
import com.trippoint.backend.document.service.DocumentService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.util.UUID

@Controller
class DocumentGraphQLController(
    private val documentService: DocumentService
) {

    @QueryMapping
    fun documents(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument filter: DocumentFilterInput?
    ): List<Document> {

        return documentService.getDocuments(
            userId = principal.userId,
            tripId = tripId,
            category = filter?.category,
            includeTrashed = filter?.includeTrashed ?: false,
            onlyFavorites = filter?.onlyFavorites ?: false,
            onlyMine = filter?.onlyMine ?: false
        )
    }

    @QueryMapping
    fun document(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID
    ): Document {

        return documentService.getDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId
        )
    }

    @MutationMapping
    fun favoriteDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID,
        @Argument favorite: Boolean
    ): Document {

        return documentService.updateFavorite(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId,
            favorite = favorite
        )
    }

    @MutationMapping
    fun trashDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID
    ): Document {

        return documentService.trashDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId
        )
    }

    @MutationMapping
    fun restoreDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID
    ): Document {

        return documentService.restoreDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId
        )
    }

    @MutationMapping
    fun permanentlyDeleteDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID
    ): Boolean {

        documentService.permanentlyDeleteDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId
        )

        return true
    }

    @MutationMapping
    fun updateDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument documentId: UUID,
        @Argument input: UpdateDocumentInput
    ): Document {

        return documentService.updateDocument(
            userId = principal.userId,
            tripId = tripId,
            documentId = documentId,
            name = input.name,
            category = input.category,
            description = input.description,
            documentNumber = input.documentNumber,
            issuedBy = input.issuedBy,
            issuedDate = input.issuedDate?.let { LocalDate.parse(it) },
            expiryDate = input.expiryDate?.let { LocalDate.parse(it) }
        )
    }
}

data class DocumentFilterInput(
    val category: DocumentCategory? = null,
    val includeTrashed: Boolean = false,
    val onlyFavorites: Boolean = false,
    val onlyMine: Boolean = false
)