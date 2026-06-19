# AMHS Backend

Spring Boot 기반 AMHS 반송 Job 경로 탐색 및 상태 관리 시스템입니다.  
반도체 FAB 내부의 반송 흐름을 단순화해서 Node, Edge, Equipment, Transfer Job, Alert, Dashboard를 백엔드 중심으로 모델링했습니다.

## 프로젝트 목적

이 프로젝트는 자동 반송 환경에서 중요한 상태 관리, 경로 탐색, 장비 할당, 장애 감지 흐름을 서버 관점에서 구현하는 것을 목표로 합니다.

- 반송 대상 이동 Job 생성
- Node / Edge 상태 기반 경로 탐색
- 장애 구간 우회 처리
- 장비 자동 할당
- Job 상태 변경 이력 관리
- 실패 Job 재처리
- 지연 / 장애 알림 감지
- 운영 현황 요약 조회

## 도메인 설명

### AMHS Node

FAB 내부의 이동 지점입니다.

- 예: `STOCKER_01`, `NODE_A`, `EQP_01`
- 상태: `AVAILABLE`, `BLOCKED`
- 타입: `STOCKER`, `OHT_NODE`, `EQP`, `PORT`

### AMHS Edge

Node와 Node 사이의 이동 경로입니다.

- 방향성을 가진 간선
- 상태: `AVAILABLE`, `BLOCKED`
- 거리와 예상 이동 시간 보유
- 혼잡도 `congestionLevel (0~100)` 보유

### Equipment

반송 작업과 연관된 장비입니다.

- 예: `OHT_001`, `CONVEYOR_001`
- 상태: `IDLE`, `MOVING`, `ERROR`, `MAINTENANCE`

### Transfer Job

Carrier를 출발 Node에서 목적 Node로 이동시키는 반송 작업입니다.

- 상태: `CREATED`, `ASSIGNED`, `MOVING`, `COMPLETED`, `FAILED`, `CANCELED`
- 우선순위: `NORMAL`, `HIGH`, `URGENT`
- 생성 시 Dijkstra 경로 탐색 수행
- 상태 변경 시 이력 저장
- 상태 전이 규칙 적용

### Transfer Job History

Transfer Job의 상태 변경 이력을 저장합니다.

- 상태
- 사유
- 할당 장비 코드
- 경로 스냅샷

### Alert

운영 중 발생하는 지연 / 장애 상황을 알림으로 관리합니다.

- 타입: `DELAYED_JOB`, `STUCK_JOB`, `EQUIPMENT_ERROR`, `ROUTE_BLOCKED`
- 상태: `OPEN`, `RESOLVED`

## 주요 기능

- Node 등록, 조회, 상태 변경
- Edge 등록, 조회, 상태 변경, 혼잡도 변경
- Equipment 등록, 조회, 상태 변경
- BFS 경로 탐색
- Dijkstra 최단 예상 시간 경로 탐색
- `CONGESTION_AWARE` 전략 기반 경로 탐색
- `BLOCKED` Node / Edge 제외 후 우회 경로 계산
- Transfer Job 생성 시 경로 저장
- Transfer Job 상태 변경 및 이력 저장
- Transfer Job 상태 전이 규칙 검증
- 실패 Job retry
- 장비 자동 할당 및 대기 Job 일괄 할당
- 배정 후보 Queue 조회
- 지연 / 장애 Alert 감지 Scheduler
- Dashboard 요약 통계 조회
- 로컬 실행 시 샘플 데이터 자동 생성

## 기술 스택

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Validation
- H2 Database
- MariaDB profile 분리
- Swagger / OpenAPI
- JUnit 5
- Gradle
- Lombok

## 테이블 구조

### `amhs_nodes`

- `id`
- `code`
- `name`
- `type`
- `status`
- `created_at`
- `updated_at`

### `amhs_edges`

- `id`
- `from_node_id`
- `to_node_id`
- `distance`
- `estimated_time_seconds`
- `congestion_level`
- `status`
- `created_at`
- `updated_at`

### `equipments`

- `id`
- `code`
- `name`
- `type`
- `status`
- `created_at`
- `updated_at`

### `transfer_jobs`

- `id`
- `carrier_id`
- `source_node_id`
- `destination_node_id`
- `assigned_equipment_id`
- `status`
- `priority`
- `path`
- `estimated_time_seconds`
- `retry_count`
- `failure_reason`
- `created_at`
- `updated_at`
- `completed_at`
- `failed_at`

### `transfer_job_histories`

- `id`
- `transfer_job_id`
- `status`
- `reason`
- `assigned_equipment_code`
- `path_snapshot`
- `created_at`

### `alerts`

- `id`
- `transfer_job_id`
- `type`
- `status`
- `message`
- `created_at`
- `updated_at`
- `resolved_at`

