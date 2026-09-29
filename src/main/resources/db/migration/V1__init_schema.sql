-- V1: 초기 스키마 (2026-09-29 로컬 DB 기준, JPA ddl-auto=update로 만들어진 상태를 그대로 옮김)
-- 이미 테이블이 있는 DB(로컬 등)는 baseline-on-migrate로 "V1 적용됨" 표시만 되고 실행되지 않는다.
-- heya.master_rikishi_id <-> rikishi.heya_id 가 서로 참조하므로 FK 검사를 잠시 끈다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `heya` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `ichimon_jp` varchar(100) DEFAULT NULL,
  `ichimon_kr` varchar(100) DEFAULT NULL,
  `master_rikishi_id` int DEFAULT NULL,
  `name_jp` varchar(100) NOT NULL,
  `name_kr` varchar(100) NOT NULL,
  `name_en` varchar(100) DEFAULT NULL,
  `name_kr_auto` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmlm00e59ggyq9jd441vjb3jgg` (`master_rikishi_id`),
  CONSTRAINT `FKmlm00e59ggyq9jd441vjb3jgg` FOREIGN KEY (`master_rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rikishi` (
  `id` int NOT NULL AUTO_INCREMENT,
  `birthdate` date DEFAULT NULL,
  `birthplace` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `current_rank` varchar(50) DEFAULT NULL,
  `debut_date` date DEFAULT NULL,
  `external_api_id` int DEFAULT NULL,
  `fighting_style` varchar(255) DEFAULT NULL,
  `height` decimal(4,1) DEFAULT NULL,
  `highest_rank` varchar(50) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `nationality` varchar(100) DEFAULT NULL,
  `oyakata_name_jp` varchar(100) DEFAULT NULL,
  `oyakata_name_kr` varchar(100) DEFAULT NULL,
  `photo_url` varchar(255) DEFAULT NULL,
  `retired_date` date DEFAULT NULL,
  `shikona_jp` varchar(100) DEFAULT NULL,
  `shikona_kr` varchar(100) DEFAULT NULL,
  `weight` decimal(4,1) DEFAULT NULL,
  `heya_id` int DEFAULT NULL,
  `shikona_en` varchar(100) DEFAULT NULL,
  `shikona_kr_auto` bit(1) DEFAULT NULL,
  `given_name_jp` varchar(50) DEFAULT NULL,
  `given_name_kr` varchar(50) DEFAULT NULL,
  `origin_kr` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK702j4r2jvbyyhoqlr7t700vbw` (`external_api_id`),
  KEY `FKrba9pc1aqgd48522ngjftgn81` (`heya_id`),
  CONSTRAINT `FKrba9pc1aqgd48522ngjftgn81` FOREIGN KEY (`heya_id`) REFERENCES `heya` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rikishi_shikona_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `shikona_jp` varchar(100) NOT NULL,
  `shikona_kr` varchar(100) NOT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  `rikishi_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKajl1wvwus1i7n44lje0dpbvo8` (`rikishi_id`),
  CONSTRAINT `FKajl1wvwus1i7n44lje0dpbvo8` FOREIGN KEY (`rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `basho` (
  `id` int NOT NULL AUTO_INCREMENT,
  `basho_month` enum('JAN','JUL','MAR','MAY','NOV','SEP') NOT NULL,
  `basho_year` int NOT NULL,
  `end_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `external_basho_id` varchar(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_basho` (`basho_year`,`basho_month`),
  UNIQUE KEY `UKq6espsod1u83d1tpy9w6jsgp` (`external_basho_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `banzuke` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `division` enum('Jonidan','Jonokuchi','Juryo','Makushita','Makuuchi','Sandanme') NOT NULL,
  `rank_name` enum('Jonidan','Jonokuchi','Juryo','Komusubi','Maegashira','Makushita','Ozeki','Sandanme','Sekiwake','Yokozuna') NOT NULL,
  `rank_score` int DEFAULT NULL,
  `rank_value` int DEFAULT NULL,
  `side` enum('EAST','WEST') NOT NULL,
  `basho_id` int NOT NULL,
  `rikishi_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rikishi_basho` (`rikishi_id`,`basho_id`),
  KEY `FK6rfhsi9ms7pkydv6uc51wkj1y` (`basho_id`),
  CONSTRAINT `FK6rfhsi9ms7pkydv6uc51wkj1y` FOREIGN KEY (`basho_id`) REFERENCES `basho` (`id`),
  CONSTRAINT `FK8rxk0nswq7fsatlr771tulye` FOREIGN KEY (`rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `torikumi` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `day` int NOT NULL,
  `description_jp` varchar(255) DEFAULT NULL,
  `description_kr` varchar(255) DEFAULT NULL,
  `division` enum('Jonidan','Jonokuchi','Juryo','Makushita','Makuuchi','Sandanme') NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `is_extra_match` bit(1) NOT NULL,
  `kimarite` varchar(100) DEFAULT NULL,
  `match_no` int DEFAULT NULL,
  `result_type` enum('FUZEN','NORMAL') NOT NULL,
  `youtube_url` varchar(255) DEFAULT NULL,
  `basho_id` int NOT NULL,
  `east_rikishi_id` int NOT NULL,
  `loser_rikishi_id` int DEFAULT NULL,
  `west_rikishi_id` int NOT NULL,
  `winner_rikishi_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_torikumi` (`basho_id`,`day`,`east_rikishi_id`,`west_rikishi_id`,`is_extra_match`),
  UNIQUE KEY `UKk7wn7pyal0lj4vu9m0n3je7c4` (`external_id`),
  KEY `FKmkun3tn2jqcarhip42rgh2xi5` (`east_rikishi_id`),
  KEY `FKonot12j1a7gvkes6ic7s8vca8` (`loser_rikishi_id`),
  KEY `FKn62kalh2l3s62k2axol7h1608` (`west_rikishi_id`),
  KEY `FKqq6nsq1qfp4kbo3ieb806ay8r` (`winner_rikishi_id`),
  CONSTRAINT `FKcnaxp0vagixakx2e9prcrnk51` FOREIGN KEY (`basho_id`) REFERENCES `basho` (`id`),
  CONSTRAINT `FKmkun3tn2jqcarhip42rgh2xi5` FOREIGN KEY (`east_rikishi_id`) REFERENCES `rikishi` (`id`),
  CONSTRAINT `FKn62kalh2l3s62k2axol7h1608` FOREIGN KEY (`west_rikishi_id`) REFERENCES `rikishi` (`id`),
  CONSTRAINT `FKonot12j1a7gvkes6ic7s8vca8` FOREIGN KEY (`loser_rikishi_id`) REFERENCES `rikishi` (`id`),
  CONSTRAINT `FKqq6nsq1qfp4kbo3ieb806ay8r` FOREIGN KEY (`winner_rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `award` (
  `id` int NOT NULL AUTO_INCREMENT,
  `award_type` enum('SANSHO_GINO','SANSHO_KANTO','SANSHO_SHUKUN','YUSHO') NOT NULL,
  `division` enum('Jonidan','Jonokuchi','Juryo','Makushita','Makuuchi','Sandanme') NOT NULL,
  `basho_id` int NOT NULL,
  `rikishi_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_award` (`rikishi_id`,`basho_id`,`division`,`award_type`),
  KEY `FK57nwcug9gr1f8ai6k3iqh2wj6` (`basho_id`),
  CONSTRAINT `FK57nwcug9gr1f8ai6k3iqh2wj6` FOREIGN KEY (`basho_id`) REFERENCES `basho` (`id`),
  CONSTRAINT `FKermmty6hyl5q0dbx181dysrg8` FOREIGN KEY (`rikishi_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `kinboshi` (
  `id` int NOT NULL AUTO_INCREMENT,
  `loser_yokozuna_id` int NOT NULL,
  `torikumi_id` int NOT NULL,
  `winner_rikishi_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKigg0biherpesv4a1sclny0fje` (`loser_yokozuna_id`),
  KEY `FK5x7jx1gry5tw8xfd52b01ki6g` (`torikumi_id`),
  KEY `FKe6yeer5ssgqhry45ggci0rv3s` (`winner_rikishi_id`),
  CONSTRAINT `FK5x7jx1gry5tw8xfd52b01ki6g` FOREIGN KEY (`torikumi_id`) REFERENCES `torikumi` (`id`),
  CONSTRAINT `FKe6yeer5ssgqhry45ggci0rv3s` FOREIGN KEY (`winner_rikishi_id`) REFERENCES `rikishi` (`id`),
  CONSTRAINT `FKigg0biherpesv4a1sclny0fje` FOREIGN KEY (`loser_yokozuna_id`) REFERENCES `rikishi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` varchar(200) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `nickname` varchar(8) NOT NULL,
  `password` varchar(255) NOT NULL,
  `torikumi_id` int NOT NULL,
  `deleted_by` enum('ADMIN','USER') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmbm6vahiwq2gnlvtfjv4ndhyf` (`torikumi_id`),
  CONSTRAINT `FKmbm6vahiwq2gnlvtfjv4ndhyf` FOREIGN KEY (`torikumi_id`) REFERENCES `torikumi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
