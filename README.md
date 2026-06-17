# AMHS Backend

Spring Boot 기반 AMHS 반송 Job 경로 탐색 및 상태 관리 시스템 프로젝트의 초기 골격입니다.

## 포함된 기본 설정

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Validation
- H2 / MariaDB profile 분리
- Swagger(OpenAPI)
- Lombok
- Global exception response
- JPA auditing `BaseTimeEntity`

## 실행

```bash
./gradlew bootRun
```

기본 프로필은 `local`이며 H2 in-memory DB를 사용합니다.

## Swagger

- `http://localhost:8080/swagger-ui.html`

## H2 Console

- `http://localhost:8080/h2-console`

## 다음 구현 대상

- Node / Edge / Equipment / Route / TransferJob / Dashboard 도메인 구현
