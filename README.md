# 🏯 sumoarchive

한국어로 오즈모(大相撲) 정보를 편하게 찾아보는 아카이브 웹 서비스입니다.

---

## 1. 기획 의도

일본 스모(오즈모)는 반즈케(番付)·시코나(四股名)·키마리테(決まり手) 등 고유 용어가 많고,
공식 정보 대부분이 일본어로만 제공되어 한국어 사용자가 접근하기 어렵습니다.

**sumoarchive는 한국어 사용자가 별도의 배경지식 없이도 리키시(선수) 프로필, 반즈케, 경기 결과를
편하게 찾아보고 즐길 수 있게 하는 것을 목표로 만든 프로젝트입니다.**

MVP(현재 단계) 이후에는 다음과 같은 방향으로 서비스를 확장할 계획입니다.

- **키마리테 백과사전**: 지금은 원형 차트로 통계만 보여주는 결정기술(키마리테)에, 기술 설명·움직임을
  텍스트/이미지로 풀어주는 사전 기능 추가
- **반즈케 예측 시뮬레이터**: 직전 바쇼 성적을 바탕으로 다음 바쇼 반즈케(순위표) 변동을
  가중치 연산으로 예측해보는 시뮬레이터

---

## 2. 목표 시나리오

기능명세서(`docs/기능명세서.txt`) 기준으로, 사용자는 다음을 할 수 있습니다.

1. **메인 화면**에서 로그인 없이 바로 최신 마쿠우치(幕內) 반즈케를 확인할 수 있다. `番付` 아래
   **연도 · 대회 · 계급 드롭다운 3개**로 원하는 반즈케를 골라 비동기로 불러온다 — 연도를 바꾸면
   그 해에 열린 대회만 대회 드롭다운에 다시 채워지고, 계급(마쿠우치/주료/마쿠시타/산단메/조니단/
   조노구치)은 독립적으로 선택된다. 기본값은 최신 대회 · 마쿠우치 (본장소 개최 전이라도 발표된
   반즈케면 노출).
2. 반즈케 표에서 **리키시 이름을 클릭**해 해당 선수의 프로필 페이지로 이동할 수 있다.
3. **검색창**에 한국어 시코나 / 일본어 시코나 / 과거에 쓰던 시코나까지 통합 검색해서, 동명이인이나
   대물림 시코나(습명)가 있어도 실명·활동 시기·최고 계급·현재 상태(현역/은퇴/오야카타)를 보고
   원하는 선수를 정확히 골라 들어갈 수 있다.
4. **리키시 프로필 페이지**에서 기본 정보(본명·생년월일·출신·소속 헤야·신장/체중 등), 커리어 레코드
   (통산 전적·유쇼·산쇼·킨보시), 키마리테 승리 기술 통계(원형 차트)를 한눈에 볼 수 있다.
5. 프로필 하단 **호시토리표(星取表)**에서 데뷔 이후 전 바쇼 성적을 일차별로 확인하고, 개별 경기
   아이콘(○/●/휴)을 클릭해 그 경기의 상세 정보를 슬라이드 패널로 바로 열어볼 수 있다.
6. **토리쿠미(경기) 상세 페이지**에서 대회 정보(바쇼·일차), 동/서 대진, 결정기술(한/일 병기), 유튜브
   하이라이트 영상을 확인할 수 있다.
7. 토리쿠미 상세 화면에서 **닉네임/숫자 4자리 비밀번호로 익명 댓글**을 남기고, 본인이 남긴 댓글을
   비밀번호로 직접 지울 수 있다.
8. *(관리자)* `/admin/login`으로 로그인하면 댓글 관리 대시보드(`/admin/comments`)로 이동해, 사이트
   전체 토리쿠미의 댓글을 최신순으로 한눈에 모아보고 부적절한 댓글을 비밀번호 검증 없이 즉시
   블라인드 처리할 수 있다.
9. **즐겨찾기 페이지**(`/bookmark`)에서 관심 리키시를 북마크해서(LocalStorage 기반, 로그인 불필요)
   나만의 리스트를 만들고, 드래그 앤 드롭으로 순서를 바꿀 수 있다.
10. *(관리자)* `/admin/rikishi`에서 리키시를 검색해 프로필 정보(기본 정보/계급/현역·은퇴/사진)를
    직접 수정할 수 있다.
11. *(관리자)* `/admin/basho`에서 새 바쇼를 만들고, 그 바쇼의 반즈케를 리키시 한 명씩
    (디비전/계급/동서/번호) 추가·수정·삭제할 수 있다. 반즈케가 곧 계급의 source of truth라,
    편집 결과는 리키시 상세 페이지·메인 대시보드에 그대로 반영된다.
