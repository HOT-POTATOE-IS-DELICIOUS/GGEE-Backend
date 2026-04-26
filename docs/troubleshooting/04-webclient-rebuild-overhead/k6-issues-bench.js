/**
 * k6 부하 테스트: GET /issues (WebClient 재빌드 오버헤드 before/after 비교)
 *
 * 실행 방법:
 *   TOKEN=<JWT_TOKEN> k6 run -e TOKEN=$TOKEN k6-issues-bench.js
 *
 * 또는 스크립트 내 TOKEN 변수에 직접 입력 후 실행:
 *   k6 run k6-issues-bench.js
 *
 * 사전 조건:
 *   - Docker Compose 실행: docker compose up -d postgres kafka
 *   - Kafka 토픽 생성: docker exec ggee-kafka rpk topic create crawl.community.result ...
 *   - 앱 실행: env $(cat .env.local | grep -v '^#' | grep -v '^$') java -jar build/libs/*.jar
 *   - 테스트 유저 등록 후 access_token 발급
 */
import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const issuesDuration = new Trend('issues_duration', true);
const errorRate = new Rate('error_rate');

export const options = {
  stages: [
    { duration: '5s', target: 20 },
    { duration: '20s', target: 100 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    'error_rate': ['rate<0.1'],
  },
};

// 환경변수 또는 직접 입력
const TOKEN = __ENV.TOKEN || '<YOUR_ACCESS_TOKEN>';

export default function () {
  const start = Date.now();
  const res = http.get('http://localhost:8080/issues', {
    headers: { 'Authorization': 'Bearer ' + TOKEN },
  });
  issuesDuration.add(Date.now() - start);

  const ok = check(res, { 'status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}
