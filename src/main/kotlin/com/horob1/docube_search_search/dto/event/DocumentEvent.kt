package com.horob1.docube_search_search.dto.event

data class DocumentEvent(
    val eventType: String,       // DOCUMENT_CREATED | DOCUMENT_UPDATED | DOCUMENT_DELETED
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val content: String? = null,
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val language: String? = null,
    val status: String? = null,
    val visibility: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publishedAt: String? = null,
    val viewCount: Long? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val score: Float? = null,
    val author: AuthorPayload? = null
)

data class AuthorPayload(
    val id: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null
)

