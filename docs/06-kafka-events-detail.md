# 📨 Search Service — Kafka Events & Topic Models chi tiết

> **Cập nhật:** 22/03/2026  
> **Kafka Bootstrap:** `localhost:7092`  
> **Security:** SASL_PLAINTEXT (username: `horob1`, password: `2410`)  
> **Consumer Group:** `search-service-group`

---

## 📑 Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Topic: document-events](#2-topic-document-events)
3. [Topic: author-events](#3-topic-author-events)
4. [Dead Letter Topics](#4-dead-letter-topics)
5. [Producer Configuration](#5-producer-configuration)
6. [Ví dụ code Producer](#6-ví-dụ-code-producer)
7. [Quy tắc & Best Practices](#7-quy-tắc--best-practices)

---

## 1. Tổng quan

### Sơ đồ luồng

```
┌──────────────────┐                      ┌──────────────────────────────┐
│ Document Service │──── Kafka ──────────>│ Search Service               │
│                  │  topic:              │                              │
│  Khi CREATE/     │  document-events     │  DocumentEventConsumer       │
│  UPDATE/DELETE   │                      │  ├── CREATED → index vào ES  │
│  document        │                      │  ├── UPDATED → update ES     │
│                  │                      │  └── DELETED → xóa khỏi ES  │
└──────────────────┘                      └──────────────────────────────┘

┌──────────────────┐                      ┌──────────────────────────────┐
│  User Service    │──── Kafka ──────────>│ Search Service               │
│                  │  topic:              │                              │
│  Khi CREATE/     │  author-events       │  AuthorEventConsumer         │
│  UPDATE/DELETE   │                      │  ├── CREATED → index vào ES  │
│  user/author     │                      │  ├── UPDATED → update ES     │
│                  │                      │  │   + update ALL documents   │
│                  │                      │  │     có author.id này       │
│                  │                      │  └── DELETED → xóa khỏi ES  │
└──────────────────┘                      └──────────────────────────────┘
```

### Bảng tổng hợp Topics

| Topic | Producer | Consumer | Event Types |
|-------|----------|----------|-------------|
| `document-events` | Document Service | Search Service | `DOCUMENT_CREATED`, `DOCUMENT_UPDATED`, `DOCUMENT_DELETED` |
| `author-events` | User Service | Search Service | `AUTHOR_CREATED`, `AUTHOR_UPDATED`, `AUTHOR_DELETED` |
| `document-events.DLT` | Search Service (auto) | — | Messages lỗi sau 3 lần retry |
| `author-events.DLT` | Search Service (auto) | — | Messages lỗi sau 3 lần retry |

---

## 2. Topic: `document-events`

### 2.1. Message Key

- **Type:** String
- **Value:** `id` của document (vd: `"doc-abc12345"`)
- **Mục đích:** Đảm bảo tất cả events của cùng 1 document đi vào cùng partition → xử lý đúng thứ tự

### 2.2. Event Model: `DocumentEvent`

```json
{
  "eventType": "DOCUMENT_CREATED",
  "id": "doc-abc12345",
  "title": "Tiêu đề bài viết",
  "description": "Mô tả ngắn gọn về bài viết",
  "content": "Nội dung đầy đủ của bài viết. Có thể dài nhiều paragraph...",
  "schoolId": "HCMUS",
  "schoolName": "Truong Dai hoc Khoa hoc Tu nhien",
  "facultyId": "CNTT",
  "facultyName": "Cong nghe thong tin",
  "tags": ["spring", "java", "backend"],
  "categories": ["Backend", "Tutorial"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:30:00Z",
  "publishedAt": "2026-02-28T10:00:00Z",
  "viewCount": 1234,
  "likeCount": 56,
  "commentCount": 12,
  "score": 4.5,
  "author": {
    "id": "author-xyz789",
    "username": "john_doe",
    "displayName": "John Doe",
    "email": "john@example.com",
    "avatarUrl": "https://example.com/avatars/john.jpg",
    "role": "AUTHOR"
  }
}
```

### 2.3. Chi tiết từng trường

#### Root fields

| # | Field | Type | Required | Giá trị hợp lệ | Mô tả |
|---|-------|------|----------|-----------------|--------|
| 1 | `eventType` | String | ✅ **BẮT BUỘC** | `DOCUMENT_CREATED` \| `DOCUMENT_UPDATED` \| `DOCUMENT_DELETED` | Loại sự kiện |
| 2 | `id` | String | ✅ **BẮT BUỘC** | Bất kỳ string unique | ID duy nhất của document. Dùng làm `_id` trong Elasticsearch |
| 3 | `title` | String | ⚠️ *Required cho CREATE/UPDATE* | Bất kỳ | Tiêu đề bài viết. Được search với **boost x6**. Được dùng cho suggest |
| 4 | `description` | String | ❌ | Bất kỳ | Mô tả ngắn |
| 5 | `content` | String | ❌ | Bất kỳ | Nội dung đầy đủ. Được search với **boost x1** |
| 6 | `schoolId` | String | ❌ | Bất kỳ | Mã trường. Map vào field `school_id` (keyword filter) |
| 7 | `schoolName` | String | ❌ | Bất kỳ | Tên trường. Map vào field `school_name` (text + keyword, boost x3) |
| 8 | `facultyId` | String | ❌ | Bất kỳ | Mã khoa. Map vào field `faculty_id` (keyword filter) |
| 9 | `facultyName` | String | ❌ | Bất kỳ | Tên khoa. Map vào field `faculty_name` (text + keyword, boost x3) |
| 10 | `tags` | List<String> | ❌ | Array of strings | Danh sách tags. Lưu dạng keyword |
| 11 | `categories` | List<String> | ❌ | Array of strings | Danh sách categories. Lưu dạng keyword |
| 12 | `language` | String | ❌ | `vi`, `en`, `ja`, ... | Mã ngôn ngữ ISO 639-1 |
| 13 | `status` | String | ❌ | `PUBLISHED` \| `DRAFT` \| `ARCHIVED` | Trạng thái bài viết. Dùng filter |
| 14 | `visibility` | String | ❌ | `PUBLIC` \| `PRIVATE` \| `UNLISTED` | Quyền hiển thị |
| 15 | `createdAt` | String | ❌ | ISO 8601 hoặc epoch millis | Ngày tạo. Dùng để sort |
| 16 | `updatedAt` | String | ❌ | ISO 8601 hoặc epoch millis | Ngày cập nhật |
| 17 | `publishedAt` | String | ❌ | ISO 8601 hoặc epoch millis | Ngày xuất bản |
| 18 | `viewCount` | Long | ❌ | `>= 0` | Lượt xem |
| 19 | `likeCount` | Long | ❌ | `>= 0` | Lượt thích |
| 20 | `commentCount` | Long | ❌ | `>= 0` | Số comment |
| 21 | `score` | Float | ❌ | `0.0 - 5.0` | Điểm đánh giá trung bình |
| 22 | `author` | Object | ⚠️ *Required cho CREATE/UPDATE* | Xem bảng bên dưới | Thông tin tác giả (embedded) |

#### Object `author` (embedded trong DocumentEvent)

| # | Field | Type | Required | Giá trị hợp lệ | Mô tả |
|---|-------|------|----------|-----------------|--------|
| 1 | `author.id` | String | ✅ **BẮT BUỘC** | Bất kỳ string unique | ID tác giả. Dùng để link với authors index. Dùng để filter (`authorId=...`) |
| 2 | `author.username` | String | ❌ | Bất kỳ | Username. Được full-text search |
| 3 | `author.displayName` | String | ❌ | Bất kỳ | Tên hiển thị. Được search với **boost ×2** |
| 4 | `author.email` | String | ❌ | Email format | Email. Lưu dạng keyword (exact match) |
| 5 | `author.avatarUrl` | String | ❌ | URL | URL ảnh avatar |
| 6 | `author.role` | String | ❌ | `AUTHOR` \| `ADMIN` \| `MODERATOR` | Role của tác giả |

### 2.4. Yêu cầu theo từng Event Type

#### `DOCUMENT_CREATED`

Bắn khi: **Tạo mới document**

```json
{
  "eventType": "DOCUMENT_CREATED",
  "id": "doc-abc12345",
  "title": "Tiêu đề bài viết",
  "description": "Mô tả ngắn",
  "content": "Nội dung đầy đủ...",
  "schoolId": "HCMUS",
  "schoolName": "Truong Dai hoc Khoa hoc Tu nhien",
  "facultyId": "CNTT",
  "facultyName": "Cong nghe thong tin",
  "tags": ["spring", "java"],
  "categories": ["Backend"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:00:00Z",
  "publishedAt": "2026-02-28T10:00:00Z",
  "viewCount": 0,
  "likeCount": 0,
  "commentCount": 0,
  "score": 0.0,
  "author": {
    "id": "author-xyz789",
    "username": "john_doe",
    "displayName": "John Doe",
    "email": "john@example.com",
    "avatarUrl": "https://example.com/a.jpg",
    "role": "AUTHOR"
  }
}
```

**Search Service xử lý:** `elasticsearchOperations.save(documentIndex)` → index vào ES

---

#### `DOCUMENT_UPDATED`

Bắn khi: **Cập nhật document**

```json
{
  "eventType": "DOCUMENT_UPDATED",
  "id": "doc-abc12345",
  "title": "Tiêu đề đã sửa",
  "description": "Mô tả đã sửa",
  "content": "Nội dung đã sửa...",
  "schoolId": "HCMUS",
  "schoolName": "Truong Dai hoc Khoa hoc Tu nhien",
  "facultyId": "CNTT",
  "facultyName": "Cong nghe thong tin",
  "tags": ["spring", "java", "updated"],
  "categories": ["Backend"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T12:00:00Z",
  "publishedAt": "2026-02-28T10:00:00Z",
  "viewCount": 1500,
  "likeCount": 80,
  "commentCount": 20,
  "score": 4.7,
  "author": {
    "id": "author-xyz789",
    "username": "john_doe",
    "displayName": "John Doe",
    "email": "john@example.com",
    "avatarUrl": "https://example.com/a.jpg",
    "role": "AUTHOR"
  }
}
```

> ⚠️ **QUAN TRỌNG:** Gửi **ĐẦY ĐỦ tất cả fields**, không chỉ fields thay đổi. Search Service sẽ **ghi đè toàn bộ document** trong ES (upsert).

**Search Service xử lý:** `elasticsearchOperations.save(documentIndex)` → upsert trong ES

---

#### `DOCUMENT_DELETED`

Bắn khi: **Xóa document**

```json
{
  "eventType": "DOCUMENT_DELETED",
  "id": "doc-abc12345"
}
```

Chỉ cần 2 trường. Tất cả trường khác bị bỏ qua.

**Search Service xử lý:** `documentSearchRepository.deleteById(id)` → xóa khỏi ES

---

## 3. Topic: `author-events`

### 3.1. Message Key

- **Type:** String
- **Value:** `id` của author/user (vd: `"author-xyz789"`)

### 3.2. Event Model: `AuthorEvent`

```json
{
  "eventType": "AUTHOR_CREATED",
  "id": "author-xyz789",
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatars/john.jpg",
  "bio": "Passionate developer and writer. I love Spring Boot and Kotlin.",
  "role": "AUTHOR",
  "status": "ACTIVE",
  "createdAt": "2026-01-15T08:00:00Z",
  "documentCount": 15,
  "followerCount": 200
}
```

### 3.3. Chi tiết từng trường

| # | Field | Type | Required | Giá trị hợp lệ | Mô tả |
|---|-------|------|----------|-----------------|--------|
| 1 | `eventType` | String | ✅ **BẮT BUỘC** | `AUTHOR_CREATED` \| `AUTHOR_UPDATED` \| `AUTHOR_DELETED` | Loại sự kiện |
| 2 | `id` | String | ✅ **BẮT BUỘC** | Bất kỳ string unique | ID duy nhất. Dùng làm `_id` trong ES. Dùng để match `author.id` trong documents |
| 3 | `username` | String | ⚠️ *Required cho CREATE/UPDATE* | Bất kỳ | Username. Được search với **boost ×2**. Hỗ trợ autocomplete |
| 4 | `displayName` | String | ⚠️ *Required cho CREATE/UPDATE* | Bất kỳ | Tên hiển thị. Được search với **boost ×2**. Hỗ trợ autocomplete + fuzzy |
| 5 | `email` | String | ❌ | Email format | Email. Lưu dạng keyword, searchable nhưng không fuzzy |
| 6 | `phoneNumber` | String | ❌ | Bất kỳ | Số điện thoại. Lưu dạng keyword |
| 7 | `avatarUrl` | String | ❌ | URL | URL ảnh avatar |
| 8 | `bio` | String | ❌ | Bất kỳ | Giới thiệu bản thân. Full-text searchable |
| 9 | `role` | String | ❌ | `AUTHOR` \| `ADMIN` \| `MODERATOR` | Role. Dùng để filter trong author search |
| 10 | `status` | String | ❌ | `ACTIVE` \| `INACTIVE` \| `BANNED` | Trạng thái tài khoản. Dùng để filter |
| 11 | `createdAt` | String | ❌ | ISO 8601 hoặc epoch millis | Ngày tạo tài khoản |
| 12 | `documentCount` | Long | ❌ | `>= 0` | Tổng số bài viết đã đăng |
| 13 | `followerCount` | Long | ❌ | `>= 0` | Số người theo dõi. Dùng cho Popular Authors ranking |

### 3.4. Yêu cầu theo từng Event Type

#### `AUTHOR_CREATED`

Bắn khi: **Tạo user mới / đăng ký tài khoản**

```json
{
  "eventType": "AUTHOR_CREATED",
  "id": "author-xyz789",                    // ✅ BẮT BUỘC
  "username": "john_doe",                    // ⚠️ NÊN CÓ (search + autocomplete)
  "displayName": "John Doe",                // ⚠️ NÊN CÓ (search + autocomplete)
  "email": "john@example.com",              // ❌ optional
  "phoneNumber": "0901234567",              // ❌ optional
  "avatarUrl": "https://example.com/a.jpg", // ❌ optional
  "bio": "Developer and writer",            // ❌ optional
  "role": "AUTHOR",                         // ⚠️ NÊN CÓ (filter)
  "status": "ACTIVE",                       // ⚠️ NÊN CÓ (filter)
  "createdAt": "2026-02-28T10:00:00Z",      // ❌ optional
  "documentCount": 0,                       // ❌ optional (default 0)
  "followerCount": 0                        // ❌ optional (default 0)
}
```

**Search Service xử lý:** Index vào `authors` index trong ES

---

#### `AUTHOR_UPDATED`

Bắn khi: **Cập nhật thông tin user** (đổi tên, email, avatar, role, status, v.v.)

```json
{
  "eventType": "AUTHOR_UPDATED",
  "id": "author-xyz789",                    // ✅ BẮT BUỘC — phải trùng ID cũ
  "username": "john_doe_updated",            // Gửi ĐẦY ĐỦ tất cả fields
  "displayName": "John Doe Updated",
  "email": "john.new@example.com",
  "phoneNumber": "0909876543",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "bio": "Updated bio text",
  "role": "ADMIN",
  "status": "ACTIVE",
  "createdAt": "2026-01-15T08:00:00Z",
  "documentCount": 20,
  "followerCount": 350
}
```

> ⚠️ **QUAN TRỌNG:** Gửi **ĐẦY ĐỦ** tất cả fields, không chỉ fields thay đổi.

**Search Service xử lý (2 bước):**

```
Bước 1: Update authors index
         → elasticsearchOperations.save(authorIndex)

Bước 2: Update ALL documents có author.id = "author-xyz789"
         → Tìm tất cả documents (phân trang 500 docs/batch)
         → Cập nhật embedded author object trong mỗi document:
            - author.id
            - author.username
            - author.displayName
            - author.email
            - author.avatarUrl
            - author.role
         → Bulk save
```

> 🔥 **Không cần bắn `DOCUMENT_UPDATED`** cho từng document khi chỉ thay đổi thông tin author. Search Service sẽ tự propagate.

---

#### `AUTHOR_DELETED`

Bắn khi: **Xóa user / Ban user**

```json
{
  "eventType": "AUTHOR_DELETED",
  "id": "author-xyz789"
}
```

Chỉ cần 2 trường.

**Search Service xử lý:** `authorSearchRepository.deleteById(id)` → xóa khỏi ES

> ⚠️ **Lưu ý:** Documents của author này **KHÔNG** tự động bị xóa. Nếu muốn xóa documents kèm theo, hãy bắn thêm `DOCUMENT_DELETED` cho từng document từ Document Service.

---

## 4. Dead Letter Topics

Khi consumer xử lý message thất bại sau 3 lần retry, message sẽ được tự động publish vào Dead Letter Topic:

| Original Topic | Dead Letter Topic |
|----------------|-------------------|
| `document-events` | `document-events.DLT` |
| `author-events` | `author-events.DLT` |

### Retry Flow

```
Message đến
    │
    ├── Xử lý OK → ✅ Commit offset
    │
    └── Xử lý FAIL
         ├── Retry 1 (chờ 1 giây)
         │    ├── OK → ✅ Commit
         │    └── FAIL
         │         ├── Retry 2 (chờ 1 giây)
         │         │    ├── OK → ✅ Commit
         │         │    └── FAIL
         │         │         ├── Retry 3 (chờ 1 giây)
         │         │         │    ├── OK → ✅ Commit
         │         │         │    └── FAIL
         │         │         │         └── 📭 Publish to .DLT topic
```

### Kiểm tra DLT messages

```bash
# Xem messages trong DLT
kafka-console-consumer --bootstrap-server localhost:7092 \
  --topic document-events.DLT --from-beginning
```

---

## 5. Producer Configuration

Service cần publish events **phải** cấu hình Kafka producer như sau:

### Spring Boot (application.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:7092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    properties:
      security.protocol: SASL_PLAINTEXT
      sasl.mechanism: PLAIN
      sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="horob1"
        password="2410";
```

### Spring Boot (application.properties)

```properties
spring.kafka.bootstrap-servers=localhost:7092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.properties.security.protocol=SASL_PLAINTEXT
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="horob1" password="2410";
```

### Yêu cầu bắt buộc

| # | Yêu cầu | Lý do |
|---|---------|-------|
| 1 | Key serializer = `StringSerializer` | Message key là String (entity ID) |
| 2 | Value serializer = `JsonSerializer` | Event payload là JSON |
| 3 | SASL_PLAINTEXT auth | Kafka broker yêu cầu |
| 4 | Message key = entity ID | Đảm bảo ordering per entity |

---

## 6. Ví dụ code Producer

### Kotlin / Spring Boot

```kotlin
// DTO
data class DocumentEvent(
    val eventType: String,
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val content: String? = null,
    val schoolId: String? = null,
    val schoolName: String? = null,
    val facultyId: String? = null,
    val facultyName: String? = null,
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val language: String? = null,
    val status: String? = null,
    val visibility: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publishedAt: String? = null,
    val viewCount: Long? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val score: Float? = null,
    val author: AuthorPayload? = null
)

data class AuthorPayload(
    val id: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null
)

data class AuthorEvent(
    val eventType: String,
    val id: String,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val documentCount: Long? = null,
    val followerCount: Long? = null
)

// Service
@Service
class DocumentEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publishCreated(document: Document) {
        val event = DocumentEvent(
            eventType = "DOCUMENT_CREATED",
            id = document.id,
            title = document.title,
            description = document.description,
            content = document.content,
            schoolId = document.schoolId,
            schoolName = document.schoolName,
            facultyId = document.facultyId,
            facultyName = document.facultyName,
            tags = document.tags,
            categories = document.categories,
            language = document.language,
            status = document.status,
            visibility = document.visibility,
            createdAt = document.createdAt.toString(),
            updatedAt = document.updatedAt.toString(),
            publishedAt = document.publishedAt?.toString(),
            viewCount = document.viewCount,
            likeCount = document.likeCount,
            commentCount = document.commentCount,
            score = document.score,
            author = AuthorPayload(
                id = document.author.id,
                username = document.author.username,
                displayName = document.author.displayName,
                email = document.author.email,
                avatarUrl = document.author.avatarUrl,
                role = document.author.role
            )
        )
        kafkaTemplate.send("document-events", document.id, event)
    }

    fun publishUpdated(document: Document) {
        // Tương tự publishCreated nhưng eventType = "DOCUMENT_UPDATED"
        // ⚠️ GỬI ĐẦY ĐỦ TẤT CẢ FIELDS
    }

    fun publishDeleted(documentId: String) {
        val event = DocumentEvent(
            eventType = "DOCUMENT_DELETED",
            id = documentId
        )
        kafkaTemplate.send("document-events", documentId, event)
    }
}

@Service
class AuthorEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publishCreated(user: User) {
        val event = AuthorEvent(
            eventType = "AUTHOR_CREATED",
            id = user.id,
            username = user.username,
            displayName = user.displayName,
            email = user.email,
            phoneNumber = user.phoneNumber,
            avatarUrl = user.avatarUrl,
            bio = user.bio,
            role = user.role,
            status = user.status,
            createdAt = user.createdAt.toString(),
            documentCount = user.documentCount,
            followerCount = user.followerCount
        )
        kafkaTemplate.send("author-events", user.id, event)
    }

    fun publishUpdated(user: User) {
        // Tương tự publishCreated nhưng eventType = "AUTHOR_UPDATED"
        // ⚠️ GỬI ĐẦY ĐỦ TẤT CẢ FIELDS
        // Search Service sẽ TỰ ĐỘNG update tất cả documents có author này
    }

    fun publishDeleted(userId: String) {
        val event = AuthorEvent(
            eventType = "AUTHOR_DELETED",
            id = userId
        )
        kafkaTemplate.send("author-events", userId, event)
    }
}
```

### Java / Spring Boot

```java
// Publish document event
DocumentEvent event = new DocumentEvent();
event.setEventType("DOCUMENT_CREATED");
event.setId(document.getId());
event.setTitle(document.getTitle());
event.setDescription(document.getDescription());
event.setContent(document.getContent());
event.setSchoolId(document.getSchoolId());
event.setSchoolName(document.getSchoolName());
event.setFacultyId(document.getFacultyId());
event.setFacultyName(document.getFacultyName());
event.setTags(document.getTags());
event.setCategories(document.getCategories());
event.setLanguage(document.getLanguage());
event.setStatus(document.getStatus());
event.setVisibility(document.getVisibility());
event.setCreatedAt(document.getCreatedAt().toString());
event.setUpdatedAt(document.getUpdatedAt().toString());
event.setPublishedAt(document.getPublishedAt() != null ? document.getPublishedAt().toString() : null);
event.setViewCount(document.getViewCount());
event.setLikeCount(document.getLikeCount());
event.setCommentCount(document.getCommentCount());
event.setScore(document.getScore());

AuthorPayload authorPayload = new AuthorPayload();
authorPayload.setId(document.getAuthor().getId());
authorPayload.setUsername(document.getAuthor().getUsername());
authorPayload.setDisplayName(document.getAuthor().getDisplayName());
authorPayload.setEmail(document.getAuthor().getEmail());
authorPayload.setAvatarUrl(document.getAuthor().getAvatarUrl());
authorPayload.setRole(document.getAuthor().getRole());
event.setAuthor(authorPayload);

kafkaTemplate.send("document-events", document.getId(), event);
```

---

## 7. Quy tắc & Best Practices

### ✅ NÊN

| # | Quy tắc |
|---|---------|
| 1 | Luôn gửi **ĐẦY ĐỦ** tất cả fields cho CREATE/UPDATE (Search Service overwrite toàn bộ) |
| 2 | Luôn dùng **entity ID** làm Kafka message key |
| 3 | Gửi date/time dạng **ISO 8601** (`2026-02-28T10:00:00Z`) |
| 4 | Gửi `status` và `role` dạng **UPPERCASE** (`PUBLISHED`, `ACTIVE`, `AUTHOR`) |
| 5 | Gửi `author` object đầy đủ khi CREATE/UPDATE document |
| 6 | Chỉ bắn `AUTHOR_UPDATED` khi thay đổi profile — Search Service tự propagate đến documents |
| 7 | Bắn event **sau khi** lưu thành công vào database chính |

### ❌ KHÔNG NÊN

| # | Quy tắc |
|---|---------|
| 1 | **KHÔNG** gửi partial update (chỉ fields thay đổi) — Search Service cần full payload |
| 2 | **KHÔNG** cần bắn `DOCUMENT_UPDATED` cho tất cả docs khi chỉ thay đổi thông tin author |
| 3 | **KHÔNG** gửi `eventType` sai chính tả — consumer sẽ log warning và bỏ qua |
| 4 | **KHÔNG** gửi `id` trống hoặc null — sẽ gây lỗi |
| 5 | **KHÔNG** gửi event **trước khi** lưu database (có thể gây inconsistency nếu DB fail) |

### ⚠️ Edge Cases

| Case | Hành vi Search Service |
|------|------------------------|
| Gửi `DOCUMENT_CREATED` với ID đã tồn tại | Ghi đè (upsert) |
| Gửi `DOCUMENT_UPDATED` với ID chưa tồn tại | Tạo mới (upsert) |
| Gửi `DOCUMENT_DELETED` với ID không tồn tại | Bỏ qua, không lỗi |
| Gửi `AUTHOR_UPDATED` nhưng author chưa có document | Chỉ update authors index, không propagate |
| Gửi `AUTHOR_DELETED` | Xóa author nhưng KHÔNG xóa documents của author |
| Gửi event với `eventType` không hợp lệ | Log warning, bỏ qua message |
| Kafka message value không parse được | Retry 3 lần → DLT |

