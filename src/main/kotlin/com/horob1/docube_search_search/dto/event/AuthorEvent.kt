package com.horob1.docube_search_search.dto.event

data class AuthorEvent(
    val eventType: String,       // AUTHOR_CREATED | AUTHOR_UPDATED | AUTHOR_DELETED
    val id: String,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val documentCount: Long? = null,
    val followerCount: Long? = null
)

