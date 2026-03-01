# 📋 Search Service — API Reference (Quick)

> Base URL: `http://localhost:9111`

---

## Search APIs

| Method | Endpoint | Params | Mô tả |
|--------|----------|--------|--------|
| GET | `/api/v1/search/documents` | keyword, page, size, sortBy, order, status, authorId | Search documents |
| GET | `/api/v1/search/authors` | keyword, page, size, role, status | Search authors |
| GET | `/api/v1/search/documents/suggest` | q (required), size | Autocomplete gợi ý |
| GET | `/api/v1/search/documents/trending` | size | Top documents theo views |
| GET | `/api/v1/search/authors/popular` | size | Top authors theo followers |

## Admin APIs

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/admin/reindex/documents` | Xóa + tạo lại index documents |
| POST | `/api/v1/admin/reindex/authors` | Xóa + tạo lại index authors |

## Test APIs (dev only)

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/test/kafka/document` | Push document event vào Kafka |
| POST | `/api/v1/test/kafka/author` | Push author event vào Kafka |
| POST | `/api/v1/test/generate/documents?count=5` | Generate sample documents |
| POST | `/api/v1/test/generate/authors?count=5` | Generate sample authors |

## Monitoring

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Service info |
| GET | `/actuator/metrics` | Metrics |
| GET | `/actuator/prometheus` | Prometheus metrics |

## Kafka Topics

| Topic | Events | Producer |
|-------|--------|----------|
| `document-events` | DOCUMENT_CREATED, DOCUMENT_UPDATED, DOCUMENT_DELETED | Document Service |
| `author-events` | AUTHOR_CREATED, AUTHOR_UPDATED, AUTHOR_DELETED | User Service |
| `document-events.DLT` | Failed messages (auto) | Search Service |
| `author-events.DLT` | Failed messages (auto) | Search Service |

## Test UI

```
http://localhost:9111       (chỉ khi profile = dev)
```