## 상태 전이

Transfer Job은 다음 상태 전이 규칙을 가집니다.

- `CREATED -> ASSIGNED`
- `CREATED -> CANCELED`
- `ASSIGNED -> MOVING`
- `ASSIGNED -> FAILED`
- `ASSIGNED -> CANCELED`
- `MOVING -> COMPLETED`
- `MOVING -> FAILED`
- `FAILED -> CREATED` 는 일반 상태 변경이 아니라 `retry` 동작에서만 허용

완료되거나 취소된 Job이 다시 이동 상태로 돌아가는 흐름은 차단합니다.

## 경로 탐색 방식

### BFS

가중치를 고려하지 않고, 거쳐가는 Node 수가 가장 적은 경로를 찾습니다.

예:

`STOCKER_01 -> NODE_A -> EQP_01`

### Dijkstra (TIME)

`estimatedTimeSeconds` 기준으로 총 예상 이동 시간이 가장 짧은 경로를 찾습니다.

예:

`STOCKER_01 -> NODE_B -> EQP_01`

### Dijkstra (CONGESTION_AWARE)

가중치를 `estimatedTimeSeconds + congestionLevel`로 계산합니다.

혼잡도가 높은 구간이 있으면 단순 최단 시간 대신 운영 상황을 반영한 경로를 선택할 수 있습니다.

## 장애 우회 예시

기본 경로가 다음과 같다고 가정합니다.

`STOCKER_01 -> NODE_B -> EQP_01`

여기서 `NODE_B` 또는 `STOCKER_01 -> NODE_B` Edge가 `BLOCKED`가 되면, 시스템은 해당 구간을 제외하고 우회 경로를 다시 계산합니다.

예:

`STOCKER_01 -> NODE_A -> EQP_01`

## 장비 자동 할당

대기 중인 Job은 장비 상태와 우선순위를 기준으로 자동 할당할 수 있습니다.

- 할당 대상 장비: `IDLE`
- 제외 장비: `ERROR`, `MAINTENANCE`
- 배정 후보 정렬:
- `priority`: `URGENT > HIGH > NORMAL`
- `createdAt`: 오래된 순
- `retryCount`: 낮은 순

개별 할당:

- `POST /api/transfer-jobs/{id}/assign`

일괄 할당:

- `POST /api/transfer-jobs/assign-pending`

배정 후보 조회:

- `GET /api/transfer-jobs/dispatch-candidates`

## Alert / Scheduler

Scheduler가 주기적으로 운영 이상 징후를 감지합니다.

- `ASSIGNED` 상태가 오래 유지되면 `STUCK_JOB`
- `MOVING` 상태가 예상 시간보다 오래 지속되면 `DELAYED_JOB`
- `ERROR` 장비에 할당된 Job은 `FAILED` 처리 후 `EQUIPMENT_ERROR` 알림 생성

알림 조회 / 해제 API:

- `GET /api/alerts`
- `PATCH /api/alerts/{id}/resolve`

## 실행 방법

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 프로필은 `local`이며 H2 in-memory DB를 사용합니다.

### 2. Swagger 접속

- `http://localhost:8080/swagger-ui.html`

### 3. H2 Console 접속

- `http://localhost:8080/h2-console`

## 샘플 데이터

로컬 프로필 실행 시 샘플 데이터가 자동 생성됩니다.

### Node

- `STOCKER_01`
- `NODE_A`
- `NODE_B`
- `NODE_C`
- `NODE_D`
- `EQP_01`
- `EQP_02`

### Equipment

- `OHT_001`
- `OHT_002`
- `CONVEYOR_001`

### Edge

- `STOCKER_01 -> NODE_A`
- `NODE_A -> NODE_B`
- `NODE_B -> EQP_01`
- `STOCKER_01 -> NODE_C`
- `NODE_C -> NODE_D`
- `NODE_D -> EQP_02`
- `NODE_A -> NODE_D`

역방향 경로도 함께 넣어두어 Swagger에서 바로 경로 탐색과 Job 생성을 테스트할 수 있습니다.

## 시연 시나리오

### 시나리오 1. 정상 반송

1. `POST /api/transfer-jobs`로 `STOCKER_01 -> EQP_01` Job 생성
2. Dijkstra 경로 계산 결과 확인
3. `POST /api/transfer-jobs/{id}/assign`으로 `OHT_001` 자동 할당
4. `PATCH /api/transfer-jobs/{id}/status`로 `MOVING`
5. `PATCH /api/transfer-jobs/{id}/status`로 `COMPLETED`
6. `GET /api/transfer-jobs/{id}/histories`로 이력 확인

### 시나리오 2. 장애 우회

