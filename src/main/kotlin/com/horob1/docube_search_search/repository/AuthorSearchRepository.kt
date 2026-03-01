package com.horob1.docube_search_search.repository

import com.horob1.docube_search_search.entity.AuthorIndex
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthorSearchRepository : ElasticsearchRepository<AuthorIndex, String>

