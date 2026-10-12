# 화면·API 목록

[README로 돌아가기](../README.md) | [日本語](api.ja.md)

> 공개 화면과 JSON API, 관리자 화면의 전체 목록입니다. README에서 옮겨 왔고(2026.10.12), 이후 추가된 항목을 더했습니다.

## 화면(View) 라우트 — Thymeleaf 렌더링

| Method | Path | 설명 |
|---|---|---|
| GET | `/` | 메인 대시보드 (최신 마쿠우치 반즈케 기본 노출) |
| GET | `/search?keyword=&page=` | 리키시 통합 검색 결과 목록 |
| GET | `/rikishi/{id}` | 리키시 상세 프로필 |
| GET | `/torikumi/{id}` | 토리쿠미(경기) 상세 전체 페이지 |
| GET | `/torikumi/{id}/fragment` | 위와 동일한 내용의 조각(fragment) — 슬라이드 패널 삽입용 |
| GET | `/bookmark` | 즐겨찾기 페이지 (LocalStorage ID 배열로 카드 목록 구성) |
| GET | `/updates` | 업데이트 소식 (`resources/updates.json`, 한/일) |
| GET | `/privacy` | 개인정보 처리 안내 (한/일) |
| GET | `/robots.txt`, `/sitemap.xml` | 검색엔진용 (사이트맵은 마쿠우치·주료 리키시 프로필 + 한/일 주소) |
| GET | `/admin/stats?days=7|30|90` | 관리자 방문 통계 (일별 방문자·페이지뷰, 많이 본 리키시, 유입처. IP 미저장) |
| GET / POST | `/admin/login` | 관리자 로그인 폼 조회 / 로그인 처리 (성공 시 `/admin/comments`로 이동) |
| POST | `/admin/logout` | 관리자 로그아웃 (세션 무효화) |
| GET | `/admin/comments?page=` | 관리자 댓글 관리 대시보드 (`/admin/**`, 세션 `isAdmin` 가드) |
| GET | `/admin/rikishi?keyword=&page=` | 관리자 리키시 목록/검색 |
| GET / POST | `/admin/rikishi/{id}/edit` | 리키시 프로필 수정 폼 조회 / 저장 |
| GET | `/admin/basho` | 관리자 바쇼 목록 (반즈케·토리쿠미 수 포함) |
| GET / POST | `/admin/basho/new`, `/admin/basho` | 새 바쇼 생성 폼 조회 / 생성 (연·월 중복 차단) |
| POST | `/admin/basho/import?fromYear=&toYear=` | sumo-api에서 연도 범위 바쇼 메타 임포트 (본장소 6개월, 미개최분 스킵) |
| POST | `/admin/basho/import-year?year=` | 해당 연도 전 바쇼의 전 디비전(마쿠우치~조노구치) 반즈케(공식 승·패·휴 포함), 토리쿠미, 우승·산쇼·킨보시 일괄 임포트 |
| POST | `/admin/basho/{bashoId}/import-all` | 바쇼 1건의 전체 데이터(반즈케·토리쿠미·수상) 일괄 임포트 |
| POST | `/admin/basho/{bashoId}/banzuke/import?division=` , `/admin/basho/{bashoId}/torikumi/import?division=` | 바쇼·디비전 단위 반즈케 / 토리쿠미 임포트 (미매칭 리키시 스킵) |
| POST | `/admin/basho/{bashoId}/awards/import` | 우승·산쇼(sumo-api) + 킨보시(반즈케·토리쿠미에서 파생) 갱신 (재실행 가능) |
| GET / POST | `/admin/basho/{bashoId}/edit`, `.../{bashoId}` , `.../{bashoId}/delete` | 바쇼 기간 수정 폼 / 시작일·종료일 수정 / 삭제 (반즈케·토리쿠미가 비어야 삭제) |
| GET | `/admin/basho/{bashoId}/banzuke?division=` | 바쇼별 반즈케 관리 (디비전 탭 + 행 추가 폼) |
| POST | `/admin/basho/{bashoId}/banzuke` | 반즈케 행 추가 (리키시·바쇼 중복 차단) |
| POST | `/admin/basho/{bashoId}/banzuke/{banzukeId}` , `.../delete` | 반즈케 행 수정 / 삭제 |
| GET | `/admin/basho/{bashoId}/torikumi?day=&division=` | 바쇼별 대전 목록 (일차 링크 + 디비전 필터) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/new`, `/admin/basho/{bashoId}/torikumi` | 새 대전 폼 조회 / 생성 (같은 날·동서 조합 중복 차단) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/{id}/edit`, `.../{id}` , `.../{id}/delete` | 대전 편집 폼 / 수정 / 삭제 |

## API 라우트 — JSON

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/banzuke?division=&bashoId=` | 디비전별 반즈케 목록 (탭 전환·바쇼 선택 시 비동기 호출; `bashoId` 생략 시 최신 바쇼) |
| GET | `/api/search?keyword=&page=` | 리키시 검색 (페이지네이션) |
| GET | `/api/rikishiEntity?ids=3,15,24` | 즐겨찾기 페이지용 — `{basho, cards}`: 기준 바쇼 + 요청 순서대로의 카드(이번 바쇼 계급·성적·마지막 경기·다음 경기). 최대 50개 |
| GET | `/api/torikumi/day?bashoId=&division=&day=` | 메인 「대진」 탭 — 하루치 결과·예정 대진(경기 순서), 선수별 그 경기 전 성적, 일차 목록과 상태 |
| GET | `/api/torikumi/{torikumiId}/comments` | 해당 경기 댓글 전체 조회 |
| POST | `/api/torikumi/{torikumiId}/comments` | 댓글 작성 (nickname, password, content) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/delete` | 본인 댓글 삭제 (password 검증) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/blind` | 관리자 블라인드 (세션 `isAdmin` 필요) |

> 댓글 API는 등록/삭제/블라인드 모두 **그 경기의 갱신된 댓글 목록 전체**를 응답으로 돌려줘서,
> 프론트는 응답을 그대로 다시 렌더링하기만 하면 되도록 설계했습니다.

