# 🌐 Search Service — API Reference chi tiết

> **Base URL:** `http://localhost:9111`  
> **Content-Type:** `application/json`  
> **Cập nhật:** 22/03/2026

---

## 📑 Mục lục

1. [Search Documents](#1-search-documents)
2. [Search Authors](#2-search-authors)
3. [Suggest (Autocomplete)](#3-suggest-autocomplete)
4. [Trending Documents](#4-trending-documents)
5. [Popular Authors](#5-popular-authors)
6. [Admin — Reindex Documents](#6-admin--reindex-documents)
7. [Admin — Reindex Authors](#7-admin--reindex-authors)
8. [Health Check](#8-health-check)
9. [Test — Push Document Event (dev only)](#9-test--push-document-event-dev-only)
10. [Test — Push Author Event (dev only)](#10-test--push-author-event-dev-only)
11. [Test — Generate Sample Documents (dev only)](#11-test--generate-sample-documents-dev-only)
12. [Test — Generate Sample Authors (dev only)](#12-test--generate-sample-authors-dev-only)

---

## 1. Search Documents

### Request

```
GET /api/v1/search/documents
```

### Query Parameters

| Param | Type | Required | Default | Constraints | Mô tả |
|-------|------|----------|---------|-------------|--------|
| `keyword` | String | ❌ | `null` | — | Từ khóa tìm kiếm. Null = trả tất cả (`match_all`) |
| `page` | Integer | ❌ | `0` | `>= 0` | Số trang (0-based) |
| `size` | Integer | ❌ | `10` | `1 – 100` | Số kết quả mỗi trang |
| `sortBy` | String | ❌ | `relevance` | `relevance` \| `createdAt` \| `viewCount` | Trường sắp xếp |
| `order` | String | ❌ | `desc` | `asc` \| `desc` | Hướng sắp xếp |
| `status` | String | ❌ | `null` | `PUBLISHED` \| `DRAFT` \| `ARCHIVED` | Lọc theo status |
| `authorId` | String | ❌ | `null` | — | Lọc theo author ID |
| `school_id` | String | ❌ | `null` | — | Lọc chính xác theo school ID (`term`) |
| `faculty_id` | String | ❌ | `null` | — | Lọc chính xác theo faculty ID (`term`) |

### Search Logic

- **MultiMatch** trên các trường (có boost):
  - `title` - boost x6 (cao nhất)
  - `school_name` - boost x3
  - `faculty_name` - boost x3
  - `content` - boost x1
- **Type:** `BEST_FIELDS`
- **Fuzziness:** `AUTO`
- **Filter:** `term` cho `status`, `author.id`, `school_id`, `faculty_id` (nếu được truyền)
- **Highlight:** `title`, `content`, `school_name`, `faculty_name` (tag: `<em>...</em>`, fragment: 150 chars, max 3 fragments)

### Ví dụ Request

```http
GET /api/v1/search/documents?keyword=Cong+nghe+thong+tin&page=0&size=10&sortBy=relevance&order=desc&school_id=HCMUS&faculty_id=CNTT
```

```powershell
curl "http://localhost:9111/api/v1/search/documents?keyword=Cong+nghe+thong+tin&page=0&size=10&sortBy=relevance&school_id=HCMUS&faculty_id=CNTT"
```

### Response `200 OK`

```json
{
  "content": [
    {
      "id": "doc-abc12345",
      "title": "Gioi thieu nganh Cong nghe thong tin",
      "description": "Tong hop tai lieu cho tan sinh vien CNTT",
      "content": "Cong nghe thong tin la mot nganh trong diem...",
      "schoolId": "HCMUS",
      "schoolName": "Truong Dai hoc Khoa hoc Tu nhien",
      "facultyId": "CNTT",
      "facultyName": "Cong nghe thong tin",
      "tags": ["cntt", "huong-nghiep"],
      "categories": ["Hoc tap"],
      "language": "vi",
      "status": "PUBLISHED",
      "visibility": "PUBLIC",
      "createdAt": "2026-03-22T10:00:00Z",
      "updatedAt": "2026-03-22T10:30:00Z",
      "publishedAt": "2026-03-22T10:00:00Z",
      "viewCount": 2345,
      "likeCount": 89,
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
        "title": ["Gioi thieu nganh <em>Cong nghe thong tin</em>"],
        "school_name": ["Truong Dai hoc Khoa hoc Tu nhien"],
        "faculty_name": ["<em>Cong nghe thong tin</em>"],
        "content": ["... nganh <em>cong nghe thong tin</em> co nhu cau cao ..."]
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1,
  "hasNext": false
}
```

### Response Fields - `content[]` (DocumentSearchResponse)

| Field | Type | Nullable | Mô tả |
|-------|------|----------|-------|
| `id` | String | ❌ | ID duy nhất của document |
| `title` | String | ✅ | Tiêu đề |
| `description` | String | ✅ | Mô tả ngắn |
| `content` | String | ✅ | Nội dung đầy đủ |
| `schoolId` | String | ✅ | ID trường |
| `schoolName` | String | ✅ | Tên trường |
| `facultyId` | String | ✅ | ID khoa |
| `facultyName` | String | ✅ | Tên khoa |
| `tags` | List<String> | ✅ | Danh sách tags |
| `categories` | List<String> | ✅ | Danh sách categories |
| `language` | String | ✅ | Mã ngôn ngữ (`vi`, `en`, ...) |
| `status` | String | ✅ | `PUBLISHED` \| `DRAFT` \| `ARCHIVED` |
| `visibility` | String | ✅ | `PUBLIC` \| `PRIVATE` \| `UNLISTED` |
| `createdAt` | String (ISO 8601) | ✅ | Ngày tạo |
| `updatedAt` | String (ISO 8601) | ✅ | Ngày cập nhật |
| `publishedAt` | String (ISO 8601) | ✅ | Ngày xuất bản |
| `viewCount` | Long | ✅ | Lượt xem |
| `likeCount` | Long | ✅ | Lượt thích |
| `commentCount` | Long | ✅ | Số comment |
| `score` | Float | ✅ | Điểm đánh giá |
| `author` | Object | ✅ | Thông tin tác giả (embedded) |
| `highlights` | Map<String, List<String>> | ✅ | Highlight HTML fragments |

### Response Fields — Pagination Wrapper

| Field | Type | Mô tả |
|-------|------|--------|
| `content` | List | Danh sách kết quả |
| `page` | Integer | Trang hiện tại (0-based) |
| `size` | Integer | Số kết quả mỗi trang |
| `totalElements` | Long | Tổng số kết quả |
| `totalPages` | Integer | Tổng số trang |
| `hasNext` | Boolean | Còn trang tiếp không |

### Validation Errors `400`

```json
{
  "timestamp": "2026-03-22T10:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "size: must be less than or equal to 100",
  "path": "/api/v1/search/documents"
}
```

---

## 2. Search Authors

### Request

```
GET /api/v1/search/authors
```

### Query Parameters

| Param | Type | Required | Default | Constraints | Mô tả |
|-------|------|----------|---------|-------------|--------|
| `keyword` | String | ❌ | `null` | — | Từ khóa. Null = trả tất cả |
| `page` | Integer | ❌ | `0` | `>= 0` | Trang |
| `size` | Integer | ❌ | `10` | `1 – 100` | Kết quả/trang |
| `role` | String | ❌ | `null` | `AUTHOR` \| `ADMIN` \| `MODERATOR` | Lọc theo role |
| `status` | String | ❌ | `null` | `ACTIVE` \| `INACTIVE` \| `BANNED` | Lọc theo status |

### Search Logic

- **MultiMatch** (fuzziness=AUTO):
  - `displayName` — boost ×2
  - `username` — boost ×2
  - `email` — boost ×1
- **MatchPhrasePrefix** trên `displayName`, `username` (autocomplete behavior)
- `minimumShouldMatch = 1`
- **Highlight:** `displayName`, `username`, `email`

### Ví dụ Request

```http
GET /api/v1/search/authors?keyword=john&page=0&size=10&role=AUTHOR&status=ACTIVE
```

```powershell
curl "http://localhost:9111/api/v1/search/authors?keyword=john&role=AUTHOR&status=ACTIVE"
```

### Response `200 OK`

```json
{
  "content": [
    {
      "id": "author-xyz789",
      "username": "john_doe",
      "displayName": "John Doe",
      "email": "john@example.com",
      "phoneNumber": "0901234567",
      "avatarUrl": "https://example.com/avatar.jpg",
      "bio": "Passionate developer and writer",
      "role": "AUTHOR",
      "status": "ACTIVE",
      "createdAt": "2026-01-15T08:00:00Z",
      "documentCount": 15,
      "followerCount": 200,
      "highlights": {
        "displayName": ["<em>John</em> Doe"],
        "username": ["<em>john</em>_doe"]
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

### Response Fields — `content[]` (AuthorSearchResponse)

| Field | Type | Nullable | Mô tả |
|-------|------|----------|--------|
| `id` | String | ❌ | ID duy nhất |
| `username` | String | ✅ | Username |
| `displayName` | String | ✅ | Tên hiển thị |
| `email` | String | ✅ | Email |
| `phoneNumber` | String | ✅ | Số điện thoại |
| `avatarUrl` | String | ✅ | URL avatar |
| `bio` | String | ✅ | Bio / giới thiệu |
| `role` | String | ✅ | `AUTHOR` \| `ADMIN` \| `MODERATOR` |
| `status` | String | ✅ | `ACTIVE` \| `INACTIVE` \| `BANNED` |
| `createdAt` | String (ISO 8601) | ✅ | Ngày tạo tài khoản |
| `documentCount` | Long | ✅ | Số bài viết |
| `followerCount` | Long | ✅ | Số followers |
| `highlights` | Map\<String, List\<String\>\> | ✅ | Highlight HTML fragments |

---

## 3. Suggest (Autocomplete)

### Request

```
GET /api/v1/search/documents/suggest
```

### Query Parameters

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `q` | String | ✅ | — | Prefix tìm kiếm (vd: "Hướng dẫn") |
| `size` | Integer | ❌ | `5` | Số gợi ý trả về (1-20) |

### Search Logic

- **MatchPhrasePrefix** trên field `title`
- Trả về danh sách title distinct

### Ví dụ Request

```http
GET /api/v1/search/documents/suggest?q=Hướng+dẫn&size=5
```

```powershell
curl "http://localhost:9111/api/v1/search/documents/suggest?q=Spring&size=5"
```

### Response `200 OK`

```json
{
  "suggestions": [
    "Hướng dẫn Spring Boot từ A đến Z",
    "Hướng dẫn Docker cho người mới",
    "Hướng dẫn sử dụng Elasticsearch"
  ]
}
```

### Response Fields

| Field | Type | Mô tả |
|-------|------|--------|
| `suggestions` | List\<String\> | Danh sách tiêu đề gợi ý (distinct) |

### Caching

- Cache name: `suggest`
- Cache key: `{prefix}_{size}`
- TTL: **10 phút**
- Tự xóa cache khi có document mới index hoặc bị xóa

---

## 4. Trending Documents

### Request

```
GET /api/v1/search/documents/trending
```

### Query Parameters

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `size` | Integer | ❌ | `10` | Số kết quả (1-50) |

### Search Logic

- Filter: `status = PUBLISHED`
- Sort: `viewCount DESC`

### Ví dụ Request

```http
GET /api/v1/search/documents/trending?size=5
```

### Response `200 OK`

```json
[
  {
    "id": "doc-abc12345",
    "title": "Elasticsearch Deep Dive",
    "description": "Tìm hiểu sâu về Elasticsearch...",
    "content": "...",
    "tags": ["elasticsearch", "search"],
    "categories": ["Search"],
    "language": "vi",
    "status": "PUBLISHED",
    "visibility": "PUBLIC",
    "createdAt": "2026-02-20T08:00:00Z",
    "updatedAt": "2026-02-25T10:00:00Z",
    "publishedAt": "2026-02-20T08:00:00Z",
    "viewCount": 4521,
    "likeCount": 234,
    "commentCount": 45,
    "score": 4.8,
    "author": {
      "id": "author-xyz789",
      "username": "john_doe",
      "displayName": "John Doe",
      "email": "john@example.com",
      "avatarUrl": "https://example.com/avatar.jpg",
      "role": "AUTHOR"
    },
    "highlights": null
  }
]
```

### Response

Trả về `List<DocumentSearchResponse>` (không có pagination wrapper).

### Caching

- Cache name: `trending`
- Cache key: `{size}`
- TTL: **5 phút**

---

## 5. Popular Authors

### Request

```
GET /api/v1/search/authors/popular
```

### Query Parameters

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|--------|
| `size` | Integer | ❌ | `10` | Số kết quả (1-50) |

### Search Logic

- `match_all`
- Sort: `followerCount DESC`

### Ví dụ Request

```http
GET /api/v1/search/authors/popular?size=5
```

### Response `200 OK`

```json
[
  {
    "id": "author-aaa111",
    "username": "alice_wonder",
    "displayName": "Alice Wonder",
    "email": "alice@example.com",
    "phoneNumber": "0909876543",
    "avatarUrl": "https://i.pravatar.cc/150?u=alice",
    "bio": "Passionate about clean code and TDD",
    "role": "AUTHOR",
    "status": "ACTIVE",
    "createdAt": "2025-06-01T08:00:00Z",
    "documentCount": 42,
    "followerCount": 980,
    "highlights": null
  }
]
```

### Response

Trả về `List<AuthorSearchResponse>` (không có pagination wrapper).

### Caching

- Cache name: `popular`
- Cache key: `{size}`
- TTL: **10 phút**

---

## 6. Admin — Reindex Documents

### Request

```
POST /api/v1/admin/reindex/documents
```

**Body:** không cần

### Hành vi

1. Xóa index `documents` hiện tại
2. Tạo lại index với mapping + custom analyzer
3. Xóa cache `suggest`, `trending`

> ⚠️ Sau khi reindex, index trống. Dữ liệu cần được repopulate qua Kafka events.

### Response `202 Accepted`

```json
{
  "message": "Document index recreated. Data will be repopulated via Kafka events."
}
```

### Runbook Migration/Reindex (Alias Swap + Rollback)

Để tránh downtime khi thay đổi mapping (ví dụ thêm `school_id`, `school_name`, `faculty_id`, `faculty_name`), nên dùng chiến lược index version hóa + alias.

#### Bước 1: Tạo index mới

```http
PUT /documents_v2
{
  "settings": { "number_of_shards": 1, "number_of_replicas": 1 },
  "mappings": {
    "properties": {
      "school_id": { "type": "keyword" },
      "school_name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "faculty_id": { "type": "keyword" },
      "faculty_name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } }
    }
  }
}
```

#### Bước 2: Reindex dữ liệu

```http
POST /_reindex
{
  "source": { "index": "documents_v1" },
  "dest": { "index": "documents_v2" }
}
```

#### Bước 3: Kiểm tra trước cutover

- So sánh `_count` giữa `documents_v1` và `documents_v2`.
- Chạy thử các truy vấn chính: keyword search, filter `school_id`, filter `faculty_id`, highlight.

#### Bước 4: Alias swap atomically

```http
POST /_aliases
{
  "actions": [
    { "remove": { "index": "documents_v1", "alias": "documents" } },
    { "add": { "index": "documents_v2", "alias": "documents" } }
  ]
}
```

#### Bước 5: Rollback (nếu cần)

```http
POST /_aliases
{
  "actions": [
    { "remove": { "index": "documents_v2", "alias": "documents" } },
    { "add": { "index": "documents_v1", "alias": "documents" } }
  ]
}
```

Khuyến nghị giữ index cũ ít nhất 24-48 giờ trước khi xóa hẳn.

---

## 7. Admin — Reindex Authors

### Request

```
POST /api/v1/admin/reindex/authors
```

**Body:** không cần

### Hành vi

1. Xóa index `authors` hiện tại
2. Tạo lại index với mapping + custom analyzer
3. Xóa cache `popular`

### Response `202 Accepted`

```json
{
  "message": "Author index recreated. Data will be repopulated via Kafka events."
}
```

---

## 8. Health Check

### Request

```
GET /actuator/health
```

### Response `200 OK`

```json
{
  "status": "UP",
  "components": {
    "discoveryComposite": {
      "status": "UP",
      "details": { "discoveryClient": { "status": "UP" } }
    },
    "elasticsearchCluster": {
      "status": "UP",
      "details": {
        "clusterName": "docube-es-cluster",
        "status": "green",
        "numberOfNodes": 3,
        "activeShards": 10,
        "activePrimaryShards": 5
      }
    },
    "redis": {
      "status": "UP",
      "details": { "version": "7.2.4" }
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

### Trạng thái

| Status | Ý nghĩa |
|--------|---------|
| `UP` | Tất cả components hoạt động |
| `DOWN` | Ít nhất 1 component lỗi |

---

## 9. Test — Push Document Event (dev only)

> ⚠️ Chỉ hoạt động khi `spring.profiles.active=dev`

### Request

```
POST /api/v1/test/kafka/document
Content-Type: application/json
```

### Body

```json
{
  "eventType": "DOCUMENT_CREATED",
  "id": "doc-test-001",
  "title": "Test Document Title",
  "description": "Test description",
  "content": "Full content of the test document...",
  "tags": ["test", "sample"],
  "categories": ["General"],
  "language": "vi",
  "status": "PUBLISHED",
  "visibility": "PUBLIC",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:00:00Z",
  "publishedAt": "2026-02-28T10:00:00Z",
  "viewCount": 100,
  "likeCount": 10,
  "commentCount": 5,
  "score": 4.0,
  "author": {
    "id": "author-001",
    "username": "test_user",
    "displayName": "Test User",
    "email": "test@example.com",
    "avatarUrl": "https://example.com/avatar.jpg",
    "role": "AUTHOR"
  }
}
```

### Response `200 OK`

```json
{
  "status": "sent",
  "topic": "document-events",
  "eventType": "DOCUMENT_CREATED",
  "id": "doc-test-001"
}
```

---

## 10. Test — Push Author Event (dev only)

### Request

```
POST /api/v1/test/kafka/author
Content-Type: application/json
```

### Body

```json
{
  "eventType": "AUTHOR_CREATED",
  "id": "author-test-001",
  "username": "test_user",
  "displayName": "Test User",
  "email": "test@example.com",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "A test author bio",
  "role": "AUTHOR",
  "status": "ACTIVE",
  "createdAt": "2026-02-28T10:00:00Z",
  "documentCount": 10,
  "followerCount": 50
}
```

### Response `200 OK`

```json
{
  "status": "sent",
  "topic": "author-events",
  "eventType": "AUTHOR_CREATED",
  "id": "author-test-001"
}
```

---

## 11. Test — Generate Sample Documents (dev only)

### Request

```
POST /api/v1/test/generate/documents?count={count}
```

| Param | Type | Default | Mô tả |
|-------|------|---------|--------|
| `count` | Integer | `5` | Số documents mẫu (tối đa 10) |

Tự động tạo 1 author + N documents với dữ liệu mẫu tiếng Việt.

### Response `200 OK`

```json
{
  "status": "generated",
  "documentsCount": 5,
  "authorId": "author-a1b2c3d4",
  "documentIds": ["doc-e5f6g7h8", "doc-i9j0k1l2", "..."]
}
```

---

## 12. Test — Generate Sample Authors (dev only)

### Request

```
POST /api/v1/test/generate/authors?count={count}
```

| Param | Type | Default | Mô tả |
|-------|------|---------|--------|
| `count` | Integer | `5` | Số authors mẫu (tối đa 10) |

### Response `200 OK`

```json
{
  "status": "generated",
  "count": 5,
  "authorIds": ["author-a1b2c3d4", "author-e5f6g7h8", "..."]
}
```

---

## 📝 Error Response Format (tất cả API)

Khi có lỗi, tất cả API trả về format thống nhất:

```json
{
  "timestamp": "2026-02-28T10:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Chi tiết lỗi",
  "path": "/api/v1/search/documents"
}
```

| Field | Type | Mô tả |
|-------|------|--------|
| `timestamp` | String (ISO 8601) | Thời điểm lỗi |
| `status` | Integer | HTTP status code |
| `error` | String | Loại lỗi |
| `message` | String | Chi tiết lỗi |
| `path` | String | Request path |

### HTTP Status Codes

| Code | Khi nào |
|------|---------|
| `200` | Thành công |
| `202` | Accepted (reindex) |
| `400` | Validation lỗi, missing param, bad request |
| `404` | Not found |
| `500` | Search error, internal error |
| `503` | Elasticsearch hoặc Kafka không khả dụng |

