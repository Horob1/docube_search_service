package com.horob1.docube_search_search.repository

import com.horob1.docube_search_search.entity.DocumentIndex
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentSearchRepository : ElasticsearchRepository<DocumentIndex, String>

