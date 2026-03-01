package com.horob1.docube_search_search.dto.response

data class AuthorSearchResponse(
    val id: String,
    val username: String?,
    val displayName: String?,
    val email: String?,
    val phoneNumber: String?,
    val avatarUrl: String?,
    val bio: String?,
    val role: String?,
    val status: String?,
    val createdAt: String?,
    val documentCount: Long?,
    val followerCount: Long?,
    val highlights: Map<String, List<String>>? = null
)

