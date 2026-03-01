package com.horob1.docube_search_search.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
class KafkaConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val DOCUMENT_TOPIC = "document-events"
        const val AUTHOR_TOPIC = "author-events"
    }

    /**
     * Custom error handler:
     * - Retry 3 times with 1s interval
     * - After exhaustion, send to Dead Letter Topic (original-topic.DLT)
     */
    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, Any>): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3L))
        val listener = RetryListener { record, ex, attempt ->
            log.warn("⚠️ Kafka retry attempt {} for topic={}, key={}, error={}",
                attempt, record.topic(), record.key(), ex.message)
        }
        errorHandler.setRetryListeners(listener)
        return errorHandler
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        kafkaErrorHandler: CommonErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(kafkaErrorHandler)
        factory.setConcurrency(3)
        return factory
    }
}