1. `PATCH /api/nodes/{id}/status` 또는 `PATCH /api/edges/{id}/status`로 장애 구간 설정
2. `GET /api/routes/dijkstra` 재호출
3. `BLOCKED` 구간을 제외한 우회 경로 확인

### 시나리오 3. 실패 후 재처리

1. Job 생성 및 장비 할당
2. 상태를 `MOVING -> FAILED`로 변경
3. 장애 Edge를 복구하거나 다른 경로를 열어둠
4. `POST /api/transfer-jobs/{id}/retry`
5. 새 경로 계산 및 `CREATED` 상태 복구 확인

### 시나리오 4. 지연 감지

1. Job 생성 후 장비 할당
2. `ASSIGNED` 또는 `MOVING` 상태 유지
3. Scheduler 감지 후 `GET /api/alerts`로 `STUCK_JOB` 또는 `DELAYED_JOB` 확인
4. `PATCH /api/alerts/{id}/resolve`로 알림 해제

### 시나리오 5. 혼잡도 반영 경로 선택

1. `PATCH /api/edges/{id}/congestion`으로 특정 구간 혼잡도 증가
2. `GET /api/routes/dijkstra?source=...&destination=...&strategy=TIME`
3. `GET /api/routes/dijkstra?source=...&destination=...&strategy=CONGESTION_AWARE`
4. 전략에 따라 선택 경로가 달라지는지 비교

## API 목록

### Node API

- `POST /api/nodes`
- `GET /api/nodes`
- `GET /api/nodes/{id}`
- `PATCH /api/nodes/{id}/status`

### Edge API

- `POST /api/edges`
- `GET /api/edges`
- `GET /api/edges/{id}`
- `PATCH /api/edges/{id}/status`
- `PATCH /api/edges/{id}/congestion`

### Equipment API

- `POST /api/equipments`
- `GET /api/equipments`
- `GET /api/equipments/{id}`
- `PATCH /api/equipments/{id}/status`

### Route API

- `GET /api/routes/bfs?source=STOCKER_01&destination=EQP_01`
- `GET /api/routes/dijkstra?source=STOCKER_01&destination=EQP_01&strategy=TIME`
- `GET /api/routes/dijkstra?source=STOCKER_01&destination=EQP_01&strategy=CONGESTION_AWARE`

### Transfer Job API

- `POST /api/transfer-jobs`
- `GET /api/transfer-jobs`
- `GET /api/transfer-jobs/dispatch-candidates`
- `GET /api/transfer-jobs/{id}`
- `PATCH /api/transfer-jobs/{id}/status`
- `POST /api/transfer-jobs/{id}/assign`
- `POST /api/transfer-jobs/assign-pending`
- `POST /api/transfer-jobs/{id}/retry`
- `GET /api/transfer-jobs/{id}/histories`

### Alert API

- `GET /api/alerts`
- `PATCH /api/alerts/{id}/resolve`

### Dashboard API

- `GET /api/dashboard/summary`

## 테스트 방법

전체 테스트 실행:

```bash
./gradlew test
```

현재 포함된 주요 테스트 항목:

- BFS 경로 탐색 성공
- Dijkstra 경로 탐색 성공
- `BLOCKED` Node 제외 후 우회 경로 탐색
- `BLOCKED` Edge 제외 후 우회 경로 탐색
- 혼잡도 반영 경로 탐색
- 경로가 없을 때 예외 발생
- Transfer Job 생성 시 경로 저장
- Transfer Job 상태 변경 시 History 저장
- `FAILED` Job retry 성공
- `FAILED`가 아닌 Job retry 시 예외 발생
- 장비 자동 할당 및 대기 Job 일괄 할당
- 배정 후보 Queue 정렬 검증
- 상태 전이 규칙 검증
- Alert 감지 및 resolve 검증
- Dashboard summary 집계 검증

## 구현 포인트

- 상태값과 상태 전이 규칙을 중심으로 도메인을 구성해 비정상 흐름을 차단했습니다.
- 경로 탐색은 BFS, Dijkstra(TIME), Dijkstra(CONGESTION_AWARE)로 분리해 운영 상황에 따라 비교할 수 있게 했습니다.
- 장애 상태인 Node / Edge를 탐색 대상에서 제외해 경로 재계산 시나리오를 반영했습니다.
- 대기 중인 Job을 우선순위와 장비 상태 기준으로 정렬해 자동 할당할 수 있도록 구성했습니다.
- Transfer Job 이력을 별도 테이블로 관리해 반송 흐름과 장애 복구 이력을 추적할 수 있도록 했습니다.
- Scheduler와 Alert 도메인을 통해 지연 / 장비 이상 상황을 운영 관점에서 확인할 수 있도록 구성했습니다.
