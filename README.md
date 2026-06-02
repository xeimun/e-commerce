# E-Commerce

CS 지식을 활용해 주문 처리 성능을 개선하는 이커머스 백엔드 프로젝트입니다.

## 프로젝트 목표

- 주문 생성 기능을 구현하고, 분당 100건의 주문 처리 가능 여부를 검증합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.x
- Gradle
- Spring Web
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway
- JUnit 5

## 주요 기능

작성 예정

## 실행 방법

PostgreSQL을 실행합니다.

```powershell
docker compose up -d
```

기본 로컬 DB 포트는 `15432`입니다.

애플리케이션을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

## 테스트 방법

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## 성능 개선 기록

작성 예정
