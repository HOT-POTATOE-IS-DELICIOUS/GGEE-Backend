다음 기능을 만들어야해 해당 명세는 AI서버의 명세로서 여기서 작업할 내용이 아니고 저 서버랑 연동을
진행해서 프론트에게 전달해야하는 내용이야

기능 설명:

사용자의 단일 메시지를 받아 Gemini 의도 분류 → 도구 라우팅 → Markdown 응답까지 한 번에 처리하는 대화형 전략 API입니다. 응답은 SSE로 토큰 단위 실시간 스트리밍됩니다.

요청은 자동으로 세 가지 의도 중 하나로 분류되며, 의도에 맞는 도구가 실행됩니다.

graph_query: Neo4j 지식 그래프에서 과거 이슈를 벡터·그래프 확장 검색한 뒤, 반환

draft_statement: 입장문/사과문/보도자료 초안을 생성한 뒤, 6가지 시선으로 자체 검수하여 최종본을 반환

chat: 여론에 대한 대응 전문가 페르소나의 일반 대화 (요청을 다듬거나 후속 질문 유도)

서버는 stateless 입니다. 멀티턴 히스토리를 저장하지 않으며, 이전 컨텍스트가 필요하면 클라이언트가 직접 합쳐 하나의 message 로 전달해야 합니다.

스크린샷 2026-04-19 오후 9.25.59.png
전략 모드 기능에 대한 UI입니다.
API 정보
Endpoint & Request


POST /strategy/stream
Content-Type: application/json
{
"message": "가맹점 갑질 의혹에 대한 사과 입장문 초안 써줘",
"entity_name": "백종원",
"entity_info": "더본코리아, 요식업계"
}
Request Body
파라미터

타입

필수

설명

message

string

O

사용자의 단일 요청 문자열

entity_name

string

O

전략을 수립 중인 대상 엔티티 이름 (인물/기관/기업)

entity_info

string

O

동명이인 구분 및 도구별 검색·생성 정확도 향상을 위한 부가 정보 (쉼표 구분)

Response (SSE)
Response Headers


Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
X-Accel-Buffering: no
이벤트 프레임 포멧
각 이벤트는 다음 프레임으로 전송됩니다. 빈 줄(\n\n)이 프레임 구분자이며, JSON 페이로드는 UTF-8 그대로(ensure_ascii=false) 직렬화됩니다.



event: <event_type>
data: <json payload>
이벤트 타입
event_type

payload 필드

발생 시점 / 보장

intent_classified

intent, refined_query

스트림 시작 직후 1회

tool_started

tool, display

도구 실행 / 단계 전환 시
(UI 상태 표시용) (draft_statement는 2회 발생)

content_chunk

delta

Markdown 토큰 단위

meta

data (object)

도구별 부가 메타데이터, 도구 종료 직전 1회
(단, 사례에 따라 위치 변동)

error

message, code

의도 분류·도구 실행 중 미처리 예외 발생 시,
HTTP 5xx가 아니라 스트림 내 이벤트로 전달

done

message_id

스트림 종료 직전 1회
(마지막 finally 보장) (UUID v4 hex 32자, 하이픈 없음)

tool_started display 문구
tool

display

의미

kg_search

지식 그래프에서 검색

Neo4j 벡터·그래프 검색 진행

statement_draft

입장문 초안 생성 중

초안 작성 시작 (1회차)

statement_draft

6개 시선 자체 검수 중

초안을 6개의 시선으로 검수 (2회차)

chat

답변 생성 중

일반 대회 시작

이벤트 발생 순서 보장


intent_classified
→ tool_started
→ (content_chunk × N)
→ [draft_statement 의도일 경우: tool_started (검수) → (content_chunk × N) → meta]
→ [그 외: meta]
→ [(예외 발생 시) error]
→ done    ← 항상 마지막
SSE 응답 예시 - 입장문 작성
Request


curl -N -X POST http://192.168.75.240:8002/strategy/stream \
-H "Content-Type: application/json" \
-d '{"message":"가맹점 갑질 의혹에 대한 사과 입장문 초안 써줘","entity_name":"백종원","entity_info":"더본코리아"}'
Response


event: intent_classified
data: {"intent":"draft_statement","refined_query":"가맹점 갑질 의혹 사과 입장문"}
event: tool_started
data: {"tool":"statement_draft","display":"입장문 초안 생성 중"}
event: content_chunk
data: {"delta":"# 가맹점 갑질 의혹에 대한 사과 입장문\n\n..."}
event: tool_started
data: {"tool":"statement_draft","display":"6개 시선 자체 검수 중"}
event: content_chunk
data: {"delta":"\n\n---\n\n## 6개 시선 검수 반영 최종본\n\n..."}
event: meta
data: {"audited":true,"applied_perspectives":["성별 편향","커뮤니티 성향"],"revision_count":2}
event: done
data: {"message_id":"b9c01f3e..."}
draft_statement 의도일 때 클라이언트가 content_chunk 들을 순서대로 이어붙이면 [초안 본문] + \n\n---\n\n## 6개 시선 검수 반영 최종본\n\n + [최종본] 형태의 Markdown이 됩니다. 초안에 적용할 수정 제안이 0개여도 최종본 섹션은 그대로 추가됩니다.

Intent별 meta 페이로드 상세
meta 이벤트의 data 객체는 도구별로 키 집합이 다릅니다. 각 도구마다 모든 가능한 필드와 시나리오별 조합입니다.

graph_query 필드 정의
필드

타입

Nullable

설명

issue_ids

string[]

X

Neo4j 이슈 노드 ID 목록 (최대 5건)

issue_titles

string[]

X

issue_ids 와 같은 순서의 이슈 제목 목록

status

string

O

비정상 경로 식별자 (정상 매칭 시 키 자체가 없음. 값: no_match | neo4j_unavailable | search_error)

draft_statement 필드 정의
필드

타입

Nullable

설명

필드

타입

Nullable

설명

audited

boolean

X

6개의 자체 시선 검수 단계가 정상 완료됐는지 여부
(초안 생성 실패·검수 예외 시 false)

applied_perspectives

string[]

O

최종본에 실제로 반영된 시선 한글 라벨 목록
(audited: true 일 때만 존재)

revision_count

integer

O

최종본에 in-place 치환된 수정 제안 개수
(audited: true 일 때만 존재)

status

string

O

비정상 경로 식별자
(audited: false일 때만 존재)
(값: empty_draft | audit_failed)

chat 필드 정의
필드

타입

Nullable

설명

status

string

X

응답 생성 결과. ok 외엔 직전 content_chunk에 폴백 안내 문구가 들어감
