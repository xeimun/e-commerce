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
- React
- Vite
- JavaScript

## 주요 기능

- 상품과 재고 생성/조회/수정
- 고객별 장바구니 조회
- 장바구니 상품 추가, 수량 변경, 삭제
- 장바구니 선택 상품 기반 주문 생성
- 결제 대기 주문 저장과 재고 예약 차감
- 가짜 결제 성공/취소와 재고 복구
- 결제 대기 주문 만료 자동 취소와 재고 복구
- 고객 보유 쿠폰 예약/사용과 결제 취소·만료 시 예약 해제
- 상품 즉시 할인 설정과 주문 수량별 할인 적용
- 쿠폰 발급 가능 목록, 고객별 1회 발급, 내 보유 쿠폰 조회
- 상품 목록/상세와 장바구니를 사용하는 프론트엔드 MVP UI

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

프론트엔드 개발 서버를 실행합니다.

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

프론트엔드 기본 주소는 `http://localhost:5173`입니다.
Vite 개발 서버는 `/api` 요청을 `http://localhost:8080` 백엔드로 프록시합니다.

## 테스트 방법

```powershell
.\gradlew.bat test
.\gradlew.bat build
cd frontend
npm.cmd run build
```

## 성능 개선 기록

- 인기 상품 주문 생성 재고 차감 개선: 조건부 업데이트로 초과 판매 0건을 유지하면서 k6 VU 100 기준 p95 응답 시간을 1,610.89ms에서 1,342.18ms로 낮추고 주문 생성 구간 처리량을 76.97건/초에서 103.76건/초로 개선했습니다.
