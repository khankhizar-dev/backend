package com.trippoint.backend.document.dto

import com.trippoint.backend.document.domain.DocumentCategory

data class UpdateDocumentInput(
    val name: String? = null,
    val category: DocumentCategory? = null,
    val description: String? = null,
    val documentNumber: String? = null,
    val issuedBy: String? = null,
    val issuedDate: String? = null,
    val expiryDate: String? = null
)
