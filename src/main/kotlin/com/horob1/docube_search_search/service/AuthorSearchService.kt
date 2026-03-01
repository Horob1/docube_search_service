package com.horob1.docube_search_search.service

import com.horob1.docube_search_search.dto.PageResponse
import com.horob1.docube_search_search.dto.request.AuthorSearchRequest
import com.horob1.docube_search_search.dto.response.AuthorSearchResponse
import com.horob1.docube_search_search.entity.AuthorIndex
import com.horob1.docube_search_search.exception.SearchException
import com.horob1.docube_search_search.mapper.AuthorMapper
import com.horob1.docube_search_search.repository.AuthorSearchRepository
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters
import org.springframework.stereotype.Service

@Service
class AuthorSearchService(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val authorSearchRepository: AuthorSearchRepository,
    private val authorMapper: AuthorMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(request: AuthorSearchRequest): PageResponse<AuthorSearchResponse> {
        try {
            val pageable = PageRequest.of(request.page, request.size)
            val queryBuilder = NativeQuery.builder()

            if (!request.keyword.isNullOrBlank()) {
                queryBuilder.withQuery { q ->
                    q.bool { b ->
                        b.should { s ->
                            s.multiMatch { mm ->
                                mm.query(request.keyword)
                                    .fields("displayName^2", "username^2", "email")
                                    .type(TextQueryType.BestFields)
                                    .fuzziness("AUTO")
                            }
                        }
                        b.should { s ->
                            s.matchPhrasePrefix { mpp ->
                                mpp.field("displayName").query(request.keyword)
                            }
                        }
                        b.should { s ->
                            s.matchPhrasePrefix { mpp ->
                                mpp.field("username").query(request.keyword)
                            }
                        }
                        b.minimumShouldMatch("1")

                        if (!request.role.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("role").value(request.role) } }
                        }
                        if (!request.status.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("status").value(request.status) } }
                        }
                        b
                    }
                }
            } else {
                queryBuilder.withQuery { q ->
                    q.bool { b ->
                        b.must { m -> m.matchAll { it } }
                        if (!request.role.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("role").value(request.role) } }
                        }
                        if (!request.status.isNullOrBlank()) {
                            b.filter { f -> f.term { t -> t.field("status").value(request.status) } }
                        }
                        b
                    }
                }
            }

            // Highlight
            val highlight = Highlight(
                HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build(),
                listOf(
                    HighlightField("displayName"),
                    HighlightField("username"),
                    HighlightField("email")
                )
            )

            queryBuilder
                .withPageable(pageable)
                .withHighlightQuery(HighlightQuery(highlight, AuthorIndex::class.java))

            val query = queryBuilder.build()
            val searchHits = elasticsearchOperations.search(query, AuthorIndex::class.java)

            val totalHits = searchHits.totalHits
            val totalPages = if (totalHits == 0L) 0 else ((totalHits + request.size - 1) / request.size).toInt()

            val content = searchHits.searchHits.map { hit ->
                val highlightMap = hit.highlightFields.filter { it.value.isNotEmpty() }
                authorMapper.toResponse(hit.content, highlightMap)
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
            throw SearchException("Error searching authors: ${ex.message}", ex)
        }
    }

    @Cacheable(value = ["popular"], key = "#size")
    fun popular(size: Int = 10): List<AuthorSearchResponse> {
        val query = NativeQuery.builder()
            .withQuery { q -> q.matchAll { it } }
            .withSort(Sort.by(Sort.Direction.DESC, "followerCount"))
            .withPageable(PageRequest.of(0, size))
            .build()

        val hits = elasticsearchOperations.search(query, AuthorIndex::class.java)
        return hits.searchHits.map { authorMapper.toResponse(it.content) }
    }

    @CacheEvict(value = ["popular"], allEntries = true)
    fun index(author: AuthorIndex) {
        elasticsearchOperations.save(author)
        log.info("Indexed author: {}", author.id)
    }

    @CacheEvict(value = ["popular"], allEntries = true)
    fun delete(id: String) {
        authorSearchRepository.deleteById(id)
        log.info("Deleted author: {}", id)
    }

    @CacheEvict(value = ["popular"], allEntries = true)
    fun reindexAll(): String {
        log.info("Reindex authors triggered — recreating index")
        val indexOps = elasticsearchOperations.indexOps(AuthorIndex::class.java)
        if (indexOps.exists()) {
            indexOps.delete()
        }
        indexOps.createWithMapping()
        log.info("Authors index recreated successfully")
        return "Author index recreated. Data will be repopulated via Kafka events."
    }
}
