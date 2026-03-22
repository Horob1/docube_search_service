package com.horob1.docube_search_search.controller

import com.horob1.docube_search_search.dto.PageResponse
import com.horob1.docube_search_search.dto.request.AuthorSearchRequest
import com.horob1.docube_search_search.dto.request.DocumentSearchRequest
import com.horob1.docube_search_search.dto.response.AuthorSearchResponse
import com.horob1.docube_search_search.dto.response.DocumentSearchResponse
import com.horob1.docube_search_search.dto.response.SuggestResponse
import com.horob1.docube_search_search.service.AuthorSearchService
import com.horob1.docube_search_search.service.DocumentSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val documentSearchService: DocumentSearchService,
    private val authorSearchService: AuthorSearchService
) {

    // ==================== Document Search ====================

    @GetMapping("/documents")
    fun searchDocuments(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "relevance") sortBy: String,
        @RequestParam(defaultValue = "desc") order: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) authorId: String?,
        @RequestParam(name = "school_id", required = false) schoolId: String?,
        @RequestParam(name = "faculty_id", required = false) facultyId: String?
    ): ResponseEntity<PageResponse<DocumentSearchResponse>> {
        val request = DocumentSearchRequest(
            keyword = keyword,
            page = page,
            size = size,
            sortBy = sortBy,
            order = order,
            status = status,
            authorId = authorId,
            schoolId = schoolId,
            facultyId = facultyId
        )
        return ResponseEntity.ok(documentSearchService.search(request))
    }

    // ==================== Author Search ====================

    @GetMapping("/authors")
    fun searchAuthors(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<PageResponse<AuthorSearchResponse>> {
        val request = AuthorSearchRequest(
            keyword = keyword,
            page = page,
            size = size,
            role = role,
            status = status
        )
        return ResponseEntity.ok(authorSearchService.search(request))
    }

    // ==================== Suggest (Autocomplete) ====================

    @GetMapping("/documents/suggest")
    fun suggest(
        @RequestParam q: String,
        @RequestParam(defaultValue = "5") size: Int
    ): ResponseEntity<SuggestResponse> {
        return ResponseEntity.ok(documentSearchService.suggest(q, size))
    }

    // ==================== Trending Documents ====================

    @GetMapping("/documents/trending")
    fun trending(
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<DocumentSearchResponse>> {
        return ResponseEntity.ok(documentSearchService.trending(size))
    }

    // ==================== Popular Authors ====================

    @GetMapping("/authors/popular")
    fun popularAuthors(
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<AuthorSearchResponse>> {
        return ResponseEntity.ok(authorSearchService.popular(size))
    }
}