12. *(관리자)* `/admin/basho/{id}/torikumi`에서 그 바쇼의 대전을 일차·디비전별로 추가·수정·삭제할
    수 있다 (동/서 리키시, 승자, 결정기술, 부전승패, 결정전 여부, 유튜브 URL, 한/일 해설).
    입력한 대전은 토리쿠미 상세 페이지·리키시 호시토리표·키마리테 통계에 그대로 반영된다.

---

## 3. 기술 스택

| 구분 | 스택 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Spring MVC, Spring Data JPA) |
| View | Thymeleaf (서버사이드 렌더링 + fragment 재사용) |
| DB | MySQL 8.0 (`mysql-connector-j`), Hibernate ORM |
| Frontend | Vanilla JS(fetch 기반 부분 갱신), Chart.js(CDN, 키마리테 도넛 차트), Google Fonts |
| 인증 | Spring Security 미사용 — 세션(`HttpSession`) + 커스텀 인터셉터 기반 경량 구현 |
| 빌드 | Gradle (Gradle Wrapper 포함) |
| 기타 | Lombok |

---

## 4. 프로젝트 구조

```
sumoarchive/
├── docs/                                  # 기능/테이블 명세서, ERD, DB 스키마 DDL
│   ├── 기능명세서.txt
│   ├── 테이블명세서_v1.txt / v2.txt
│   ├── sumoarchive_db_schema.sql
│   └── erd.png
└── src/main/java/com/torikumilab/sumoarchive/
    ├── controller/            # 화면(View) 컨트롤러 — Thymeleaf 뷰 이름 반환
    │   ├── advice/              # ViewExceptionHandler (화면 쪽 404 등 공통 예외 처리)
    │   └── api/                 # JSON API 컨트롤러 + ApiExceptionHandler
    ├── config/                # WebConfig, AdminAuthInterceptor (/admin/** 세션 가드)
    ├── domain/
    │   ├── entity/              # JPA 엔티티 (+ constant 하위 enum)
    │   └── dto/                 # 화면/응답 전용 DTO (표시용 문자열은 서버에서 조립)
    ├── repository/            # Spring Data JPA 리포지토리 (+ QueryDSL 없이 커스텀 구현체)
    ├── service/               # 비즈니스 로직 (+ exception 하위 도메인 예외)
    ├── util/                  # RankDisplayUtil, KimariteDisplayUtil, YoutubeUrlUtil 등
    ├── client/                # SumoApiClient (sumo-api.com 호출 래퍼)
    └── DataSeeder.java        # 개발용 더미데이터 시더 (기본 비활성 — app.seed-demo=true일 때만)
```

`src/main/resources/`
```
templates/       # index, search/result, rikishi/detail, torikumi/detail, admin/login, error/404
static/          # css, js, images
application.properties                  # 커밋 대상 — 실제 비밀번호 없음
application-local.properties.example    # 커밋 대상 — 로컬 설정 템플릿
application-local.properties            # git 제외 — 실제 DB/관리자 비밀번호
```

---

## 5. API 요약

### 화면(View) 라우트 — Thymeleaf 렌더링

| Method | Path | 설명 |
|---|---|---|
| GET | `/` | 메인 대시보드 (최신 마쿠우치 반즈케 기본 노출) |
| GET | `/search?keyword=&page=` | 리키시 통합 검색 결과 목록 |
| GET | `/rikishi/{id}` | 리키시 상세 프로필 |
| GET | `/torikumi/{id}` | 토리쿠미(경기) 상세 전체 페이지 |
| GET | `/torikumi/{id}/fragment` | 위와 동일한 내용의 조각(fragment) — 슬라이드 패널 삽입용 |
| GET | `/bookmark` | 즐겨찾기 페이지 (LocalStorage ID 배열로 카드 목록 구성) |
| GET / POST | `/admin/login` | 관리자 로그인 폼 조회 / 로그인 처리 (성공 시 `/admin/comments`로 이동) |
| POST | `/admin/logout` | 관리자 로그아웃 (세션 무효화) |
| GET | `/admin/comments?page=` | 관리자 댓글 관리 대시보드 (`/admin/**`, 세션 `isAdmin` 가드) |
| GET | `/admin/rikishi?keyword=&page=` | 관리자 리키시 목록/검색 |
| GET / POST | `/admin/rikishi/{id}/edit` | 리키시 프로필 수정 폼 조회 / 저장 |
| GET | `/admin/basho` | 관리자 바쇼 목록 (반즈케·토리쿠미 수 포함) |
| GET / POST | `/admin/basho/new`, `/admin/basho` | 새 바쇼 생성 폼 조회 / 생성 (연·월 중복 차단) |
| POST | `/admin/basho/import?fromYear=&toYear=` | sumo-api에서 연도 범위 바쇼 메타 임포트 (본장소 6개월, 미개최분 스킵) |
| GET / POST | `/admin/basho/{bashoId}/edit`, `.../{bashoId}` , `.../{bashoId}/delete` | 바쇼 기간 수정 폼 / 시작일·종료일 수정 / 삭제 (반즈케·토리쿠미가 비어야 삭제) |
| GET | `/admin/basho/{bashoId}/banzuke?division=` | 바쇼별 반즈케 관리 (디비전 탭 + 행 추가 폼) |
| POST | `/admin/basho/{bashoId}/banzuke` | 반즈케 행 추가 (리키시·바쇼 중복 차단) |
| POST | `/admin/basho/{bashoId}/banzuke/{banzukeId}` , `.../delete` | 반즈케 행 수정 / 삭제 |
| GET | `/admin/basho/{bashoId}/torikumi?day=&division=` | 바쇼별 대전 목록 (일차 링크 + 디비전 필터) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/new`, `/admin/basho/{bashoId}/torikumi` | 새 대전 폼 조회 / 생성 (같은 날·동서 조합 중복 차단) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/{id}/edit`, `.../{id}` , `.../{id}/delete` | 대전 편집 폼 / 수정 / 삭제 |

