# 📋 Search Service — API Reference (Quick)

> Base URL: `http://localhost:9111`
> Cập nhật: 22/03/2026

---

## Search APIs

| Method | Endpoint | Params | Mô tả |
|--------|----------|--------|--------|
| GET | `/api/v1/search/documents` | `keyword`, `page`, `size`, `sortBy`, `order`, `status`, `authorId`, `school_id`, `faculty_id` | Search documents theo full-text + filter |
| GET | `/api/v1/search/authors` | `keyword`, `page`, `size`, `role`, `status` | Search authors |
| GET | `/api/v1/search/documents/suggest` | `q` (required), `size` | Autocomplete gợi ý theo title |
| GET | `/api/v1/search/documents/trending` | `size` | Top documents theo views |
| GET | `/api/v1/search/authors/popular` | `size` | Top authors theo followers |

## Document Search Logic (v1.1)

- `keyword` sẽ search trên 4 field: `title`, `content`, `school_name`, `faculty_name`.
- Boost relevance:
  - `title^6` (cao nhất)
  - `school_name^3`, `faculty_name^3` (trung bình)
  - `content^1` (thấp hơn)
- Query DSL: `bool.must.multi_match` + `bool.filter.term`.
- Optional filters:
  - `school_id` -> `term` filter chính xác
  - `faculty_id` -> `term` filter chính xác
- Highlight fields: `title`, `content`, `school_name`, `faculty_name`.

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
| POST | `/api/v1/test/generate/documents?count=5` | Generate sample documents (đã có `school/faculty`) |
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
| `document-events` | `DOCUMENT_CREATED`, `DOCUMENT_UPDATED`, `DOCUMENT_DELETED` | Document Service |
| `author-events` | `AUTHOR_CREATED`, `AUTHOR_UPDATED`, `AUTHOR_DELETED` | User Service |
| `document-events.DLT` | Failed messages (auto) | Search Service |
| `author-events.DLT` | Failed messages (auto) | Search Service |

## Test UI

```
http://localhost:9111       (chỉ khi profile = dev)
```
