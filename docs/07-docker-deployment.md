# 🐳 Search Service — Docker Deployment Guide

> **Cập nhật:** 28/02/2026

---

## 📑 Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Cấu trúc Docker files](#2-cấu-trúc-docker-files)
3. [Chạy DEV](#3-chạy-dev)
4. [Chạy PRODUCTION](#4-chạy-production)
5. [So sánh DEV vs PROD](#5-so-sánh-dev-vs-prod)
6. [Environment Variables](#6-environment-variables)
7. [Networking](#7-networking)
8. [Lệnh quản lý](#8-lệnh-quản-lý)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                   Docker Network: horob1_docub               │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   es01   │  │   es02   │  │   es03   │  │   search-   │ │
│  │ :9200    │  │ :9200    │  │ :9200    │  │   service   │ │
│  │(internal)│  │(internal)│  │(internal)│  │  :9111      │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────┬──────┘ │
│                                                    │        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │        │
│  │  redis   │  │  kafka   │  │  eureka  │◄────────┘        │
│  │  :6379   │  │  :7092   │  │  :9000   │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘
                         │
              Host ports │ exposed
                         ▼
              :9111 (Search API)
              :9112 (ES — dev only)
              :5601 (Kibana — dev only)
```

---

## 2. Cấu trúc Docker files

```
docube_search_search/
├── Dockerfile                  # Multi-stage build (gradle → JRE alpine)
├── .dockerignore               # Ignore build artifacts, docs, IDE files
├── .env.example                # Environment variables mẫu
├── docker-compose.dev.yml      # DEV: ES single-node + Kibana + App (profile=dev)
├── docker-compose.prod.yml     # PROD: ES 3-node cluster + App (profile=prod)
└── src/main/resources/
    ├── application.properties         # Base config (profile=dev mặc định)
    └── application-prod.properties    # Production overrides
```

---

## 3. Chạy DEV

### Lần đầu — tạo network

```powershell
docker network create horob1_docub
```

### Build & Start

```powershell
docker compose -f docker-compose.dev.yml up -d --build
```

### Kiểm tra

```powershell
# Xem container status
docker compose -f docker-compose.dev.yml ps

# Xem logs search service
docker compose -f docker-compose.dev.yml logs -f search-service

# Health check
curl http://localhost:9111/actuator/health
```

### Truy cập

| Service | URL | Mô tả |
|---------|-----|--------|
| **Search Service** | http://localhost:9111 | Test UI Dashboard |
| **Search API** | http://localhost:9111/api/v1/search/documents | REST API |
| **Test API** | http://localhost:9111/api/v1/test/* | ✅ Push fake events |
| **Elasticsearch** | http://localhost:9112 | Exposed ra host |
| **Kibana** | http://localhost:5601 | Dev query tool |
| **Health** | http://localhost:9111/actuator/health | Health check |
| **Metrics** | http://localhost:9111/actuator/prometheus | Prometheus metrics |

### Dừng

```powershell
docker compose -f docker-compose.dev.yml down        # giữ data
docker compose -f docker-compose.dev.yml down -v      # xóa luôn data
```

---

## 4. Chạy PRODUCTION

### Chuẩn bị

```powershell
# 1. Tạo network (nếu chưa có)
docker network create horob1_docub

# 2. Copy và sửa environment file
copy .env.example .env
# Sửa .env: đổi passwords, hostnames phù hợp production
```

### Build & Start

```powershell
docker compose -f docker-compose.prod.yml up -d --build
```

### Kiểm tra

```powershell
# Container status
docker compose -f docker-compose.prod.yml ps

# Logs
docker compose -f docker-compose.prod.yml logs -f search-service

# ES cluster health (từ bên trong network)
docker exec ss-es01-prod curl -s -u elastic:horob1@2410 http://localhost:9200/_cluster/health?pretty
```

### Truy cập

| Service | URL | Mô tả |
|---------|-----|--------|
| **Search API** | http://localhost:9111/api/v1/search/documents | REST API |
| **Health** | http://localhost:9111/actuator/health | Health check |
| **Metrics** | http://localhost:9111/actuator/prometheus | Prometheus metrics |
| **Elasticsearch** | ❌ Không expose | Chỉ internal network |
| **Kibana** | ❌ Không có | Không deploy ở prod |
| **Test UI** | ❌ Disabled | Profile=prod |
| **Test API** | ❌ 404 | TestController disabled |

### Dừng

```powershell
docker compose -f docker-compose.prod.yml down        # giữ data
docker compose -f docker-compose.prod.yml down -v      # xóa data (⚠️ CẢNH BÁO)
```

---

## 5. So sánh DEV vs PROD

| Tính năng | DEV | PROD |
|-----------|-----|------|
| **Spring Profile** | `dev` | `prod` |
| **TestController / Test UI** | ✅ Có | ❌ Disabled |
| **ES Topology** | Single-node | 3-node cluster |
| **ES Exposed port** | `:9112` | ❌ Internal only |
| **Kibana** | ✅ `:5601` | ❌ Không có |
| **ES Memory** | 512MB | 1GB per node |
| **App Memory** | 256-512MB | 512MB-1GB |
| **Logging level** | DEBUG | WARN/INFO |
| **Actuator details** | `show-details=always` | `show-details=when_authorized` |
| **Actuator endpoints** | health, info, metrics, prometheus | health, prometheus |
| **Restart policy** | `on-failure` | `unless-stopped` |
| **Resource limits** | Không | Có (memory limits) |
| **ES Replicas** | 0 | Managed by cluster |

---

## 6. Environment Variables

Tất cả có thể override qua file `.env` hoặc docker-compose `environment`:

| Variable | Default (dev) | Default (prod) | Mô tả |
|----------|--------------|----------------|--------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Spring profile |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | `-Xms512m -Xmx1g` | JVM options |
| `SPRING_ELASTICSEARCH_URIS` | `http://es01:9200` | `http://es01:9200` | ES URI (internal) |
| `SPRING_ELASTICSEARCH_USERNAME` | `elastic` | `elastic` | ES username |
| `SPRING_ELASTICSEARCH_PASSWORD` | `horob1@2410` | `horob1@2410` | ES password |
| `SPRING_DATA_REDIS_HOST` | `redis` | `redis` | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | `6379` | `6379` | Redis port |
| `SPRING_DATA_REDIS_PASSWORD` | `2410` | `2410` | Redis password |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:7092` | `kafka:7092` | Kafka brokers |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka-server:9000/eureka` | `http://eureka-server:9000/eureka` | Eureka URL |
| `ES_PASSWORD` | `horob1@2410` | `horob1@2410` | ES container password |
| `ES_JAVA_OPTS` | `-Xms512m -Xmx512m` | `-Xms1g -Xmx1g` | ES JVM |

---

## 7. Networking

### External Network

Search Service sử dụng **external Docker network** `horob1_docub`. Tất cả microservices phải join cùng network này:

```yaml
networks:
  horob1_docub:
    external: true
```

### Service Discovery trong Docker

Trong Docker Compose, các container có thể giao tiếp qua **container name** hoặc **service name**:

| Service | Hostname (trong Docker network) | Port (internal) |
|---------|-------------------------------|-----------------|
| Elasticsearch node 1 | `es01` | 9200 |
| Elasticsearch node 2 | `es02` | 9200 |
| Elasticsearch node 3 | `es03` | 9200 |
| Search Service | `search-service` hoặc `ss-search-dev` / `ss-search-prod` | 9111 |
| Redis | `redis` | 6379 |
| Kafka | `kafka` | 7092 |
| Eureka | `eureka-server` | 9000 |

### Từ service khác gọi Search Service

```
# Trong cùng Docker network horob1_docub
http://search-service:9111/api/v1/search/documents

# Hoặc dùng container name
http://ss-search-dev:9111/api/v1/search/documents    (dev)
http://ss-search-prod:9111/api/v1/search/documents   (prod)
```

### Eureka Registration

Search Service tự đăng ký với Eureka qua `eureka.client.service-url.defaultZone`. Các service khác có thể discover qua tên `SS_DEV1`:

```
# Eureka REST
http://eureka-server:9000/eureka/apps/SS_DEV1
```

---

## 8. Lệnh quản lý

### Build lại image (sau khi sửa code)

```powershell
# Dev
docker compose -f docker-compose.dev.yml build search-service
docker compose -f docker-compose.dev.yml up -d search-service

# Prod
docker compose -f docker-compose.prod.yml build search-service
docker compose -f docker-compose.prod.yml up -d search-service
```

### Xem logs

```powershell
# Tất cả
docker compose -f docker-compose.dev.yml logs -f

# Chỉ search service
docker compose -f docker-compose.dev.yml logs -f search-service

# Chỉ ES
docker compose -f docker-compose.dev.yml logs -f es01
```

### Restart search service

```powershell
docker compose -f docker-compose.dev.yml restart search-service
```

### Vào shell container

```powershell
docker exec -it ss-search-dev sh
docker exec -it ss-es01-dev bash
```

### Kiểm tra ES cluster health

```powershell
# Dev (từ host)
curl -u elastic:horob1@2410 http://localhost:9112/_cluster/health?pretty

# Prod (từ bên trong container — ES không expose ra host)
docker exec ss-es01-prod curl -s -u elastic:horob1@2410 http://localhost:9200/_cluster/health?pretty
```

---

## 9. Troubleshooting

### Search Service không start

```powershell
# Xem logs
docker compose -f docker-compose.dev.yml logs search-service

# Kiểm tra ES đã healthy chưa
docker compose -f docker-compose.dev.yml ps
```

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| `Connection refused: es01:9200` | ES chưa ready | Chờ ES healthcheck pass (30-60s) |
| `Network horob1_docub not found` | Chưa tạo network | `docker network create horob1_docub` |
| `Connection refused: kafka:7092` | Kafka không cùng network | Đảm bảo Kafka join `horob1_docub` |
| `Connection refused: redis:6379` | Redis không cùng network | Đảm bảo Redis join `horob1_docub` |
| `OutOfMemoryError` | JVM memory thiếu | Tăng `JAVA_OPTS` memory |

### ES cluster RED status

```powershell
# Xem shard allocation
docker exec ss-es01-prod curl -s -u elastic:horob1@2410 http://localhost:9200/_cluster/allocation/explain?pretty

# Xem unassigned shards
docker exec ss-es01-prod curl -s -u elastic:horob1@2410 http://localhost:9200/_cat/shards?v&h=index,shard,prirep,state,unassigned.reason
```

### Rebuild từ đầu

```powershell
# Xóa tất cả containers + volumes + images
docker compose -f docker-compose.dev.yml down -v --rmi local

# Build lại từ đầu
docker compose -f docker-compose.dev.yml up -d --build
```

