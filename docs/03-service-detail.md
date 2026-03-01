# 📖 Search Service — Tài liệu chi tiết (Service Documentation)

> **Service:** Search Service (SS_DEV1)  
> **Version:** 1.0.0  
> **Stack:** Kotlin, Spring Boot 3.x, Java 21, Elasticsearch 8.x, Kafka, Redis  
> **Cập nhật:** 28/02/2026

---

## 📑 Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Cấu trúc project](#2-cấu-trúc-project)
3. [Elasticsearch Index Design](#3-elasticsearch-index-design)
4. [Kafka Consumer Design](#4-kafka-consumer-design)
5. [Search Implementation](#5-search-implementation)
6. [Caching Strategy](#6-caching-strategy)
7. [Error Handling & Resilience](#7-error-handling--resilience)
8. [Configuration Reference](#8-configuration-reference)
9. [Dependencies](#9-dependencies)
10. [Deployment](#10-deployment)

---

## 1. Tổng quan kiến trúc

### Clean Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Controller Layer                     │
│  SearchController, AdminReindexController, TestController│
├─────────────────────────────────────────────────────────┤
│                      Service Layer                       │
│  DocumentSearchService, AuthorSearchService              │
│  IndexInitService                                        │
├─────────────────────────────────────────────────────────┤
│                    Repository Layer                       │
│  DocumentSearchRepository, AuthorSearchRepository        │
│  (+ ElasticsearchOperations for NativeQuery)             │
├─────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                    │
│  Kafka Consumers, Redis Cache, ES Client                 │
│  ElasticsearchConfig, KafkaConfig, RedisConfig           │
└─────────────────────────────────────────────────────────┘
```

### Data Flow

```
                     ┌─────────────┐
                     │   Kafka     │
                     │  Consumer   │
                     └──────┬──────┘
                            │ Event
                     ┌──────▼──────┐
                     │   Mapper    │
                     └──────┬──────┘
                            │ Entity
                     ┌──────▼──────┐
                     │   Service   │──── Cache (Redis)
                     └──────┬──────┘
                            │ Save/Query
                     ┌──────▼──────┐
                     │ Elastic-    │
                     │  search     │
                     └─────────────┘

    Client ──▶ Controller ──▶ Service ──▶ ElasticsearchOperations ──▶ ES
                                │
                                └──▶ Redis Cache (suggest, trending, popular)
```

---

## 2. Cấu trúc project

```
src/main/kotlin/com/horob1/docube_search_search/
├── DocubeSearchSearchApplication.kt    # @SpringBootApplication + @EnableDiscoveryClient
│
├── config/
│   ├── ElasticsearchConfig.kt          # @EnableElasticsearchRepositories
│   ├── ElasticsearchHealthIndicator.kt # Custom health check cho ES cluster
│   ├── KafkaConfig.kt                  # @EnableKafka + DLT + Retry + ContainerFactory
│   ├── RedisConfig.kt                  # @EnableCaching + ConnectionFactory + CacheManager
│   └── WebConfig.kt                    # CORS filter
│
├── controller/
│   ├── SearchController.kt             # Search + Suggest + Trending + Popular APIs
│   ├── AdminReindexController.kt       # POST /admin/reindex/{type}
│   └── TestController.kt               # @Profile("dev") — test endpoints
│
├── consumer/
│   ├── DocumentEventConsumer.kt        # @KafkaListener(topic=document-events)
│   └── AuthorEventConsumer.kt          # @KafkaListener(topic=author-events)
│
├── dto/
│   ├── PageResponse.kt                 # Generic pagination wrapper
│   ├── event/
│   │   ├── DocumentEvent.kt            # Kafka document event + AuthorPayload
│   │   └── AuthorEvent.kt              # Kafka author event
│   ├── request/
│   │   ├── DocumentSearchRequest.kt    # Query params + validation
│   │   └── AuthorSearchRequest.kt      # Query params + validation
│   └── response/
│       ├── DocumentSearchResponse.kt   # + AuthorEmbeddedResponse
│       ├── AuthorSearchResponse.kt
│       ├── SuggestResponse.kt
│       └── ErrorResponse.kt
│
├── entity/
│   ├── DocumentIndex.kt                # @Document(indexName="documents") + EmbeddedAuthor
│   └── AuthorIndex.kt                  # @Document(indexName="authors")
│
├── exception/
│   ├── GlobalExceptionHandler.kt       # @RestControllerAdvice
│   ├── ResourceNotFoundException.kt
│   └── SearchException.kt
│
├── mapper/
│   ├── DocumentMapper.kt               # Event → Entity, Entity → Response
│   └── AuthorMapper.kt
│
├── repository/
│   ├── DocumentSearchRepository.kt     # ElasticsearchRepository<DocumentIndex, String>
│   └── AuthorSearchRepository.kt       # ElasticsearchRepository<AuthorIndex, String>
│
└── service/
    ├── DocumentSearchService.kt        # Search, Suggest, Trending, Index, Delete, UpdateAuthor
    ├── AuthorSearchService.kt          # Search, Popular, Index, Delete, Reindex
    └── IndexInitService.kt             # ApplicationRunner — auto-create indices on startup

src/main/resources/
├── application.properties              # Main config (port, eureka, redis, es, kafka, actuator)
├── elasticsearch/
│   ├── documents-settings.json         # Custom analyzer (edge-ngram autocomplete)
│   └── authors-settings.json           # Custom analyzer
└── static/
    └── index.html                      # Test UI Dashboard (served by Spring Boot)
```

---

## 3. Elasticsearch Index Design

### 3.1. Index: `documents`

| Field | ES Type | Analyzer | Mô tả |
|-------|---------|----------|--------|
| `id` | keyword | — | Primary key |
| `title` | text + keyword | autocomplete_analyzer | Tiêu đề (boost ×3) |
| `description` | text | standard | Mô tả |
| `content` | text | standard | Nội dung |
| `tags` | keyword | — | Tags (array) |
| `categories` | keyword | — | Danh mục (array) |
| `language` | keyword | — | vi, en, ... |
| `status` | keyword | — | PUBLISHED, DRAFT, ARCHIVED |
| `visibility` | keyword | — | PUBLIC, PRIVATE, UNLISTED |
| `createdAt` | date | — | ISO 8601 |
| `updatedAt` | date | — | ISO 8601 |
| `publishedAt` | date | — | ISO 8601 |
| `viewCount` | long | — | Lượt xem |
| `likeCount` | long | — | Lượt thích |
| `commentCount` | long | — | Số comment |
| `score` | float | — | Điểm đánh giá |
| `author` | object | — | Embedded author |
| `author.id` | keyword | — | Author ID |
| `author.username` | text | standard | Username |
| `author.displayName` | text | autocomplete_analyzer | Tên hiển thị (boost ×2) |
| `author.email` | keyword | — | Email |
| `author.avatarUrl` | keyword | — | URL avatar |
| `author.role` | keyword | — | Role |

### 3.2. Index: `authors`

| Field | ES Type | Analyzer | Mô tả |
|-------|---------|----------|--------|
| `id` | keyword | — | Primary key |
| `username` | text + keyword | autocomplete_analyzer | Username (boost ×2) |
| `displayName` | text + keyword | autocomplete_analyzer | Tên hiển thị (boost ×2) |
| `email` | keyword | — | Email |
| `phoneNumber` | keyword | — | SĐT |
| `avatarUrl` | keyword | — | URL avatar |
| `bio` | text | standard | Giới thiệu |
| `role` | keyword | — | AUTHOR, ADMIN, MODERATOR |
| `status` | keyword | — | ACTIVE, INACTIVE, BANNED |
| `createdAt` | date | — | ISO 8601 |
| `documentCount` | long | — | Số bài viết |
| `followerCount` | long | — | Số follower |

### 3.3. Custom Analyzer

```json
{
  "analysis": {
    "filter": {
      "autocomplete_filter": {
        "type": "edge_ngram",
        "min_gram": 2,
        "max_gram": 20
      }
    },
    "analyzer": {
      "autocomplete_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase", "autocomplete_filter"]
      }
    }
  }
}
```

- `autocomplete_analyzer`: tokenize + lowercase + edge_ngram (2-20 chars)
- Dùng cho `title`, `author.displayName`, `username`, `displayName`
- Search sử dụng `standard` analyzer (không edge_ngram khi search)

### 3.4. Design Decisions

| Decision | Lý do |
|----------|-------|
| **Denormalized author** trong document | Tránh parent-child join → performance tốt hơn |
| **Edge-ngram autocomplete** | Hỗ trợ tìm kiếm ngay khi gõ, không cần completion suggester |
| **Keyword cho tags/categories** | Exact match filter, không cần full-text search |
| **Shards: 1, Replicas: 0** (dev) | Đủ cho development. Production nên tăng replicas lên 1-2 |

---

## 4. Kafka Consumer Design

### 4.1. Consumer Architecture

```
Kafka Topic ──▶ @KafkaListener ──▶ switch(eventType) ──▶ Handler ──▶ Service ──▶ ES
                     │
                     ├── Error → Retry (3 times, 1s interval)
                     │
                     └── Still fail → Dead Letter Topic (.DLT)
```

### 4.2. Document Consumer

**Topic:** `document-events`  
**Group ID:** `search-service-group`

| Event | Handler | Service Method |
|-------|---------|----------------|
| `DOCUMENT_CREATED` | `handleCreate()` | `documentSearchService.index(doc)` |
| `DOCUMENT_UPDATED` | `handleUpdate()` | `documentSearchService.index(doc)` (upsert) |
| `DOCUMENT_DELETED` | `handleDelete()` | `documentSearchService.delete(id)` |

### 4.3. Author Consumer

**Topic:** `author-events`  
**Group ID:** `search-service-group`

| Event | Handler | Service Method |
|-------|---------|----------------|
| `AUTHOR_CREATED` | `handleCreate()` | `authorSearchService.index(author)` |
| `AUTHOR_UPDATED` | `handleUpdate()` | `authorSearchService.index(author)` **+** `documentSearchService.updateAuthorInDocuments(id, embedded)` |
| `AUTHOR_DELETED` | `handleDelete()` | `authorSearchService.delete(id)` |

### 4.4. Author Propagation

Khi `AUTHOR_UPDATED`:

```
1. Update authors index (ES save)
2. Query all documents WHERE author.id = {authorId}
   └── Pagination: 500 docs/batch
3. Update embedded author object in each document
4. Bulk save updated documents
```

### 4.5. Error Handling

```
Message ──▶ Consumer
             │ fail
             ├──▶ Retry 1 (after 1s)
             │ fail
             ├──▶ Retry 2 (after 1s)
             │ fail
             ├──▶ Retry 3 (after 1s)
             │ fail
             └──▶ Dead Letter Topic: {topic-name}.DLT
```

- `DefaultErrorHandler` + `FixedBackOff(1000ms, 3 retries)`
- `DeadLetterPublishingRecoverer` → publish failed message to `.DLT` topic
- `RetryListener` → log mỗi lần retry

### 4.6. Concurrency

- `spring.kafka.listener.concurrency=3` → 3 consumer threads
- `ConcurrentKafkaListenerContainerFactory` với concurrency = 3
- `ack-mode=record` → commit offset per record

---

## 5. Search Implementation

### 5.1. Document Search Query

```
BoolQuery:
├── must:
│   └── MultiMatch:
│       ├── fields: title^3, description^2, content, author.displayName^2, author.username
│       ├── type: BEST_FIELDS
│       └── fuzziness: AUTO
├── filter:
│   ├── term(status) — if provided
│   └── term(author.id) — if provided
│
├── highlight:
│   ├── title (pre: <em>, post: </em>)
│   ├── description
│   └── content
│   ├── fragmentSize: 150
│   └── numberOfFragments: 3
│
├── sort:
│   ├── relevance → _score DESC
│   ├── createdAt → createdAt ASC/DESC
│   └── viewCount → viewCount ASC/DESC
│
└── pagination: PageRequest(page, size)
```

**Nếu keyword = null → `match_all` + filters**

### 5.2. Author Search Query

```
BoolQuery:
├── should (minimumShouldMatch = 1):
│   ├── MultiMatch:
│   │   ├── fields: displayName^2, username^2, email
│   │   ├── type: BEST_FIELDS
│   │   └── fuzziness: AUTO
│   ├── MatchPhrasePrefix(displayName)
│   └── MatchPhrasePrefix(username)
├── filter:
│   ├── term(role) — if provided
│   └── term(status) — if provided
│
├── highlight: displayName, username, email
└── pagination
```

### 5.3. Suggest (Autocomplete)

```
MatchPhrasePrefix:
├── field: title
├── query: {prefix}
└── PageRequest(0, size)

→ Trả về distinct titles
→ Cached 10 phút (key: prefix_size)
```

### 5.4. Trending

```
BoolQuery:
├── must: match_all
├── filter: term(status=PUBLISHED)
├── sort: viewCount DESC
└── PageRequest(0, size)

→ Cached 5 phút
```

### 5.5. Popular Authors

```
match_all
├── sort: followerCount DESC
└── PageRequest(0, size)

→ Cached 10 phút
```

---

## 6. Caching Strategy

### Redis Cache Configuration

| Cache Name | TTL | Key Pattern | Mô tả |
|------------|-----|-------------|--------|
| `suggest` | 10 phút | `search:suggest::{prefix}_{size}` | Autocomplete |
| `trending` | 5 phút | `search:trending::{size}` | Trending documents |
| `popular` | 10 phút | `search:popular::{size}` | Popular authors |
| `documents` | 15 phút | `search:documents::*` | (reserved) |
| `authors` | 30 phút | `search:authors::*` | (reserved) |

### Cache Eviction

| Sự kiện | Cache bị xóa |
|---------|--------------|
| Document indexed | `suggest`, `trending` |
| Document deleted | `suggest`, `trending` |
| Author indexed | `popular` |
| Author deleted | `popular` |
| Reindex documents | `suggest`, `trending` |
| Reindex authors | `popular` |

### Prefix

Tất cả cache keys có prefix `search:` để tránh conflict với các service khác dùng chung Redis.

---

## 7. Error Handling & Resilience

### Global Exception Handler

| Exception | HTTP Status | Error Type |
|-----------|-------------|------------|
| `ResourceNotFoundException` | 404 | Not Found |
| `SearchException` | 500 | Search Error |
| `UncategorizedElasticsearchException` | 503 | ES Unavailable |
| `KafkaException` | 503 | Kafka Error |
| `MethodArgumentNotValidException` | 400 | Validation Error |
| `MissingServletRequestParameterException` | 400 | Missing Parameter |
| `IllegalArgumentException` | 400 | Bad Request |
| `Exception` (catchall) | 500 | Internal Server Error |

### Graceful Shutdown

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

- Khi nhận SIGTERM, service sẽ:
  1. Ngừng nhận request mới
  2. Hoàn thành request đang xử lý (tối đa 30s)
  3. Commit Kafka offsets
  4. Đóng kết nối ES, Redis
  5. Shutdown

### Health Check

- `GET /actuator/health` — tổng hợp tất cả
- Custom `ElasticsearchHealthIndicator`: kiểm tra cluster name, status, nodes, shards
- Auto-configured: Redis, Kafka, Eureka health indicators

---

## 8. Configuration Reference

### application.properties

```properties
# ==================== Core ====================
spring.application.name=SS_DEV1
spring.profiles.active=dev
server.port=9111

# ==================== Eureka ====================
eureka.instance.appname=SS_DEV1
eureka.instance.prefer-ip-address=true
eureka.client.service-url.defaultZone=http://localhost:9000/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# ==================== Redis ====================
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=2410
spring.data.redis.timeout=5000ms
spring.data.redis.database=3
spring.cache.type=redis

# ==================== Elasticsearch ====================
spring.elasticsearch.uris=http://localhost:9112
spring.elasticsearch.username=elastic
spring.elasticsearch.password=horob1@2410
spring.elasticsearch.connection-timeout=1000ms
spring.elasticsearch.socket-timeout=30000ms

# ==================== Kafka ====================
spring.kafka.bootstrap-servers=localhost:7092
spring.kafka.consumer.group-id=search-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=...StringDeserializer
spring.kafka.consumer.value-deserializer=...JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.listener.concurrency=3
spring.kafka.listener.ack-mode=record
spring.kafka.producer.key-serializer=...StringSerializer
spring.kafka.producer.value-serializer=...JsonSerializer
spring.kafka.properties.security.protocol=SASL_PLAINTEXT
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=...PlainLoginModule required username="horob1" password="2410";

# ==================== Actuator ====================
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.health.elasticsearch.enabled=true
management.health.redis.enabled=true

# ==================== Server ====================
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Environment Variables (production override)

| Variable | Property | Mô tả |
|----------|----------|--------|
| `SPRING_PROFILES_ACTIVE` | spring.profiles.active | `prod` để disable test endpoints |
| `ES_URI` | spring.elasticsearch.uris | ES cluster URL |
| `ES_USERNAME` | spring.elasticsearch.username | ES username |
| `ES_PASSWORD` | spring.elasticsearch.password | ES password |
| `KAFKA_BOOTSTRAP` | spring.kafka.bootstrap-servers | Kafka broker |
| `REDIS_HOST` | spring.data.redis.host | Redis host |
| `REDIS_PASSWORD` | spring.data.redis.password | Redis password |
| `EUREKA_URL` | eureka.client.service-url.defaultZone | Eureka URL |

---

## 9. Dependencies

### Production Dependencies

| Dependency | Version (BOM) | Mục đích |
|------------|---------------|----------|
| spring-boot-starter-web | 3.5.x | REST API |
| spring-boot-starter-data-elasticsearch | 3.5.x | ES client + repositories |
| spring-boot-starter-data-redis | 3.5.x | Redis client + cache |
| spring-boot-starter-cache | 3.5.x | Spring Cache abstraction |
| spring-boot-starter-validation | 3.5.x | Jakarta Bean Validation |
| spring-boot-starter-actuator | 3.5.x | Health + Metrics |
| spring-kafka | 3.x | Kafka consumer/producer |
| spring-cloud-starter-netflix-eureka-client | 2025.x | Service discovery |
| jackson-module-kotlin | — | JSON serialization |
| kotlin-reflect | — | Kotlin reflection |
| micrometer-registry-prometheus | — | Prometheus metrics |
| spring-retry + spring-aspects | — | Retry mechanism |

### Infrastructure

| Component | Version | Port |
|-----------|---------|------|
| Elasticsearch | 8.13.4 | 9112 (host) → 9200 (container) |
| Kafka | — | 7092 |
| Redis | — | 6379 |
| Eureka | — | 9000 |

---

## 10. Deployment

### Docker network

```bash
docker network create horob1_docub
```

### Elasticsearch cluster

```bash
docker compose up -d
```

3 nodes: es01 (exposed 9112), es02, es03 + Kibana (5601)

### Build & Run

```powershell
# Build
.\gradlew.bat clean build -x test

# Run (dev)
.\gradlew.bat bootRun

# Run (prod — no test endpoints)
.\gradlew.bat bootRun --args='--spring.profiles.active=prod'
```

### Production Checklist

- [ ] Đổi `spring.profiles.active=prod`
- [ ] Đổi ES password mạnh hơn
- [ ] Đổi Redis password
- [ ] Đổi Kafka SASL credentials
- [ ] Bật ES transport SSL
- [ ] Tăng `number_of_replicas` lên 1-2 trong settings JSON
- [ ] Cấu hình Kubernetes liveness/readiness probe tới `/actuator/health`
- [ ] Monitor via Prometheus (`/actuator/prometheus`)
- [ ] Giám sát Dead Letter Topics (`.DLT`)

