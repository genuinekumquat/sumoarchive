-- 메인 「이번 반즈케 변동」(신입막·재입막·신주료·재주료·신제자)용 - sumo-api 전체 계급 이력(/api/ranks)에서
-- 처음 반즈케에 오른 / 처음 주료 이상 / 처음 마쿠우치에 오른 바쇼(YYYYMM). 우리 반즈케는 2019년부터라 그 전 경력은
-- 여기서만 안다. null = 그 지위에 오른 적 없음. rank_history_synced=0이면 아직 이력을 안 받아서 모름.
ALTER TABLE `rikishi`
  ADD COLUMN `first_banzuke_basho` varchar(6) DEFAULT NULL,
  ADD COLUMN `first_juryo_basho` varchar(6) DEFAULT NULL,
  ADD COLUMN `first_makuuchi_basho` varchar(6) DEFAULT NULL,
  ADD COLUMN `rank_history_synced` bit(1) NOT NULL DEFAULT b'0';
