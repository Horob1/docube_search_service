package com.horob1.docube_search_search.dto.response

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?
)

