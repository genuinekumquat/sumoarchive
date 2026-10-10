-- 바쇼별 공식 성적(승·패·휴). sumo-api 반즈케 응답의 wins/losses/absences를 그대로 저장한다.
-- 마쿠시타 이하는 15일 중 7번만 싸우므로 "경기 없는 날 = 휴장"으로 추론할 수 없어서, 성적은 이 값을 기준으로 한다.
-- 관리자가 손으로 넣은 반즈케나 임포트 전 행은 NULL (그때는 등록된 토리쿠미로 계산).
ALTER TABLE `banzuke`
  ADD COLUMN `wins` int DEFAULT NULL,
  ADD COLUMN `losses` int DEFAULT NULL,
  ADD COLUMN `absences` int DEFAULT NULL;
