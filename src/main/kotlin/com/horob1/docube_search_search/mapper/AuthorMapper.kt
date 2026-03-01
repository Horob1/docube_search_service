package com.horob1.docube_search_search.mapper

import com.horob1.docube_search_search.dto.event.AuthorEvent
import com.horob1.docube_search_search.dto.response.AuthorSearchResponse
import com.horob1.docube_search_search.entity.AuthorIndex
import org.springframework.stereotype.Component

@Component
class AuthorMapper {

    fun toResponse(author: AuthorIndex, highlights: Map<String, List<String>>? = null): AuthorSearchResponse {
        return AuthorSearchResponse(
            id = author.id,
            username = author.username,
            displayName = author.displayName,
            email = author.email,
            phoneNumber = author.phoneNumber,
            avatarUrl = author.avatarUrl,
            bio = author.bio,
            role = author.role,
            status = author.status,
            createdAt = author.createdAt,
            documentCount = author.documentCount,
            followerCount = author.followerCount,
            highlights = highlights
        )
    }

    fun fromEvent(event: AuthorEvent): AuthorIndex {
        return AuthorIndex(
            id = event.id,
            username = event.username,
            displayName = event.displayName,
            email = event.email,
            phoneNumber = event.phoneNumber,
            avatarUrl = event.avatarUrl,
            bio = event.bio,
            role = event.role,
            status = event.status,
            createdAt = event.createdAt,
            documentCount = event.documentCount,
            followerCount = event.followerCount
        )
    }
}

