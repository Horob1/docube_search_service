package com.horob1.docube_search_search.controller

import com.horob1.docube_search_search.service.AuthorSearchService
import com.horob1.docube_search_search.service.DocumentSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/reindex")
class AdminReindexController(
    private val documentSearchService: DocumentSearchService,
    private val authorSearchService: AuthorSearchService
) {

    @PostMapping("/documents")
    fun reindexDocuments(): ResponseEntity<Map<String, String>> {
        val result = documentSearchService.reindexAll()
        return ResponseEntity.accepted().body(mapOf("message" to result))
    }

    @PostMapping("/authors")
    fun reindexAuthors(): ResponseEntity<Map<String, String>> {
        val result = authorSearchService.reindexAll()
        return ResponseEntity.accepted().body(mapOf("message" to result))
    }
}

