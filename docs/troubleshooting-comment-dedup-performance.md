# 트러블슈팅: 댓글 중복 재처리로 인한 DB 쿼리 급증 문제

> **프로젝트**: 깐깐한 시선들 (AI 기반 콘텐츠 평판 분석 시스템)
> **발생 시점**: 2026년 1월
> **브랜치**: `performance`

---

## 1. 문제 배경

키워드 기반 커뮤니티 반응 분석 시스템에서, 동일 키워드로 재요청 시 이미 수집된 댓글의 대부분은 내용이 변하지 않았음에도 **모든 댓글의 좋아요 수를 매번 재조회·재업데이트**하는 구조였다.

댓글 좋아요 수와 게시글 댓글 목록은 시간이 지나도 지속적으로 변동되므로 주기적인 업데이트 자체는 필수였다. 문제는 **"변경되지 않은 댓글도 변경된 것과 동일한 쿼리 비용을 지불"** 한다는 점이었다.

---

## 2. 개선 전 구조와 문제점

```
[크롤러] → Kafka → [Consumer]
                        │
                 댓글 N개 수신
                        │
               for each comment:
                 ① SELECT  — 이미 저장된 댓글인지 확인      (N 번)
                 ② INSERT 또는 UPDATE                       (N 번)
```

| 상황 | DB 쿼리 수 |
|------|-----------|
| 최초 크롤링 (댓글 N개) | SELECT × N + INSERT × N = **2N** |
| 재크롤링 (동일 댓글 N개) | SELECT × N + UPDATE × N = **2N** |
| K회 재크롤링 | **2N × K** |

---

## 3. 개선 후 구조

### 핵심 변경 2가지

#### ① Kafka Streams 기반 슬라이딩 TTL 중복 필터링

```
[크롤러] → crawl.result (Kafka)
                │
    ┌───────────▼──────────────────────────┐
    │  CommentDedupStream (always-on)      │
    │  RocksDB state store                 │
    │  key: commentId|postUrl              │
    │  TTL: 24시간 (슬라이딩)               │
    │                                      │
    │  TTL 이내 재등장 → 필터링             │
    │  TTL 만료 후 재등장 → 통과            │
    └───────────▼──────────────────────────┘
                │
    crawl.comment.deduped
                │
    ┌───────────▼──────────┐
    │  INSERT ON CONFLICT  │  ← SELECT 없음
    └──────────────────────┘
```

#### ② SELECT 제거 + UPSERT

```sql
-- 개선 전: 2 round-trips
SELECT 1 FROM comments WHERE comment_key = ?;
INSERT INTO comments ... / UPDATE comments SET ...;

-- 개선 후: 1 round-trip
INSERT INTO comments (...) VALUES (...)
ON CONFLICT (comment_key) DO UPDATE
    SET likes = EXCLUDED.likes, ...;
```

---

## 4. 실측 성능 비교

### 4-1. PostgreSQL 직접 쿼리 비교

> `CommentProcessingBeforeAfterTest` — PostgreSQL 17 (Docker)
> 조건: 키워드 10개, 게시글 5개/키워드, 댓글 40개/게시글, 재크롤 8회 (24h TTL 이내)

| 항목 | 개선 전 | 개선 후 |
|------|---------|---------|
| 총 DB 쿼리 | 32,000건 | 2,000건 |
| DB 실행 시간 | 9,373 ms | 679 ms |
| **DB 쿼리 감소** | — | **93.8%** |
| **실행 시간 단축** | — | **13.8배** |

---

### 4-2. k6 부하 테스트 (프로덕션 환경 동일 구성)

> **환경**: Spring Boot WebFlux + PostgreSQL 17 + Apache Kafka (KRaft) — 모두 Docker
> **도구**: k6 v1.5.0
> **부하**: 20 VUs, ramp-up 20s → hold 60s → ramp-down 10s (총 90s)
> **페이로드**: 게시글 2개 × 댓글 30개 = 60 comments / 요청
> **before**: 매 요청마다 60 comments 전부 SELECT + INSERT/UPDATE
> **after**: TTL 내 동일 댓글 → 필터링 (0 DB ops), 최초 1회만 UPSERT

#### before 결과

```
iterations........: 3,302    (36.4 req/s)

http_req_duration.: avg=458ms  med=328ms  p(90)=891ms  p(95)=1,270ms  max=4,150ms
http_req_failed...: 0.00%
```

#### after 결과

```
iterations........: 158,722   (1,762 req/s)

http_req_duration.: avg=9ms    med=4ms    p(90)=17ms   p(95)=31ms     max=646ms
http_req_failed...: 0.00%
```

#### 비교 요약

| 지표 | before | after | 개선 |
|------|--------|-------|------|
| 처리량 (req/s) | 36.4 | **1,762** | **+48배** |
| 평균 응답시간 | 458 ms | 9 ms | **-98%** |
| p(95) 응답시간 | 1,270 ms | 31 ms | **-97.6%** |
| 최대 응답시간 | 4,150 ms | 646 ms | **-84.4%** |
| 에러율 | 0% | 0% | — |

---

## 5. 결과 해석

**before** 는 매 요청마다 PostgreSQL에 60번의 SELECT + 60번의 INSERT/UPDATE = **120 round-trips**를 순차적으로 수행한다. DB 병목이 응답 시간을 지배한다.

**after** 는 동일한 댓글 ID가 TTL 이내 반복 요청되면 in-memory 상태 저장소에서 필터링 후 **DB에 전혀 접근하지 않는다**. 결과적으로 응답이 메모리 연산 수준으로 빨라진다.

처리량이 48배 차이나는 이유가 바로 여기 있다. DB 쿼리를 없앤 것 자체가 병목 제거다.

---

## 6. TTL 만료 후 동작 검증

슬라이딩 TTL 방식으로, 24시간 이후에는 재크롤 시 다시 DB에 쓴다.

```
T=0h:   댓글 1001 최초 크롤링 → DB INSERT ✅
T=6h:   댓글 1001 재크롤링    → 필터링 🚫 (TTL 이내)
T=25h:  댓글 1001 재크롤링    → DB UPSERT ✅ (TTL 만료, 좋아요 갱신)
```

```
suppressesDuplicateCommentsWithinOneDayTtl  ✅ PASSED
reEmitsCommentAfterTtlExpires               ✅ PASSED
```

---

## 7. 확장성

단일 Kafka Streams 토폴로지 안에서 중복 제거 + 집계를 처리하므로, 이후 기능 추가 시 스트림 단계만 붙이면 된다.

```
crawl.result
    │
    ▼ flatMapValues (댓글 개별화)
    │
    ▼ CommentDeduplicationProcessor (TTL 중복 제거)
    │
    ├──▶ crawl.comment.deduped → DB UPSERT
    │
    └── (추후 추가, DB 부하 없음)
         ├── 인기 댓글 분석
         └── 실시간 반응 알림
```