### API 라우트 — JSON

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/banzuke?division=&bashoId=` | 디비전별 반즈케 목록 (탭 전환·바쇼 선택 시 비동기 호출; `bashoId` 생략 시 최신 바쇼) |
| GET | `/api/search?keyword=&page=` | 리키시 검색 (페이지네이션) |
| GET | `/api/rikishiEntity?ids=3,15,24` | 즐겨찾기 페이지용 — ID 목록으로 리키시 카드 조회 (요청 순서 유지) |
| GET | `/api/torikumi/{torikumiId}/comments` | 해당 경기 댓글 전체 조회 |
| POST | `/api/torikumi/{torikumiId}/comments` | 댓글 작성 (nickname, password, content) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/delete` | 본인 댓글 삭제 (password 검증) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/blind` | 관리자 블라인드 (세션 `isAdmin` 필요) |

> 댓글 API는 등록/삭제/블라인드 모두 **그 경기의 갱신된 댓글 목록 전체**를 응답으로 돌려줘서,
> 프론트는 응답을 그대로 다시 렌더링하기만 하면 되도록 설계했습니다.

---

## 6. 핵심 설계 포인트

- **표시 문자열은 서버에서 완성해서 내려줌**: "前頭10枚目", "3일째/3日目" 같은 표기 변환을
  `RankDisplayUtil`/`KimariteDisplayUtil` 등 유틸로 서비스 계층에서 미리 만들어 DTO에 담고,
  템플릿에서는 최소한의 분기만 하도록 설계했습니다.
- **Thymeleaf fragment 재사용**: 토리쿠미 상세는 독립 페이지(`/torikumi/{id}`)와 리키시 프로필의
  슬라이드 패널(`/torikumi/{id}/fragment`)이 완전히 같은 `th:fragment="content"`를 공유합니다.
  패널은 그 fragment를 fetch로 받아 그대로 삽입하고, 삽입 후 `SumoComments.bind()`만 다시 호출해
  댓글 기능까지 재사용합니다.
- **배치 조회로 N+1 방지**: 토리쿠미/호시토리표에서 동서 두 선수(또는 여러 상대)의 반즈케를 조회할
  때 `IN` 절 배치 쿼리 한 번으로 모아 조회한 뒤 Map으로 매핑합니다.
- **소프트 딜리트 + 사유 구분**: 댓글 삭제는 물리 삭제 없이 `is_deleted` 플래그만 갱신하고,
  `DeletedBy`(USER/ADMIN) enum으로 "작성자에 의해 삭제됨"과 "관리자에 의해 블라인드됨"을 구분
  표시해 레이아웃과 맥락을 유지합니다.
- **예외 처리를 화면/API로 이원화**: 같은 `EntityNotFoundException`이라도 `controller` 패키지(화면)는
  `ViewExceptionHandler`가 404 페이지로, `controller.api` 패키지는 `ApiExceptionHandler`
  (`@Order(HIGHEST_PRECEDENCE)`)가 JSON으로 각각 다르게 변환합니다.
- **경량 관리자 인증**: Spring Security 없이 세션 속성 `isAdmin`만으로 관리자를 구분합니다.
  `AdminAuthInterceptor` + `WebConfig`가 `/admin/**` 경로를 가드하고, 댓글 블라인드 API는
  별도로 자체 세션 체크 후 `AdminOnlyException`(401)을 던지는 방식으로 이중 게이트를 둡니다.
- **화면-API 재사용**: 댓글 관리 대시보드(`/admin/comments`)는 새 삭제 API를 만들지 않고, 토리쿠미
  상세 화면이 쓰던 블라인드 API(`POST /api/torikumi/{id}/comments/{id}/blind`)를 fetch로 그대로
  호출합니다.
- **서버가 모르는 개인화**: 즐겨찾기는 DB에 전혀 저장하지 않고 브라우저 LocalStorage의 ID 배열이
  유일한 저장소입니다. 서버는 그 배열을 절대 순서를 바꾸지 않고 그대로 응답 순서에 반영해서, 드래그로
  바꾼 정렬이 새로고침 후에도 유지되도록 합니다.
- **`<input type="date">`는 로케일 포맷을 믿지 않음**: `@DateTimeFormat(pattern = "yyyy-MM-dd")`을
  명시하지 않으면 Spring이 요청 로케일(한국어) 기준 short style("94. 3. 1.")로 값을 내려줘서 브라우저가
  날짜를 못 읽습니다. 관리자 리키시 수정 폼의 생년월일/데뷔일/은퇴일 모두 이 패턴을 명시했습니다.

---

## 7. Infrastructure / 환경 설정

- **DB**: MySQL 8.0. 스키마 DDL은 `docs/sumoarchive_db_schema.sql` 참고 (실제로는
  `spring.jpa.hibernate.ddl-auto=update`로 엔티티 기준 자동 반영하며 개발 중입니다).
- **시크릿 분리**: `application.properties`는 커밋되지만 실제 값이 없고, DB 계정과 관리자
  로그인 계정은 `application-local.properties`(git 제외)에서만 관리합니다.
  ```properties
  spring.config.import=optional:classpath:application-local.properties
  ```
  로 로컬 설정을 선택적으로 불러오는 구조입니다.
- **데이터 소스**: 초기에는 `DataSeeder`(개발용 더미)로 채웠으나, 이제 **sumo-api.com**에서
  실데이터를 가져오는 방향으로 전환 중입니다. `DataSeeder`는 기본 비활성(`app.seed-demo=false`)이고,
  관리자 `/admin/rikishi`의 "로스터 임포트" 버튼이 현역 헤야·리키시를, `/admin/basho`의
  "바쇼 임포트" 버튼이 연도 범위의 바쇼 메타를 가져옵니다 (반즈케/토리쿠미 임포트는 후속 작업).
  외부 API 관련 설정: `sumo-api.base-url`, `app.seed-demo`.

### 로컬 실행 방법

```bash
# 1. MySQL에 스키마 생성 (예: sumo)
CREATE DATABASE sumo CHARACTER SET utf8mb4;

# 2. 로컬 설정 파일 생성
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# → DB 계정, 원하는 관리자 아이디/비밀번호로 값 채우기

# 3. 실행 (최초 기동 시 더미데이터 자동 시딩)
./gradlew bootRun
```

기본 접속: `http://localhost:8080`
관리자 로그인: `http://localhost:8080/admin/login` (로그인 성공 시 댓글 관리 대시보드로 이동)

---

## 8. 다음 단계 (로드맵)

- [x] 관리자 댓글 관리 대시보드 (`/admin/comments` — 전체 댓글 조회 + 블라인드)
- [x] 즐겨찾기 페이지 (`/bookmark`, LocalStorage 기반, 로그인 불필요, 드래그 정렬)
- [x] 관리자 리키시 프로필 수정 (`/admin/rikishi` — 목록/검색 + 수정 폼)
- [x] 관리자 반즈케 데이터 수동 갱신 (`/admin/basho` — 바쇼 생성·기간수정·삭제 + 반즈케 행 CRUD)
- [x] 관리자 토리쿠미(대전) 입력 UI (`/admin/basho/{id}/torikumi` — 일차·디비전별 대전 CRUD)
- [ ] 키마리테 상세 설명 백과사전
- [ ] 반즈케 예측 시뮬레이터
- [ ] 외부 스모 데이터 API(sumo-api.com) 연동 — *진행 중*: 로스터·바쇼 임포트 완료,
  반즈케/토리쿠미 임포트는 진행 예정

---

## 9. 문서

- [`docs/기능명세서.txt`](docs/기능명세서.txt) — 전체 기능 명세
- [`docs/테이블명세서_v2.txt`](docs/테이블명세서_v2.txt), [`docs/sumoarchive_db_schema.sql`](docs/sumoarchive_db_schema.sql) — DB 설계
- [`docs/erd.png`](docs/erd.png) — ERD 다이어그램
