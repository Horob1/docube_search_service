package com.horob1.docube_search_search.mapper

import com.horob1.docube_search_search.dto.event.DocumentEvent
import com.horob1.docube_search_search.dto.response.AuthorEmbeddedResponse
import com.horob1.docube_search_search.dto.response.DocumentSearchResponse
import com.horob1.docube_search_search.entity.DocumentIndex
import com.horob1.docube_search_search.entity.EmbeddedAuthor
import org.springframework.stereotype.Component

@Component
class DocumentMapper {

    fun toResponse(doc: DocumentIndex, highlights: Map<String, List<String>>? = null): DocumentSearchResponse {
        return DocumentSearchResponse(
            id = doc.id,
            title = doc.title,
            description = doc.description,
            content = doc.content,
            schoolId = doc.schoolId,
            schoolName = doc.schoolName,
            facultyId = doc.facultyId,
            facultyName = doc.facultyName,
            tags = doc.tags,
            categories = doc.categories,
            language = doc.language,
            status = doc.status,
            visibility = doc.visibility,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            publishedAt = doc.publishedAt,
            viewCount = doc.viewCount,
            likeCount = doc.likeCount,
            commentCount = doc.commentCount,
            score = doc.score,
            author = doc.author?.let {
                AuthorEmbeddedResponse(
                    id = it.id,
                    username = it.username,
                    displayName = it.displayName,
                    email = it.email,
                    avatarUrl = it.avatarUrl,
                    role = it.role
                )
            },
            highlights = highlights
        )
    }

    fun fromEvent(event: DocumentEvent): DocumentIndex {
        return DocumentIndex(
            id = event.id,
            title = event.title,
            description = event.description,
            content = event.content,
            schoolId = event.schoolId,
            schoolName = event.schoolName,
            facultyId = event.facultyId,
            facultyName = event.facultyName,
            tags = event.tags,
            categories = event.categories,
            language = event.language,
            status = event.status,
            visibility = event.visibility,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt,
            publishedAt = event.publishedAt,
            viewCount = event.viewCount,
            likeCount = event.likeCount,
            commentCount = event.commentCount,
            score = event.score,
            author = event.author?.let {
                EmbeddedAuthor(
                    id = it.id,
                    username = it.username,
                    displayName = it.displayName,
                    email = it.email,
                    avatarUrl = it.avatarUrl,
                    role = it.role
                )
            }
        )
    }
}
