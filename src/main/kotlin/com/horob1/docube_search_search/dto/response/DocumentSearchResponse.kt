package com.horob1.docube_search_search.dto.response

data class DocumentSearchResponse(
    val id: String,
    val title: String?,
    val description: String?,
    val content: String?,
    val schoolId: String?,
    val schoolName: String?,
    val facultyId: String?,
    val facultyName: String?,
    val tags: List<String>?,
    val categories: List<String>?,
    val language: String?,
    val status: String?,
    val visibility: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val publishedAt: String?,
    val viewCount: Long?,
    val likeCount: Long?,
    val commentCount: Long?,
    val score: Float?,
    val author: AuthorEmbeddedResponse?,
    val highlights: Map<String, List<String>>? = null
)

data class AuthorEmbeddedResponse(
    val id: String?,
    val username: String?,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val role: String?
)
