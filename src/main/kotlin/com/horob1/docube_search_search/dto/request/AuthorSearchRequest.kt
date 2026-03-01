package com.horob1.docube_search_search.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class AuthorSearchRequest(
    val keyword: String? = null,
    @field:Min(0) val page: Int = 0,
    @field:Min(1) @field:Max(100) val size: Int = 10,
    val role: String? = null,
    val status: String? = null
)
