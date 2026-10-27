-- 즐겨찾기 통계 (관리자 /admin/stats). 리키시별·날짜별로 즐겨찾기 추가/해제 횟수만 더한다 — 누가 했는지는 저장하지 않는다.
-- 즐겨찾기 목록 자체는 여전히 방문자 브라우저(localStorage)에만 있고, ☆를 누를 때 "이 리키시 +1/−1"만 온다.
-- 같은 날 같은 방문자의 같은 리키시 추가(또는 해제)는 한 번만 센다(VisitStatsCollector, 방문자 수와 같은 해시 방식).
CREATE TABLE `bookmark_daily_stat` (
  `stat_date` date NOT NULL,
  `rikishi_id` int NOT NULL,
  `adds` int NOT NULL DEFAULT 0,
  `removes` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`stat_date`, `rikishi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
