-- 메인 화면 팝업 공지. 관리자 화면(/admin/notice)에서 쓰고 켜고 끈다. 켜진 공지 중 가장 최근 것 하나만 뜬다.
-- 본문은 HTML이 아니라 일반 텍스트(줄바꿈 유지). 일본어 문구가 비어 있으면 일본어 화면에서는 띄우지 않는다.
CREATE TABLE `notice` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title_ko` varchar(100) NOT NULL,
  `body_ko` varchar(1000) NOT NULL,
  `title_ja` varchar(100) DEFAULT NULL,
  `body_ja` varchar(1000) DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notice_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
