-- 2026년 11월 바쇼 개명 중 V15에서 빠진 하위 디비전 2명. 개명 감지가 들어가기 전 로스터 동기화가
-- 일본어·영문만 바꿔 놓아서 운영은 「타카노 / 弘騰龍」처럼 어긋나 있었다 (로컬은 새 동기화가 감지).
-- 한글은 읽기(こうとうりゅう, たいが)로 검수한 값. 기간은 sumo-api /api/shikonas 기준.
UPDATE rikishi SET shikona_kr = '코토류', shikona_kr_auto = b'0', shikona_jp = '弘騰龍', shikona_en = 'Kotoryu'
WHERE external_api_id = 381;
UPDATE rikishi SET shikona_kr = '타이가', shikona_kr_auto = b'0', shikona_jp = '泰我', shikona_en = 'Taiga'
WHERE external_api_id = 9147;

INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타카노', '高野', '2020-07-01', '2026-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 381
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '高野');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이노우에', '井上', '2026-09-01', '2026-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 9147
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '井上');
