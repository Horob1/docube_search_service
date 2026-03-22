# 🧪 Search Service — Hướng dẫn kiểm thử (Testing Guide)

> **Service:** Search Service (SS_DEV1)  
> **Port:** 9111  
> **Test UI:** `http://localhost:9111` (chỉ khi profile = dev)  
> **Cập nhật:** 22/03/2026

---

## 📑 Mục lục

1. [Yêu cầu môi trường](#1-yêu-cầu-môi-trường)
2. [Khởi động service](#2-khởi-động-service)
3. [Test UI Dashboard](#3-test-ui-dashboard)
4. [Kịch bản kiểm thử](#4-kịch-bản-kiểm-thử)
5. [Test bằng cURL](#5-test-bằng-curl)
6. [Kiểm tra logs](#6-kiểm-tra-logs)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Yêu cầu môi trường

Trước khi test, đảm bảo các service sau đang chạy:

| Service | Host:Port | Kiểm tra |
|---------|-----------|----------|
| Kafka | localhost:7092 | Kafka broker + SASL auth |
| Elasticsearch | localhost:9112 | `curl -u elastic:horob1@2410 http://localhost:9112/_cluster/health` |
| Redis | localhost:6379 | `redis-cli -a 2410 ping` |
| Eureka | localhost:9000 | `http://localhost:9000` (optional) |

### Khởi động Elasticsearch cluster

```powershell
# Tạo network (1 lần)
docker network create horob1_docub

# Chạy ES cluster
cd docube_search_search
docker compose up -d

# Kiểm tra
curl -u elastic:horob1@2410 http://localhost:9112/_cluster/health?pretty
```

---

## 2. Khởi động service

```powershell
# Build
.\gradlew.bat clean build -x test

# Chạy (profile dev mặc định)
.\gradlew.bat bootRun

# Hoặc chỉ định profile rõ ràng
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Khi service khởi động thành công, bạn sẽ thấy log:

```
✅ Created index 'documents' with mappings and settings
✅ Created index 'authors' with mappings and settings
```

---

## 3. Test UI Dashboard

Mở trình duyệt: **http://localhost:9111**

> ⚠️ Test UI chỉ hoạt động khi `spring.profiles.active=dev`. Trong production, UI và test endpoints bị vô hiệu hóa.

### Các tab trong Dashboard

| Tab | Mô tả |
|-----|-------|
| 🧪 Generate Data | Tạo dữ liệu mẫu (push qua Kafka) |
| 📄 Push Document Event | Push 1 document event vào Kafka |
| 👤 Push Author Event | Push 1 author event vào Kafka |
| 🔍 Search Documents | Test search documents API |
| 🔍 Search Authors | Test search authors API |
| ⚡ Suggest / Trending / Popular | Test autocomplete, trending, popular |
| 🔧 Admin | Reindex (xóa + tạo lại index) |
| 🩺 Health | Xem trạng thái hệ thống |

---

## 4. Kịch bản kiểm thử

### Test Case 1: Generate + Search Documents

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Generate Data" -> nhấn "Generate 5 Documents + 1 Author" | Response: `status: generated`, 5 document IDs |
| 2 | Chờ 2-3 giây (Kafka consumer xử lý) | Log hiện: `Indexed new document: doc-xxx` |
| 3 | Tab "Search Documents" -> keyword = "Công nghệ thông tin" -> nhấn Search | Kết quả có tài liệu liên quan hoặc thuộc khoa CNTT |
| 4 | Truyền `school_id=HCMUS` và `faculty_id=CNTT` | Chỉ trả về tài liệu đúng trường/khoa |
| 5 | keyword = "" (trống) -> Search | Trả về tất cả documents (match_all) |
| 6 | Filter status = "DRAFT" | Kết quả: 0 (vì sample data đều PUBLISHED) |

### Test Case 2: Search Authors

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Generate Data" → nhấn "Generate 5 Authors" | Response: 5 author IDs |
| 2 | Chờ 2-3 giây | Log hiện: `✅ Indexed new author: author-xxx` |
| 3 | Tab "Search Authors" → keyword = "alice" | Kết quả: "Alice Wonder" với highlight |
| 4 | keyword = "alce" (lỗi chính tả) | Kết quả: "Alice Wonder" (fuzzy search) |
| 5 | Filter role = "ADMIN" | Chỉ authors có role ADMIN |

### Test Case 3: Suggest (Autocomplete)

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Đã generate documents ở Test Case 1 | — |
| 2 | Tab "Suggest/Trending" → gõ "Hướng" | Gợi ý: "Hướng dẫn Spring Boot từ A đến Z" |
| 3 | Gõ "Elastic" | Gợi ý: "Elasticsearch Deep Dive" |

### Test Case 4: Trending Documents

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Suggest/Trending" → nhấn "Get Trending" | Danh sách documents sắp xếp theo viewCount giảm dần |
| 2 | Document có viewCount cao nhất ở đầu | ✅ |

### Test Case 5: Popular Authors

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Suggest/Trending" → nhấn "Get Popular" | Danh sách authors sắp xếp theo followerCount giảm dần |

### Test Case 6: Push Custom Document Event

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Push Document Event" → nhấn "Fill Sample Data" | Form tự điền dữ liệu mẫu |
| 2 | Event Type = DOCUMENT_CREATED → nhấn "Push Event" | Response: `status: sent` |
| 3 | Chờ 1-2s → Search Documents → tìm theo title | Document xuất hiện trong kết quả |
| 4 | Đổi Event Type = DOCUMENT_UPDATED, sửa title → Push | Document được cập nhật |
| 5 | Đổi Event Type = DOCUMENT_DELETED → Push | Document bị xóa khỏi kết quả search |

### Test Case 7: Author Update → Propagation to Documents

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Generate 5 documents (tạo author "John Doe") | 5 docs + 1 author |
| 2 | Tab "Push Author Event" → dùng cùng author ID | — |
| 3 | Event Type = AUTHOR_UPDATED, displayName = "Jane Doe" | Push event |
| 4 | Chờ 3-5s | Log: `🔄 Propagated author update to documents for author: xxx` |
| 5 | Search Documents → kiểm tra author name | Tất cả documents giờ hiện "Jane Doe" |

### Test Case 8: Reindex

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Admin" → nhấn "Reindex Documents" | Response: `Document index recreated...` |
| 2 | Search Documents → keyword trống | Kết quả: 0 (index đã xóa sạch) |
| 3 | Generate lại data | Data mới được index |

### Test Case 9: Health Check

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Tab "Health" → nhấn "Refresh" | Hiện trạng thái UP/DOWN cho ES, Redis, Kafka |
| 2 | Dừng Redis → Refresh | Redis hiện DOWN |
| 3 | Khởi động lại Redis → Refresh | Redis hiện UP |

### Test Case 10: Error Handling

| Bước | Hành động | Kết quả mong đợi |
|------|-----------|-------------------|
| 1 | Search Documents → size = 999 | Response 400: `size: must be less than or equal to 100` |
| 2 | Dừng Elasticsearch → Search | Response 503: `Elasticsearch Unavailable` |

---

## 5. Test bằng cURL

### Generate sample data

```powershell
# Generate 5 documents + 1 author
curl -X POST http://localhost:9111/api/v1/test/generate/documents?count=5

# Generate 5 authors
curl -X POST http://localhost:9111/api/v1/test/generate/authors?count=5
```

### Search Documents

```powershell
# Search all
curl "http://localhost:9111/api/v1/search/documents"

# Search với keyword
curl "http://localhost:9111/api/v1/search/documents?keyword=Cong+nghe+thong+tin&page=0&size=10"

# Search + filter status
curl "http://localhost:9111/api/v1/search/documents?keyword=Kafka&status=PUBLISHED&sortBy=viewCount&order=desc"

# Search + school/faculty exact filter
curl "http://localhost:9111/api/v1/search/documents?keyword=Cong+nghe+thong+tin&school_id=HCMUS&faculty_id=CNTT"
```

### Search Authors

```powershell
# Search authors
curl "http://localhost:9111/api/v1/search/authors?keyword=john"

# Filter by role
curl "http://localhost:9111/api/v1/search/authors?role=ADMIN&status=ACTIVE"
```

### Suggest

```powershell
curl "http://localhost:9111/api/v1/search/documents/suggest?q=Spring&size=5"
```

### Trending & Popular

```powershell
curl "http://localhost:9111/api/v1/search/documents/trending?size=5"
curl "http://localhost:9111/api/v1/search/authors/popular?size=5"
```

### Push custom event

```powershell
# Push document event
curl -X POST http://localhost:9111/api/v1/test/kafka/document `
  -H "Content-Type: application/json" `
  -d '{\"eventType\":\"DOCUMENT_CREATED\",\"id\":\"doc-test-001\",\"title\":\"Test Document\",\"description\":\"Test desc\",\"status\":\"PUBLISHED\",\"author\":{\"id\":\"author-001\",\"displayName\":\"Test Author\"}}'

# Push author event
curl -X POST http://localhost:9111/api/v1/test/kafka/author `
  -H "Content-Type: application/json" `
  -d '{\"eventType\":\"AUTHOR_CREATED\",\"id\":\"author-test-001\",\"username\":\"test_user\",\"displayName\":\"Test User\",\"email\":\"test@example.com\",\"role\":\"AUTHOR\",\"status\":\"ACTIVE\"}'
```

### Reindex

```powershell
curl -X POST http://localhost:9111/api/v1/admin/reindex/documents
curl -X POST http://localhost:9111/api/v1/admin/reindex/authors
```

### Health

```powershell
curl http://localhost:9111/actuator/health | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

---

## 6. Kiểm tra logs

### Log quan trọng cần kiểm tra

```
# Consumer nhận event
Received document event: type=DOCUMENT_CREATED, id=doc-xxx, topic=document-events, partition=0, offset=42

# Index thành công
Indexed new document: doc-xxx
Indexed new author: author-xxx

# Author propagation
Propagated author update to documents for author: author-xxx
Updated total 5 documents for author author-xxx

# Delete
Deleted document: doc-xxx

# Kafka retry
Kafka retry attempt 1 for topic=document-events, key=doc-xxx, error=...

# Index init
Created index 'documents' with mappings and settings
Index 'authors' already exists
```

---

## 7. Troubleshooting

### Service không khởi động

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| `Connection refused: localhost:9112` | ES chưa chạy | `docker compose up -d` |
| `Connection refused: localhost:7092` | Kafka chưa chạy | Khởi động Kafka broker |
| `Connection refused: localhost:6379` | Redis chưa chạy | Khởi động Redis |
| `JVM version` | JDK < 17 | Cài JDK 17 hoặc 21, set JAVA_HOME |

### Test UI không hiện

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| 404 khi truy cập `/` | Profile không phải `dev` | Thêm `spring.profiles.active=dev` |
| Test API trả 404 | TestController bị disabled | Kiểm tra profile |

### Search trả 0 kết quả

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| Vừa generate xong | Kafka consumer chưa xử lý | Chờ 2-3 giây |
| Sau reindex | Index bị xóa sạch | Generate lại data |
| Filter sai | Status/role không khớp | Kiểm tra giá trị filter |

### Kafka consumer không nhận event

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| SASL auth failed | Username/password sai | Kiểm tra `application.properties` |
| Topic không tồn tại | Chưa tạo topic | Kafka auto-create hoặc tạo manual |
| Deserialize error | Event format sai | Kiểm tra JSON payload |

### Elasticsearch lỗi

```powershell
# Kiểm tra cluster health
curl -u elastic:horob1@2410 http://localhost:9112/_cluster/health?pretty

# Xem indices
curl -u elastic:horob1@2410 http://localhost:9112/_cat/indices?v

# Xem số documents trong index
curl -u elastic:horob1@2410 http://localhost:9112/documents/_count
curl -u elastic:horob1@2410 http://localhost:9112/authors/_count

# Xem mapping
curl -u elastic:horob1@2410 http://localhost:9112/documents/_mapping?pretty
```
