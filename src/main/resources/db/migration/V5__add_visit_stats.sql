-- 방문 통계 (관리자 /admin/stats). 날짜별 합계만 저장한다 — IP·브라우저 정보는 저장하지 않는다.
-- 순방문자는 앱 메모리에서 그날만 쓰는 랜덤 값으로 해시해 세고(VisitStatsCollector), 여기엔 숫자만 더한다.

-- 하루 전체: 순방문자 · 페이지뷰 · 모바일 순방문자
CREATE TABLE `site_daily_stat` (
  `stat_date` date NOT NULL,
  `visitors` int NOT NULL DEFAULT 0,
  `page_views` int NOT NULL DEFAULT 0,
  `mobile_visitors` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 페이지별 뷰: page_type = MAIN·RIKISHI·TORIKUMI·SEARCH·BOOKMARK·UPDATES·PRIVACY, target_id = 리키시/경기 id (없으면 0)
CREATE TABLE `page_daily_stat` (
  `stat_date` date NOT NULL,
  `page_type` varchar(20) NOT NULL,
  `target_id` int NOT NULL DEFAULT 0,
  `views` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`stat_date`, `page_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 외부 유입: source = dcinside·google·naver·daum·x·facebook·bing·other (사이트 단위로만 묶음, 주소 전체는 저장하지 않음)
CREATE TABLE `referrer_daily_stat` (
  `stat_date` date NOT NULL,
  `source` varchar(20) NOT NULL,
  `visits` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`stat_date`, `source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
