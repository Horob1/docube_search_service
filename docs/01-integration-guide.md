# 📘 Search Service — Hướng dẫn tích hợp (Integration Guide)

> **Service:** Search Service (SS_DEV1)  
> **Version:** 1.1.0  
> **Port:** 9111  
> **Base URL:** `http://localhost:9111`  
> **Cập nhật:** 22/03/2026

---

## 📑 Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc tích hợp](#2-kiến-trúc-tích-hợp)
3. [REST APIs](#3-rest-apis)
4. [Kafka Events — Đồng bộ dữ liệu](#4-kafka-events--đồng-bộ-dữ-liệu)
5. [Response Format](#5-response-format)
6. [Error Handling](#6-error-handling)
7. [Checklist tích hợp](#7-checklist-tích-hợp)

---

## 1. Tổng quan

Search Service là một microservice độc lập chịu trách nhiệm:
- **Tìm kiếm documents** (bài viết, tài liệu) với full-text search, highlight, fuzzy matching
- **Tìm kiếm authors** (người dùng) với autocomplete, fuzzy search
- **Đồng bộ dữ liệu** từ các service khác (Document Service, User Service) qua **Kafka events**

### Luồng dữ liệu

```
┌──────────────┐   Kafka Event    ┌──────────────────┐    Elasticsearch
│ Document Svc │ ───────────────> │  Search Service   │ ──────────────> ES Cluster
│ User Service │                  │  (Consumer)       │
└──────────────┘                  └──────────────────┘
                                          │
                                  REST API │ (Search)
                                          ▼
                                  ┌──────────────┐
                                  │   Client /   │
                                  │   Frontend   │
                                  └──────────────┘
```

> ⚠️ Search Service **KHÔNG** có database riêng. Mọi dữ liệu được nhận qua Kafka và lưu trong Elasticsearch.

---

## 2. Kiến trúc tích hợp

### Các service cần tích hợp

| Service | Kafka Topic | Mục đích |
|---------|-------------|----------|
| **Document Service** | `document-events` | Đồng bộ CRUD documents |
| **User Service** | `author-events` | Đồng bộ CRUD authors/users |

### Infrastructure cần có

| Thành phần | Mục đích |
|------------|----------|
| Kafka (localhost:7092) | Message broker |
| Elasticsearch (localhost:9112) | Search engine |
| Redis (localhost:6379) | Cache |
| Eureka (localhost:9000) | Service discovery |

---

## 3. REST APIs

### 3.1. Search Documents

```
GET /api/v1/search/documents
```

**Query Parameters:**

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `keyword` | String | ❌ | null | Từ khóa tìm kiếm (null = trả tất cả) |
| `page` | Integer | ❌ | 0 | Trang (bắt đầu từ 0) |
| `size` | Integer | ❌ | 10 | Số kết quả/trang (1-100) |
| `sortBy` | String | ❌ | relevance | `relevance` \| `createdAt` \| `viewCount` |
| `order` | String | ❌ | desc | `asc` \| `desc` |
| `status` | String | ❌ | null | Filter: `PUBLISHED` \| `DRAFT` \| `ARCHIVED` |
| `authorId` | String | ❌ | null | Filter theo author ID |
| `school_id` | String | ❌ | null | Filter chính xác theo school ID |
| `faculty_id` | String | ❌ | null | Filter chính xác theo faculty ID |

**Ví dụ:**

```http
GET /api/v1/search/documents?keyword=Cong+nghe+thong+tin&page=0&size=10&sortBy=relevance&school_id=HCMUS&faculty_id=CNTT
```

**Search logic:**
- MultiMatch trên: `title^6`, `school_name^3`, `faculty_name^3`, `content^1`
- Fuzzy search (`AUTO`)
- Filter term (nếu có): `status`, `author.id`, `school_id`, `faculty_id`
- Highlight trên: `title`, `content`, `school_name`, `faculty_name`
- Nếu `keyword` = null -> trả về `match_all` (tất cả documents)

---

### 3.2. Search Authors

```
GET /api/v1/search/authors
```

**Query Parameters:**

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `keyword` | String | ❌ | null | Từ khóa tìm kiếm |
| `page` | Integer | ❌ | 0 | Trang |
| `size` | Integer | ❌ | 10 | Kết quả/trang (1-100) |
| `role` | String | ❌ | null | Filter: `AUTHOR` \| `ADMIN` \| `MODERATOR` |
| `status` | String | ❌ | null | Filter: `ACTIVE` \| `INACTIVE` \| `BANNED` |

**Ví dụ:**

```http
GET /api/v1/search/authors?keyword=john&role=AUTHOR&status=ACTIVE
```

**Search logic:**
- MultiMatch trên: `displayName^2`, `username^2`, `email`
- Fuzzy search + Match phrase prefix (autocomplete)
- Highlight trên: `displayName`, `username`, `email`

---

### 3.3. Suggest (Autocomplete)

```
GET /api/v1/search/documents/suggest?q={prefix}&size={size}
```

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `q` | String | ✅ | - | Prefix tìm kiếm |
| `size` | Integer | ❌ | 5 | Số gợi ý |

**Response:**

```json
{
  "suggestions": [
    "Hướng dẫn Spring Boot từ A đến Z",
    "Hướng dẫn Docker cho người mới"
  ]
}
```

---

### 3.4. Trending Documents

```
GET /api/v1/search/documents/trending?size={size}
```

Trả về documents có `viewCount` cao nhất, filter `status=PUBLISHED`.

---

### 3.5. Popular Authors

```
GET /api/v1/search/authors/popular?size={size}
```

Trả về authors có `followerCount` cao nhất.

---

### 3.6. Admin — Reindex

```
POST /api/v1/admin/reindex/documents
POST /api/v1/admin/reindex/authors
```

> ⚠️ **Chỉ dành cho admin.** Xóa toàn bộ index và tạo lại. Dữ liệu cần được repopulate qua Kafka events.

---

### 3.7. Health Check

```
GET /actuator/health
```

Trả về trạng thái ES cluster, Redis, Kafka, Eureka.

---

### 3.8. Migration/Reindex khuyến nghị (không downtime)

Khi thay đổi mapping lớn (như thêm `school/faculty`), ưu tiên quy trình:

1. Tạo index mới version hóa (`documents_v2`).
2. Reindex dữ liệu từ index cũ.
3. Verify kết quả search/filter.
4. Alias swap atomically sang index mới.
5. Giữ index cũ 24-48 giờ để rollback.

> Chi tiết lệnh mẫu xem tại `docs/05-api-detail.md` (mục **Runbook Migration/Reindex**).

---

## 4. Kafka Events — Đồng bộ dữ liệu

### 4.1. Document Events

**Topic:** `document-events`

**Event Types:**

| Event Type | Khi nào bắn | Search Service xử lý |
|------------|-------------|----------------------|
| `DOCUMENT_CREATED` | Tạo document mới | Index document vào ES |
| `DOCUMENT_UPDATED` | Cập nhật document | Update document trong ES |
| `DOCUMENT_DELETED` | Xóa document | Xóa document khỏi ES |

**Payload (JSON):**

```json
{
  "eventType": "DOCUMENT_CREATED",
  "id": "doc-abc12345",
  "title": "Tiêu đề bài viết",
  "description": "Mô tả ngắn",
  "content": "Nội dung đầy đủ của bài viết...",
  "schoolId": "HCMUS",
  "schoolName": "Trường Đại học Khoa học Tự nhiên",
  "facultyId": "CNTT",
  "facultyName": "Công nghệ thông tin",
  "tags": ["spring", "java", "backend"],
  "categories": ["Backend"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:00:00Z",
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
    "avatarUrl": "https://example.com/avatar.jpg",
    "role": "AUTHOR"
  }
}
```

**Chi tiết các trường:**

| Trường | Type | Required | Mô tả |
|--------|------|----------|--------|
| `eventType` | String | ✅ | `DOCUMENT_CREATED` \| `DOCUMENT_UPDATED` \| `DOCUMENT_DELETED` |
| `id` | String | ✅ | ID duy nhất của document |
| `title` | String | ❌* | Tiêu đề (required cho CREATE/UPDATE) |
| `description` | String | ❌ | Mô tả ngắn |
| `content` | String | ❌ | Nội dung đầy đủ |
| `schoolId` | String | ❌ | Mã trường (sẽ map thành `school_id`) |
| `schoolName` | String | ❌ | Tên trường (sẽ map thành `school_name`) |
| `facultyId` | String | ❌ | Mã khoa (sẽ map thành `faculty_id`) |
| `facultyName` | String | ❌ | Tên khoa (sẽ map thành `faculty_name`) |
| `tags` | List<String> | ❌ | Tags |
| `categories` | List<String> | ❌ | Danh mục |
| `language` | String | ❌ | Mã ngôn ngữ (vi, en...) |
| `status` | String | ❌ | `PUBLISHED` \| `DRAFT` \| `ARCHIVED` |
| `visibility` | String | ❌ | `PUBLIC` \| `PRIVATE` \| `UNLISTED` |
| `createdAt` | String (ISO 8601) | ❌ | Ngày tạo |
| `updatedAt` | String (ISO 8601) | ❌ | Ngày cập nhật |
| `publishedAt` | String (ISO 8601) | ❌ | Ngày xuất bản |
| `viewCount` | Long | ❌ | Lượt xem |
| `likeCount` | Long | ❌ | Lượt thích |
| `commentCount` | Long | ❌ | Số comment |
| `score` | Float | ❌ | Điểm đánh giá |
| `author` | Object | ❌* | Thông tin tác giả (required cho CREATE/UPDATE) |

> 💡 Với event `DOCUMENT_DELETED`, chỉ cần gửi `eventType` và `id`.

---

### 4.2. Author Events

**Topic:** `author-events`

**Event Types:**

| Event Type | Khi nào bắn | Search Service xử lý |
|------------|-------------|----------------------|
| `AUTHOR_CREATED` | Tạo user mới | Index author vào ES |
| `AUTHOR_UPDATED` | Cập nhật user | Update author trong ES **+ update tất cả documents có author.id tương ứng** |
| `AUTHOR_DELETED` | Xóa/ban user | Xóa author khỏi ES |

**Payload (JSON):**

```json
{
  "eventType": "AUTHOR_CREATED",
  "id": "author-xyz789",
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "Passionate developer and writer",
  "role": "AUTHOR",
  "status": "ACTIVE",
  "createdAt": "2026-02-28T10:00:00Z",
  "documentCount": 15,
  "followerCount": 200
}
```

**Chi tiết các trường:**

| Trường | Type | Required | Mô tả |
|--------|------|----------|--------|
| `eventType` | String | ✅ | `AUTHOR_CREATED` \| `AUTHOR_UPDATED` \| `AUTHOR_DELETED` |
| `id` | String | ✅ | ID duy nhất |
| `username` | String | ❌* | Username (required cho CREATE/UPDATE) |
| `displayName` | String | ❌ | Tên hiển thị |
| `email` | String | ❌ | Email |
| `phoneNumber` | String | ❌ | Số điện thoại |
| `avatarUrl` | String | ❌ | URL avatar |
| `bio` | String | ❌ | Giới thiệu |
| `role` | String | ❌ | `AUTHOR` \| `ADMIN` \| `MODERATOR` |
| `status` | String | ❌ | `ACTIVE` \| `INACTIVE` \| `BANNED` |
| `createdAt` | String (ISO 8601) | ❌ | Ngày tạo |
| `documentCount` | Long | ❌ | Số bài viết |
| `followerCount` | Long | ❌ | Số người theo dõi |

> 💡 Với event `AUTHOR_DELETED`, chỉ cần gửi `eventType` và `id`.

> 🔥 **QUAN TRỌNG:** Khi bắn `AUTHOR_UPDATED`, Search Service sẽ tự động cập nhật **tất cả documents** có `author.id` tương ứng. Không cần bắn thêm `DOCUMENT_UPDATED` cho từng document.

---

### 4.3. Kafka Configuration yêu cầu

Các service producer cần cấu hình:

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
        username="horob1" password="2410";
```

**Key:** Sử dụng `id` của entity (document ID hoặc author ID) làm Kafka message key.

```kotlin
// Ví dụ code producer
kafkaTemplate.send("document-events", document.id, documentEvent)
kafkaTemplate.send("author-events", author.id, authorEvent)
```

---

## 5. Response Format

### Pagination Response

Tất cả API search trả về format:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "hasNext": true
}
```

### Document Response

```json
{
  "id": "doc-abc12345",
  "title": "Tiêu đề",
  "description": "Mô tả",
  "content": "Nội dung...",
  "schoolId": "HCMUS",
  "schoolName": "Trường Đại học Khoa học Tự nhiên",
  "facultyId": "CNTT",
  "facultyName": "Công nghệ thông tin",
  "tags": ["spring", "java"],
  "categories": ["Backend"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:00:00Z",
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
    "avatarUrl": "https://example.com/avatar.jpg",
    "role": "AUTHOR"
  },
  "highlights": {
    "title": ["Tiêu đề có <em>từ khóa</em> highlight"],
    "school_name": ["<em>Truong Dai hoc</em> Khoa hoc Tu nhien"],
    "faculty_name": ["<em>Cong nghe thong tin</em>"],
    "content": ["Noi dung co <em>tu khoa</em>"]
  }
}
```

### Author Response

```json
{
  "id": "author-xyz789",
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "Developer",
  "role": "AUTHOR",
  "status": "ACTIVE",
  "createdAt": "2026-02-28T10:00:00Z",
  "documentCount": 15,
  "followerCount": 200,
  "highlights": {
    "displayName": ["<em>John</em> Doe"]
  }
}
```

---

## 6. Error Handling

### Error Response Format

```json
{
  "timestamp": "2026-02-28T10:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "size: must be less than or equal to 100",
  "path": "/api/v1/search/documents"
}
```

### HTTP Status Codes

| Code | Ý nghĩa |
|------|---------|
| 200 | Thành công |
| 202 | Accepted (reindex triggered) |
| 400 | Bad Request — validation error, missing param |
| 404 | Not Found |
| 500 | Internal Server Error — search error |
| 503 | Service Unavailable — ES hoặc Kafka down |

### Kafka Error Handling

- Consumer tự retry **3 lần**, mỗi lần cách **1 giây**
- Sau 3 lần thất bại → message được gửi vào **Dead Letter Topic** (`document-events.DLT` hoặc `author-events.DLT`)
- Log chi tiết mỗi lần retry

---

## 7. Checklist tích hợp

### Cho Document Service

- [ ] Khi tạo document → bắn `DOCUMENT_CREATED` event vào topic `document-events`
- [ ] Khi update document → bắn `DOCUMENT_UPDATED` event (gửi đầy đủ thông tin, kể cả author)
- [ ] Khi xóa document → bắn `DOCUMENT_DELETED` event (chỉ cần `eventType` + `id`)
- [ ] Key = document ID
- [ ] Cấu hình Kafka producer với SASL_PLAINTEXT

### Cho User Service

- [ ] Khi tạo user → bắn `AUTHOR_CREATED` event vào topic `author-events`
- [ ] Khi update user → bắn `AUTHOR_UPDATED` event (đầy đủ thông tin)
- [ ] Khi xóa/ban user → bắn `AUTHOR_DELETED` event (chỉ cần `eventType` + `id`)
- [ ] Key = user/author ID
- [ ] Cấu hình Kafka producer với SASL_PLAINTEXT

### Cho Frontend/Client

- [ ] Gọi `GET /api/v1/search/documents` để search bài viết
- [ ] Truyền thêm `school_id` / `faculty_id` khi cần lọc chính xác theo trường/khoa
- [ ] Gọi `GET /api/v1/search/authors` để search tác giả
- [ ] Gọi `GET /api/v1/search/documents/suggest` cho autocomplete
- [ ] Xử lý pagination response format
- [ ] Render HTML highlight tags (`<em>...</em>`)
