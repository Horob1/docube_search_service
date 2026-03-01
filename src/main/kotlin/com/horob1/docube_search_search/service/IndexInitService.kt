package com.horob1.docube_search_search.service

import com.horob1.docube_search_search.entity.AuthorIndex
import com.horob1.docube_search_search.entity.DocumentIndex
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.IndexOperations
import org.springframework.stereotype.Component

@Component
class IndexInitService(
    private val elasticsearchOperations: ElasticsearchOperations
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        createIndexIfNotExists(DocumentIndex::class.java, "documents")
        createIndexIfNotExists(AuthorIndex::class.java, "authors")
    }

    private fun <T> createIndexIfNotExists(clazz: Class<T>, indexName: String) {
        val indexOps: IndexOperations = elasticsearchOperations.indexOps(clazz)
        if (!indexOps.exists()) {
            indexOps.createWithMapping()
            log.info("✅ Created index '{}' with mappings and settings", indexName)
        } else {
            log.info("✔ Index '{}' already exists", indexName)
        }
    }
}

