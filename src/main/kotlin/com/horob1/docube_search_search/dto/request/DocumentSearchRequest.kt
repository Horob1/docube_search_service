package com.horob1.docube_search_search.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

data class DocumentSearchRequest(
    val keyword: String? = null,
    @field:Min(0) val page: Int = 0,
    @field:Min(1) @field:Max(100) val size: Int = 10,
    @field:Pattern(regexp = "relevance|createdAt|viewCount", message = "sortBy must be: relevance, createdAt, or viewCount")
    val sortBy: String = "relevance",
    @field:Pattern(regexp = "asc|desc", message = "order must be: asc or desc")
    val order: String = "desc",
    val status: String? = null,
    val authorId: String? = null,
    val schoolId: String? = null,
    val facultyId: String? = null
)
