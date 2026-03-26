# 성능 테스트 재현 가이드

`troubleshooting-comment-dedup-performance.md`의 측정 결과를 직접 재현하는 방법입니다.

## 사전 요구사항

| 도구 | 버전 | 확인 명령어 |
|------|------|-------------|
| Docker Desktop | 최신 | `docker info` |
| Java | 21 | `java -version` |
| k6 | 1.x | `k6 version` |

k6 설치 (macOS):
```bash
brew install k6
```

---

## 1단계: 인프라 기동

PostgreSQL + Kafka를 Docker로 띄웁니다.

```bash
docker compose up postgres kafka -d --wait
```

정상 기동 확인:
```
NAME            STATUS
ggee-postgres   Up (healthy)
ggee-kafka      Up (healthy)
```

### perf_comments 테이블 생성

init SQL은 볼륨이 처음 생성될 때만 실행됩니다.
컨테이너가 이미 존재했다면 수동으로 생성합니다.

```bash
docker exec ggee-postgres psql -U ggee-user -d ggee -c "
CREATE TABLE IF NOT EXISTS perf_comments (
  comment_key  VARCHAR(512) PRIMARY KEY,
  site         VARCHAR(64)  NOT NULL,
  keyword      VARCHAR(128) NOT NULL,
  post_url     VARCHAR(512) NOT NULL,
  comment_id   INT          NOT NULL,
  author       VARCHAR(128),
  content      TEXT,
  likes        VARCHAR(16),
  dislikes     VARCHAR(16),
  crawled_at   VARCHAR(64)
);"
```

---

## 2단계: Spring Boot 앱 기동

```bash
./gradlew bootRun > /tmp/ggee-app.log 2>&1 &
```

앱이 뜰 때까지 기다립니다 (약 15~20초):

```bash
# UP 이 나올 때까지 반복
curl -s http://localhost:8080/actuator/health
# {"status":"UP"} 확인
```

---

## 3단계: k6 부하 테스트 실행

### before 시나리오 (개선 전 — SELECT + INSERT/UPDATE)

```bash
k6 run -e SCENARIO=before k6/perf-test.js
```

완료까지 약 **90초** 소요.
끝나면 아래 항목을 기록해두세요:

```
http_req_duration: avg / p(95)
http_reqs:         x.xx/s
```

### after 시나리오 (개선 후 — 중복 필터링 + UPSERT)

```bash
k6 run -e SCENARIO=after k6/perf-test.js
```

---

## 4단계: 결과 읽는 법

k6 출력에서 주요 지표:

```
http_req_duration: avg=???ms  p(95)=???ms   ← 응답 시간
http_reqs........: ???/s                     ← 초당 처리 요청 수
http_req_failed..: 0.00%                     ← 에러율
iterations.......: ???                       ← 총 처리 건수
```

**before**와 **after**의 `http_reqs`(처리량)와 `p(95)`(응답시간)를 비교하면 됩니다.

---

## 5단계: 정리

```bash
# 앱 종료
pkill -f bootRun

# 인프라 종료 (데이터 유지)
docker compose down

# 인프라 + 데이터 완전 삭제
docker compose down -v
```

---

## 테스트 조건 요약

| 항목 | 값 |
|------|----|
| Spring Boot | WebFlux (Reactive) |
| DB | PostgreSQL 17 (Docker) |
| Kafka | Apache Kafka KRaft (Docker) |
| k6 VUs | 20 |
| 부하 패턴 | ramp-up 20s → hold 60s → ramp-down 10s |
| 요청당 댓글 수 | 60개 (게시글 2개 × 댓글 30개) |
| comment ID | 고정 (동일 ID 반복 → dedup 테스트) |
| TTL | 24시간 |

같은 60개 comment ID를 계속 반복 요청하기 때문에:
- **before**: 매 요청마다 60 SELECT + 60 INSERT/UPDATE = 120 DB round-trips
- **after**: 최초 1회 이후 모두 in-memory 필터링 → DB 접근 0

---

## 주의사항

- 결과는 로컬 머신 성능에 따라 달라집니다. 처리량 절대값보다 **before/after 배수**를 보세요.
- after는 TTL이 24시간이라 테스트 재실행 시 `DELETE /perf/reset`이 자동 호출되지만, 앱을 재시작해야 dedup 상태가 완전히 초기화됩니다.
- before 테스트 후 after 테스트 전에 앱을 재시작하지 않아도 되지만, DB에 데이터가 누적된 상태이므로 before는 대부분 UPDATE, after는 dedup으로 전부 필터링됩니다 — 의도된 동작입니다.
