-- 댓글 신고: 방문자가 누른 신고 수와 마지막 신고 시각. 관리자 댓글 관리 화면에서 신고된 댓글을 모아 보는 데 쓴다.
-- 신고자 정보(IP 등)는 저장하지 않는다. 같은 사람의 중복 신고는 앱 메모리에서 막는다(RateLimiterService).
ALTER TABLE `comment`
  ADD COLUMN `report_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `last_reported_at` datetime(6) DEFAULT NULL;
