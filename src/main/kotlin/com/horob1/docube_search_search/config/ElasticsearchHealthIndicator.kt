package com.horob1.docube_search_search.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component("elasticsearchCluster")
class ElasticsearchHealthIndicator(
    private val elasticsearchClient: ElasticsearchClient
) : HealthIndicator {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun health(): Health {
        return try {
            val clusterHealth = elasticsearchClient.cluster().health()
            val status = clusterHealth.status().jsonValue()
            val details = mapOf(
                "clusterName" to clusterHealth.clusterName(),
                "status" to status,
                "numberOfNodes" to clusterHealth.numberOfNodes(),
                "activeShards" to clusterHealth.activeShards(),
                "activePrimaryShards" to clusterHealth.activePrimaryShards()
            )

            when (status.lowercase()) {
                "green", "yellow" -> Health.up().withDetails(details).build()
                else -> Health.down().withDetails(details).build()
            }
        } catch (ex: Exception) {
            log.error("Elasticsearch health check failed: {}", ex.message)
            Health.down()
                .withDetail("error", ex.message ?: "Unknown error")
                .build()
        }
    }
}
