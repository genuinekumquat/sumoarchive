-- 발표됐지만 아직 안 치른 대진(예정 대진). 대진은 전날 낮(첫날·둘째 날은 개막 이틀 전)에 발표되고, sumo-api는 이걸
-- 승자·결정기술 없이 준다. 통계가 "승자 있는 경기"를 전제로 하는 torikumi와 섞지 않으려고 따로 둔다.
-- 경기 임포트(TorikumiImportService)가 디비전마다 동기화한다: 결과가 나오면 torikumi로 들어가고 여기선 지워진다.
CREATE TABLE `scheduled_bout` (
  `id` int NOT NULL AUTO_INCREMENT,
  `basho_id` int NOT NULL,
  `day` int NOT NULL,
  `division` enum('Jonidan','Jonokuchi','Juryo','Makushita','Makuuchi','Sandanme') NOT NULL,
  `match_no` int DEFAULT NULL,
  `external_id` varchar(255) NOT NULL,
  `east_rikishi_id` int NOT NULL,
  `west_rikishi_id` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_scheduled_bout_external_id` (`external_id`),
  KEY `idx_scheduled_bout_basho_division` (`basho_id`, `division`),
  KEY `fk_scheduled_bout_east` (`east_rikishi_id`),
  KEY `fk_scheduled_bout_west` (`west_rikishi_id`),
  CONSTRAINT `fk_scheduled_bout_basho` FOREIGN KEY (`basho_id`) REFERENCES `basho` (`id`),
  CONSTRAINT `fk_scheduled_bout_east` FOREIGN KEY (`east_rikishi_id`) REFERENCES `rikishi` (`id`),
  CONSTRAINT `fk_scheduled_bout_west` FOREIGN KEY (`west_rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
