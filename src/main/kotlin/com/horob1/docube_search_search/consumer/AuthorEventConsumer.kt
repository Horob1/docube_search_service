package com.horob1.docube_search_search.consumer

import com.horob1.docube_search_search.config.KafkaConfig
import com.horob1.docube_search_search.dto.event.AuthorEvent
import com.horob1.docube_search_search.entity.EmbeddedAuthor
import com.horob1.docube_search_search.mapper.AuthorMapper
import com.horob1.docube_search_search.service.AuthorSearchService
import com.horob1.docube_search_search.service.DocumentSearchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class AuthorEventConsumer(
    private val authorSearchService: AuthorSearchService,
    private val documentSearchService: DocumentSearchService,
    private val authorMapper: AuthorMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaConfig.AUTHOR_TOPIC],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        @Payload event: AuthorEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        log.info("📩 Received author event: type={}, id={}, topic={}, partition={}, offset={}",
            event.eventType, event.id, topic, partition, offset)

        try {
            when (event.eventType.uppercase()) {
                "AUTHOR_CREATED" -> handleCreate(event)
                "AUTHOR_UPDATED" -> handleUpdate(event)
                "AUTHOR_DELETED" -> handleDelete(event)
                else -> log.warn("Unknown author event type: {}", event.eventType)
            }
        } catch (ex: Exception) {
            log.error("❌ Failed to process author event: type={}, id={}, error={}",
                event.eventType, event.id, ex.message, ex)
            throw ex
        }
    }

    private fun handleCreate(event: AuthorEvent) {
        val author = authorMapper.fromEvent(event)
        authorSearchService.index(author)
        log.info("✅ Indexed new author: {}", event.id)
    }

    private fun handleUpdate(event: AuthorEvent) {
        // 1. Update authors index
        val author = authorMapper.fromEvent(event)
        authorSearchService.index(author)
        log.info("✅ Updated author: {}", event.id)

        // 2. Update embedded author in ALL documents with this author.id
        val embeddedAuthor = EmbeddedAuthor(
            id = event.id,
            username = event.username,
            displayName = event.displayName,
            email = event.email,
            avatarUrl = event.avatarUrl,
            role = event.role
        )
        documentSearchService.updateAuthorInDocuments(event.id, embeddedAuthor)
        log.info("🔄 Propagated author update to documents for author: {}", event.id)
    }

    private fun handleDelete(event: AuthorEvent) {
        authorSearchService.delete(event.id)
        log.info("🗑 Deleted author: {}", event.id)
    }
}
