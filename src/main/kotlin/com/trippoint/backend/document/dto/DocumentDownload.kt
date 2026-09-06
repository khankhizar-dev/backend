package com.trippoint.backend.document.dto

import java.io.InputStream

data class DocumentDownload(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val inputStream: InputStream
)
