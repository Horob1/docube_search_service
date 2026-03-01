# 📚 Search Service — Documentation

> Thư mục này chứa tất cả tài liệu của Search Service.

---

## 📁 Danh sách tài liệu

| # | File | Mô tả | Đối tượng |
|---|------|--------|-----------|
| 1 | [01-integration-guide.md](./01-integration-guide.md) | **Hướng dẫn tích hợp** — APIs, Kafka events, payload format, checklist | Backend dev tích hợp |
| 2 | [02-testing-guide.md](./02-testing-guide.md) | **Hướng dẫn kiểm thử** — Test UI, kịch bản test, cURL, troubleshooting | QA / Dev |
| 3 | [03-service-detail.md](./03-service-detail.md) | **Tài liệu chi tiết** — Kiến trúc, ES index, Kafka design, cache, config | Dev / DevOps |
| 4 | [04-api-reference.md](./04-api-reference.md) | **API Reference nhanh** — Bảng tổng hợp tất cả endpoints | Tất cả |
| 5 | [05-api-detail.md](./05-api-detail.md) | **API Reference chi tiết** — Tất cả APIs, đầy đủ params, response fields, ví dụ request/response | Frontend / Backend dev |
| 6 | [06-kafka-events-detail.md](./06-kafka-events-detail.md) | **Kafka Events chi tiết** — Đầy đủ topics, models, từng field, ví dụ code producer, best practices | Backend dev tích hợp |
| 7 | [07-docker-deployment.md](./07-docker-deployment.md) | **Docker Deployment** — Dockerfile, docker-compose dev/prod, networking, env vars, troubleshooting | DevOps / Dev |

---

## 🚀 Quick Start

```powershell
# 1. Khởi động infrastructure
docker network create horob1_docub
docker compose up -d

# 2. Build & run service
.\gradlew.bat clean build -x test
.\gradlew.bat bootRun

# 3. Mở test dashboard
# http://localhost:9111

# 4. Generate sample data → Search → Done!
```

---

## 📞 Liên hệ

- **Service name:** SS_DEV1
- **Port:** 9111
- **Eureka:** http://localhost:9000
- **Health:** http://localhost:9111/actuator/health

