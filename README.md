# E-Commerce

콘텐츠 IP 기반 한정판 굿즈를 판매하는 드롭 커머스 플랫폼입니다.
CS 지식을 활용해 주문 처리 성능을 개선하는 이커머스 백엔드 프로젝트입니다.

## 서비스 컨셉

웹툰, 웹소설, 영화, 드라마, 음악 등 콘텐츠 IP의 한정판 굿즈를 기간 한정으로 판매합니다.
초기에는 단일 운영자가 상품과 이벤트를 관리하는 구조로 시작하고, 파트너사 또는 창작자 입점 구조는 이후 확장 후보로 둡니다.

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

- 상품과 재고 생성/조회/수정
- 고객별 장바구니 조회
- 장바구니 상품 추가, 수량 변경, 삭제

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
