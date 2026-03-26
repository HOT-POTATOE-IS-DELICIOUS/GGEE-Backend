import http from "k6/http";
import { check } from "k6";

// ─── Configuration ────────────────────────────────────────────────────────────

const SCENARIO = __ENV.SCENARIO || "before";
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TARGET_URL = `${BASE_URL}/perf/${SCENARIO}`;

// ─── k6 options ───────────────────────────────────────────────────────────────

export const options = {
  stages: [
    { duration: "20s", target: 20 }, // ramp up
    { duration: "60s", target: 20 }, // hold
    { duration: "10s", target: 0  }, // ramp down
  ],
  thresholds: {
    http_req_duration: ["p(95)<10000"],
    http_req_failed:   ["rate<0.01"],
  },
};

// ─── Payload builder ──────────────────────────────────────────────────────────

function buildComments(startId, count) {
  const comments = [];
  for (let i = 0; i < count; i++) {
    comments.push({
      id: startId + i,
      parent_id: null,
      author: `user-${i}`,
      date: "2026-01-01T00:00:00Z",
      content: `content-${i}`,
      likes: String(i),
      dislikes: "0",
    });
  }
  return comments;
}

// Fixed payload — same comment IDs every request to exercise dedup in "after"
const PAYLOAD = JSON.stringify({
  job_id: "load-test-job",
  timestamp: "2026-01-01T00:00:00Z",
  status: "completed",
  site: "theqoo",
  keyword: "keyword-1",
  total_urls: 2,
  processed_count: 2,
  success_count: 2,
  failed_count: 0,
  results: [
    {
      title: "Post 0",
      view_count: "1",
      recommend_count: "10",
      comment_count: "30",
      date: "2026-01-01T00:00:00Z",
      body: "body",
      url: "https://community.example/keyword-1/post-0",
      comments: buildComments(0, 30),
    },
    {
      title: "Post 1",
      view_count: "1",
      recommend_count: "10",
      comment_count: "30",
      date: "2026-01-01T00:00:00Z",
      body: "body",
      url: "https://community.example/keyword-1/post-1",
      comments: buildComments(10000, 30),
    },
  ],
});

const HEADERS = { "Content-Type": "application/json" };

// ─── Lifecycle ────────────────────────────────────────────────────────────────

export function setup() {
  const res = http.del(`${BASE_URL}/perf/reset`);
  check(res, { "reset 200": (r) => r.status === 200 });
  console.log(`[setup] State cleared. Running scenario: ${SCENARIO}`);
}

export default function () {
  const res = http.post(TARGET_URL, PAYLOAD, { headers: HEADERS });
  check(res, { "status 200": (r) => r.status === 200 });
}

export function teardown() {
  console.log(
    `[teardown] Scenario "${SCENARIO}" complete. ` +
    `Compare p(95) latency and http_req_failed between before/after runs.`
  );
}
