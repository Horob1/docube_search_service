package com.horob1.docube_search_search.consumer

import com.horob1.docube_search_search.config.KafkaConfig
import com.horob1.docube_search_search.dto.event.DocumentEvent
import com.horob1.docube_search_search.mapper.DocumentMapper
import com.horob1.docube_search_search.service.DocumentSearchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class DocumentEventConsumer(
    private val documentSearchService: DocumentSearchService,
    private val documentMapper: DocumentMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaConfig.DOCUMENT_TOPIC],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        @Payload event: DocumentEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        log.info("📩 Received document event: type={}, id={}, topic={}, partition={}, offset={}",
            event.eventType, event.id, topic, partition, offset)

        try {
            when (event.eventType.uppercase()) {
                "DOCUMENT_CREATED" -> handleCreate(event)
                "DOCUMENT_UPDATED" -> handleUpdate(event)
                "DOCUMENT_DELETED" -> handleDelete(event)
                else -> log.warn("Unknown document event type: {}", event.eventType)
            }
        } catch (ex: Exception) {
            log.error("❌ Failed to process document event: type={}, id={}, error={}",
                event.eventType, event.id, ex.message, ex)
            throw ex // re-throw so Kafka error handler can retry / DLT
        }
    }

    private fun handleCreate(event: DocumentEvent) {
        val doc = documentMapper.fromEvent(event)
        documentSearchService.index(doc)
        log.info("✅ Indexed new document: {}", event.id)
    }

    private fun handleUpdate(event: DocumentEvent) {
        val doc = documentMapper.fromEvent(event)
        documentSearchService.index(doc)
        log.info("✅ Updated document: {}", event.id)
    }

    private fun handleDelete(event: DocumentEvent) {
        documentSearchService.delete(event.id)
        log.info("🗑 Deleted document: {}", event.id)
    }
}
