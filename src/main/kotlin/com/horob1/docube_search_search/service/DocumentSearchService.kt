package com.horob1.docube_search_search.service

import com.horob1.docube_search_search.dto.PageResponse
import com.horob1.docube_search_search.dto.request.DocumentSearchRequest
import com.horob1.docube_search_search.dto.response.DocumentSearchResponse
import com.horob1.docube_search_search.dto.response.SuggestResponse
import com.horob1.docube_search_search.entity.DocumentIndex
import com.horob1.docube_search_search.entity.EmbeddedAuthor
import com.horob1.docube_search_search.exception.SearchException
import com.horob1.docube_search_search.mapper.DocumentMapper
import com.horob1.docube_search_search.repository.DocumentSearchRepository
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters
import org.springframework.stereotype.Service

@Service
class DocumentSearchService(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val documentSearchRepository: DocumentSearchRepository,
    private val documentMapper: DocumentMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BULK_SIZE = 500
    }

    fun search(request: DocumentSearchRequest): PageResponse<DocumentSearchResponse> {
        try {
            val pageable = PageRequest.of(request.page, request.size)
            val queryBuilder = NativeQuery.builder()

            // Full-text search across document and organization fields.
            if (!request.keyword.isNullOrBlank()) {
                queryBuilder.withQuery { q ->
                    q.bool { b ->
                        b.must { m ->
                            m.multiMatch { mm ->
                                mm.query(request.keyword)
                                    .fields(
                                        "title^6",
                                        "school_name^3",
                                        "faculty_name^3",
                                        "content^1"
                                    )
                                    .type(TextQueryType.BestFields)
                                    .fuzziness("AUTO")
                            }
                        }
                        if (!request.status.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("status").value(request.status) } }
                        }
                        if (!request.authorId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("author.id").value(request.authorId) } }
                        }
                        if (!request.schoolId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("school_id").value(request.schoolId) } }
                        }
                        if (!request.facultyId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("faculty_id").value(request.facultyId) } }
                        }
                        b
                    }
                }
            } else {
                queryBuilder.withQuery { q ->
                    q.bool { b ->
                        b.must { m -> m.matchAll { it } }
                        if (!request.status.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("status").value(request.status) } }
                        }
                        if (!request.authorId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("author.id").value(request.authorId) } }
                        }
                        if (!request.schoolId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("school_id").value(request.schoolId) } }
                        }
                        if (!request.facultyId.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("faculty_id").value(request.facultyId) } }
                        }
                        b
                    }
                }
            }

            // Sorting
            val sort = when (request.sortBy.lowercase()) {
                "createdat" -> Sort.by(
                    if (request.order.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC,
                    "createdAt"
                )
                "viewcount" -> Sort.by(
                    if (request.order.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC,
                    "viewCount"
                )
                else -> Sort.by(Sort.Direction.DESC, "_score")
            }

            // Highlight
            val highlight = Highlight(
                HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .withNumberOfFragments(3)
                    .withFragmentSize(150)
                    .build(),
                listOf(
                    HighlightField("title"),
                    HighlightField("content"),
                    HighlightField("school_name"),
                    HighlightField("faculty_name")
                )
            )

            queryBuilder
                .withPageable(pageable)
                .withSort(sort)
                .withHighlightQuery(HighlightQuery(highlight, DocumentIndex::class.java))

            val query = queryBuilder.build()
            val searchHits: SearchHits<DocumentIndex> = elasticsearchOperations.search(query, DocumentIndex::class.java)

            val totalHits = searchHits.totalHits
            val totalPages = if (totalHits == 0L) 0 else ((totalHits + request.size - 1) / request.size).toInt()

            val content = searchHits.searchHits.map { hit ->
                val highlightMap = hit.highlightFields.filter { it.value.isNotEmpty() }
                documentMapper.toResponse(hit.content, highlightMap)
            }

            return PageResponse(
                content = content,
                page = request.page,
                size = request.size,
                totalElements = totalHits,
                totalPages = totalPages,
                hasNext = request.page < totalPages - 1
            )
        } catch (ex: Exception) {
            throw SearchException("Error searching documents: ${ex.message}", ex)
        }
    }

    @Cacheable(value = ["suggest"], key = "#prefix + '_' + #size")
    fun suggest(prefix: String, size: Int = 5): SuggestResponse {
        val query = NativeQuery.builder()
            .withQuery { q ->
                q.matchPhrasePrefix { mpp ->
                    mpp.field("title").query(prefix)
                }
            }
            .withPageable(PageRequest.of(0, size))
            .build()

        val hits = elasticsearchOperations.search(query, DocumentIndex::class.java)
        val suggestions = hits.searchHits.mapNotNull { it.content.title }.distinct()
        return SuggestResponse(suggestions)
    }

    @Cacheable(value = ["trending"], key = "#size")
    fun trending(size: Int = 10): List<DocumentSearchResponse> {
        val query = NativeQuery.builder()
            .withQuery { q ->
                q.bool { b ->
                    b.must { m -> m.matchAll { it } }
                    b.filter { f -> f.term { t -> t.field("status").value("PUBLISHED") } }
                    b
                }
            }
            .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
            .withPageable(PageRequest.of(0, size))
            .build()

        val hits = elasticsearchOperations.search(query, DocumentIndex::class.java)
        return hits.searchHits.map { documentMapper.toResponse(it.content) }
    }

    @CacheEvict(value = ["trending", "suggest"], allEntries = true)
    fun index(document: DocumentIndex) {
        elasticsearchOperations.save(document)
        log.info("Indexed document: {}", document.id)
    }

    @CacheEvict(value = ["trending", "suggest"], allEntries = true)
    fun delete(id: String) {
        documentSearchRepository.deleteById(id)
        log.info("Deleted document: {}", id)
    }

    fun updateAuthorInDocuments(authorId: String, updatedAuthor: EmbeddedAuthor) {
        log.info("Updating author {} in all documents", authorId)

        var page = 0
        var totalUpdated = 0

        do {
            val query = NativeQuery.builder()
                .withQuery { q -> q.term { t -> t.field("author.id").value(authorId) } }
                .withPageable(PageRequest.of(page, BULK_SIZE))
                .build()

            val hits = elasticsearchOperations.search(query, DocumentIndex::class.java)

            if (hits.searchHits.isEmpty()) break

            val updatedDocs = hits.searchHits.map { hit ->
                hit.content.copy(author = updatedAuthor)
            }

            elasticsearchOperations.save(updatedDocs)
            totalUpdated += updatedDocs.size
            page++

            log.debug("Bulk updated {} documents (batch {})", updatedDocs.size, page)
        } while (hits.searchHits.size == BULK_SIZE)

        log.info("Updated total {} documents for author {}", totalUpdated, authorId)
    }

    @CacheEvict(value = ["trending", "suggest"], allEntries = true)
    fun reindexAll(): String {
        log.info("Reindex documents triggered — recreating index")
        val indexOps = elasticsearchOperations.indexOps(DocumentIndex::class.java)
        if (indexOps.exists()) {
            indexOps.delete()
        }
        indexOps.createWithMapping()
        log.info("Documents index recreated successfully")
        return "Document index recreated. Data will be repopulated via Kafka events."
    }
}
