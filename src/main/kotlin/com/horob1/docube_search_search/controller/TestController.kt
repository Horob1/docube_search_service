package com.horob1.docube_search_search.controller

import com.horob1.docube_search_search.config.KafkaConfig
import com.horob1.docube_search_search.dto.event.AuthorEvent
import com.horob1.docube_search_search.dto.event.AuthorPayload
import com.horob1.docube_search_search.dto.event.DocumentEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

/**
 * DEV-only controller for testing Kafka events and search functionality.
 * Provides endpoints to push fake events into Kafka topics.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/test")
class TestController(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ==================== Push Document Events ====================

    @PostMapping("/kafka/document")
    fun pushDocumentEvent(@RequestBody event: DocumentEvent): ResponseEntity<Map<String, Any>> {
        log.info("🧪 TEST: Pushing document event: type={}, id={}", event.eventType, event.id)
        kafkaTemplate.send(KafkaConfig.DOCUMENT_TOPIC, event.id, event)
        return ResponseEntity.ok(mapOf(
            "status" to "sent",
            "topic" to KafkaConfig.DOCUMENT_TOPIC,
            "eventType" to event.eventType,
            "id" to event.id
        ))
    }

    @PostMapping("/kafka/author")
    fun pushAuthorEvent(@RequestBody event: AuthorEvent): ResponseEntity<Map<String, Any>> {
        log.info("🧪 TEST: Pushing author event: type={}, id={}", event.eventType, event.id)
        kafkaTemplate.send(KafkaConfig.AUTHOR_TOPIC, event.id, event)
        return ResponseEntity.ok(mapOf(
            "status" to "sent",
            "topic" to KafkaConfig.AUTHOR_TOPIC,
            "eventType" to event.eventType,
            "id" to event.id
        ))
    }

    // ==================== Generate Sample Data ====================

    @PostMapping("/generate/documents")
    fun generateSampleDocuments(@RequestParam(defaultValue = "5") count: Int): ResponseEntity<Map<String, Any>> {
        val authorId = "author-${UUID.randomUUID().toString().take(8)}"
        val authorPayload = AuthorPayload(
            id = authorId,
            username = "john_doe",
            displayName = "John Doe",
            email = "john@example.com",
            avatarUrl = "https://i.pravatar.cc/150?u=$authorId",
            role = "AUTHOR"
        )

        // Push the author first
        val authorEvent = AuthorEvent(
            eventType = "AUTHOR_CREATED",
            id = authorId,
            username = "john_doe",
            displayName = "John Doe",
            email = "john@example.com",
            phoneNumber = "0901234567",
            avatarUrl = "https://i.pravatar.cc/150?u=$authorId",
            bio = "A passionate writer and developer",
            role = "AUTHOR",
            status = "ACTIVE",
            createdAt = Instant.now().toString(),
            documentCount = count.toLong(),
            followerCount = 42
        )
        kafkaTemplate.send(KafkaConfig.AUTHOR_TOPIC, authorId, authorEvent)

        val sampleDocs = listOf(
            Triple("Hướng dẫn Spring Boot từ A đến Z", "Tổng hợp kiến thức Spring Boot cho người mới bắt đầu", "Spring Boot là framework phổ biến nhất trong hệ sinh thái Java..."),
            Triple("Elasticsearch Deep Dive", "Tìm hiểu sâu về Elasticsearch và cách tối ưu search", "Elasticsearch là một search engine phân tán, xây dựng trên Apache Lucene..."),
            Triple("Kafka Event-Driven Architecture", "Xây dựng hệ thống microservice với Kafka", "Apache Kafka là nền tảng streaming phân tán, được thiết kế cho throughput cao..."),
            Triple("Docker và Kubernetes cho Developer", "Containerization và orchestration trong thực tế", "Docker giúp đóng gói ứng dụng vào container, đảm bảo môi trường đồng nhất..."),
            Triple("Clean Architecture với Kotlin", "Áp dụng Clean Architecture trong dự án thực tế", "Clean Architecture tách biệt business logic khỏi framework và infrastructure..."),
            Triple("Redis Cache Strategy", "Các chiến lược cache hiệu quả với Redis", "Redis là in-memory data store, hỗ trợ nhiều cấu trúc dữ liệu phong phú..."),
            Triple("GraphQL vs REST API", "So sánh hai phong cách API phổ biến", "GraphQL cho phép client yêu cầu chính xác dữ liệu cần thiết..."),
            Triple("Microservices Design Patterns", "Các pattern quan trọng trong thiết kế microservices", "Saga pattern giúp quản lý distributed transaction giữa các service..."),
            Triple("CI/CD Pipeline với GitHub Actions", "Tự động hoá quy trình build và deploy", "Continuous Integration và Continuous Deployment giúp team ship code nhanh hơn..."),
            Triple("Monitoring với Prometheus và Grafana", "Giám sát hệ thống microservice hiệu quả", "Prometheus thu thập metrics, Grafana hiển thị dashboard trực quan...")
        )

        val tags = listOf(
            listOf("spring", "java", "backend"),
            listOf("elasticsearch", "search", "lucene"),
            listOf("kafka", "event-driven", "microservice"),
            listOf("docker", "kubernetes", "devops"),
            listOf("kotlin", "architecture", "clean-code"),
            listOf("redis", "cache", "performance"),
            listOf("graphql", "rest", "api"),
            listOf("microservices", "patterns", "design"),
            listOf("cicd", "github", "automation"),
            listOf("monitoring", "prometheus", "grafana")
        )

        val categories = listOf("Backend", "Search", "Messaging", "DevOps", "Architecture",
            "Database", "API", "Architecture", "DevOps", "Monitoring")
        val schoolIds = listOf("HCMUS", "HCMUT", "UEH", "HCMUS", "HCMUS", "HCMUT", "UEH", "HCMUS", "HCMUT", "UEH")
        val schoolNames = listOf(
            "Truong Dai hoc Khoa hoc Tu nhien",
            "Truong Dai hoc Bach Khoa",
            "Truong Dai hoc Kinh te TP.HCM",
            "Truong Dai hoc Khoa hoc Tu nhien",
            "Truong Dai hoc Khoa hoc Tu nhien",
            "Truong Dai hoc Bach Khoa",
            "Truong Dai hoc Kinh te TP.HCM",
            "Truong Dai hoc Khoa hoc Tu nhien",
            "Truong Dai hoc Bach Khoa",
            "Truong Dai hoc Kinh te TP.HCM"
        )
        val facultyIds = listOf("CNTT", "DTVT", "QTKD", "CNTT", "KHTN", "CKM", "TCNH", "CNTT", "CKM", "QTKD")
        val facultyNames = listOf(
            "Cong nghe thong tin",
            "Dien tu vien thong",
            "Quan tri kinh doanh",
            "Cong nghe thong tin",
            "Khoa hoc tu nhien",
            "Co khi may",
            "Tai chinh ngan hang",
            "Cong nghe thong tin",
            "Co khi may",
            "Quan tri kinh doanh"
        )

        val ids = mutableListOf<String>()

        for (i in 0 until minOf(count, sampleDocs.size)) {
            val docId = "doc-${UUID.randomUUID().toString().take(8)}"
            ids.add(docId)
            val (title, desc, content) = sampleDocs[i]
            val event = DocumentEvent(
                eventType = "DOCUMENT_CREATED",
                id = docId,
                title = title,
                description = desc,
                content = content,
                schoolId = schoolIds[i],
                schoolName = schoolNames[i],
                facultyId = facultyIds[i],
                facultyName = facultyNames[i],
                tags = tags[i],
                categories = listOf(categories[i]),
                language = "vi",
                status = "PUBLISHED",
                visibility = "PUBLIC",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString(),
                publishedAt = Instant.now().toString(),
                viewCount = (100..5000).random().toLong(),
                likeCount = (10..500).random().toLong(),
                commentCount = (0..100).random().toLong(),
                score = (3.0f..5.0f).random(),
                author = authorPayload
            )
            kafkaTemplate.send(KafkaConfig.DOCUMENT_TOPIC, docId, event)
        }

        log.info("🧪 TEST: Generated {} sample documents + 1 author", minOf(count, sampleDocs.size))

        return ResponseEntity.ok(mapOf(
            "status" to "generated",
            "documentsCount" to minOf(count, sampleDocs.size),
            "authorId" to authorId,
            "documentIds" to ids
        ))
    }

    @PostMapping("/generate/authors")
    fun generateSampleAuthors(@RequestParam(defaultValue = "5") count: Int): ResponseEntity<Map<String, Any>> {
        val sampleAuthors = listOf(
            Triple("alice_wonder", "Alice Wonder", "alice@example.com"),
            Triple("bob_builder", "Bob Builder", "bob@example.com"),
            Triple("charlie_dev", "Charlie Developer", "charlie@example.com"),
            Triple("diana_code", "Diana Coder", "diana@example.com"),
            Triple("eve_hacker", "Eve Hacker", "eve@example.com"),
            Triple("frank_ops", "Frank DevOps", "frank@example.com"),
            Triple("grace_ml", "Grace ML Engineer", "grace@example.com"),
            Triple("henry_data", "Henry Data", "henry@example.com"),
            Triple("ivy_design", "Ivy Designer", "ivy@example.com"),
            Triple("jack_full", "Jack Fullstack", "jack@example.com")
        )

        val bios = listOf(
            "Passionate about clean code and TDD",
            "Building scalable systems since 2015",
            "Open source contributor and tech blogger",
            "Backend specialist with 10 years experience",
            "Security researcher and ethical hacker",
            "DevOps engineer, loves automation",
            "Machine learning enthusiast",
            "Data engineer working with big data",
            "UI/UX designer turned developer",
            "Fullstack developer, JavaScript & Kotlin"
        )

        val roles = listOf("AUTHOR", "AUTHOR", "ADMIN", "AUTHOR", "MODERATOR",
            "AUTHOR", "AUTHOR", "AUTHOR", "AUTHOR", "ADMIN")

        val ids = mutableListOf<String>()

        for (i in 0 until minOf(count, sampleAuthors.size)) {
            val authorId = "author-${UUID.randomUUID().toString().take(8)}"
            ids.add(authorId)
            val (username, displayName, email) = sampleAuthors[i]
            val event = AuthorEvent(
                eventType = "AUTHOR_CREATED",
                id = authorId,
                username = username,
                displayName = displayName,
                email = email,
                phoneNumber = "090${(1000000..9999999).random()}",
                avatarUrl = "https://i.pravatar.cc/150?u=$authorId",
                bio = bios[i],
                role = roles[i],
                status = "ACTIVE",
                createdAt = Instant.now().toString(),
                documentCount = (0..50).random().toLong(),
                followerCount = (10..1000).random().toLong()
            )
            kafkaTemplate.send(KafkaConfig.AUTHOR_TOPIC, authorId, event)
        }

        log.info("🧪 TEST: Generated {} sample authors", minOf(count, sampleAuthors.size))

        return ResponseEntity.ok(mapOf(
            "status" to "generated",
            "count" to minOf(count, sampleAuthors.size),
            "authorIds" to ids
        ))
    }

    private fun ClosedFloatingPointRange<Float>.random(): Float {
        return start + Math.random().toFloat() * (endInclusive - start)
    }
}
