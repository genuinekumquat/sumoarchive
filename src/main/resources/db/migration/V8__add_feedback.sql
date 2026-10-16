-- 방문자가 운영자에게만 보내는 의견·오류 보고 (/feedback). 공개되지 않고 관리자 「의견함」(/admin/feedback)에서만 본다.
-- 보낸 사람의 IP는 저장하지 않는다(도배 방지는 RateLimiterService 메모리에서만). 연락처는 답장을 원할 때만 선택 입력.
CREATE TABLE `feedback` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category` enum('BUG','DATA','SUGGESTION','OTHER') NOT NULL,
  `content` varchar(1000) NOT NULL,
  `contact` varchar(100) DEFAULT NULL,
  `page_path` varchar(255) DEFAULT NULL,
  `lang` varchar(5) DEFAULT NULL,
  `handled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
