-- 현역 세키토리(2026년 11월 바쇼 마쿠우치·쥬료) 중 시코나가 바뀐 적 있는 36명의 옛 시코나 이력 42줄.
-- 기간은 sumo-api /api/shikonas 기준 [그 이름 첫 바쇼 달 1일, 다음 이름 첫 바쇼 전달 말일]. 뒷이름만 바뀐 건 제외.
-- 한자: sumo-api에 있으면 그 값, 없으면(대개 본명으로 출전하던 시절) 본명 한자·일본어판 위키피디아 改名歴으로 확인.
-- 한글: 로마자 음차를 읽기로 검수. 하위 디비전 리키시는 이번엔 채우지 않는다 (2026-10-28 결정).
-- 朝乃山 (Maegashira 1 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이시바시', '石橋', '2016-03-01', '2017-02-28', NOW() FROM rikishi r
WHERE r.external_api_id = 2
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '石橋');
-- 伯乃富士 (Maegashira 3 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '오치아이', '落合', '2023-01-01', '2023-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 3
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '落合');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '하쿠오호', '伯桜鵬', '2023-07-01', '2025-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 3
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '伯桜鵬');
-- 尊富士 (Maegashira 4 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이시오카', '石岡', '2022-09-01', '2022-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 5
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '石岡');
-- 霧島 (Ozeki 2 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '키리바야마', '霧馬山', '2015-03-01', '2023-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 7
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '霧馬山');
-- 琴勝峰 (Maegashira 6 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '코토테바카리', '琴手計', '2017-11-01', '2019-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 8
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '琴手計');
-- 大栄翔 (Maegashira 4 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타카니시', '高西', '2012-01-01', '2012-02-29', NOW() FROM rikishi r
WHERE r.external_api_id = 9
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '高西');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '다이쇼에이', '大翔栄', '2012-03-01', '2012-08-31', NOW() FROM rikishi r
WHERE r.external_api_id = 9
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '大翔栄');
-- 若元春 (Maegashira 7 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '아라오나미', '荒大波', '2011-11-01', '2011-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 13
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '荒大波');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '고시', '剛士', '2012-01-01', '2017-04-30', NOW() FROM rikishi r
WHERE r.external_api_id = 13
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '剛士');
-- 琴櫻 (Ozeki 1 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '코토카마타니', '琴鎌谷', '2015-11-01', '2019-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 20
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '琴鎌谷');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '코토노와카', '琴ノ若', '2019-07-01', '2024-04-30', NOW() FROM rikishi r
WHERE r.external_api_id = 20
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '琴ノ若');
-- 翔猿 (Juryo 2 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이와사키', '岩崎', '2015-01-01', '2017-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 21
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '岩崎');
-- 阿炎 (Maegashira 12 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '호리키리', '堀切', '2013-05-01', '2015-02-28', NOW() FROM rikishi r
WHERE r.external_api_id = 22
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '堀切');
-- 平戸海 (Maegashira 5 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '사카구치', '坂口', '2016-03-01', '2017-02-28', NOW() FROM rikishi r
WHERE r.external_api_id = 24
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '坂口');
-- 輝 (Juryo 3 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타츠', '達', '2010-03-01', '2014-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 29
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '達');
-- 翠富士 (Juryo 8 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이하라', '庵原', '2016-09-01', '2016-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 34
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '庵原');
-- 佐田の海 (Juryo 11 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '마츠무라', '松村', '2003-03-01', '2003-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 35
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '松村');
-- 隆の勝 (Maegashira 11 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '마스노쇼', '舛ノ勝', '2010-03-01', '2017-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 37
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '舛ノ勝');
-- 千代翔馬 (Juryo 1 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '쇼마', '翔馬', '2009-05-01', '2010-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 39
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '翔馬');
-- 錦富士 (Maegashira 8 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '오가사와라', '小笠原', '2016-09-01', '2016-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 40
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '小笠原');
-- 王鵬 (Maegashira 10 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '나야', '納谷', '2018-01-01', '2020-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 41
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '納谷');
-- 東白龍 (Juryo 8 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '시라이시', '白石', '2019-05-01', '2020-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 54
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '白石');
-- 豪ノ山 (Maegashira 5 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '니시카와', '西川', '2021-03-01', '2022-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 56
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '西川');
-- 美ノ海 (Komusubi 1 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '키자키', '木﨑', '2016-03-01', '2018-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 71
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '木﨑');
-- 時疾風 (Maegashira 14 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '토키사카에', '時栄', '2019-03-01', '2022-02-28', NOW() FROM rikishi r
WHERE r.external_api_id = 83
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '時栄');
-- 白熊 (Juryo 9 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타카하시', '高橋', '2022-05-01', '2023-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 98
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '高橋');
-- 玉正鳳 (Juryo 12 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타카카스가', '高春日', '2011-07-01', '2017-04-30', NOW() FROM rikishi r
WHERE r.external_api_id = 101
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '高春日');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '타네가시마', '種子島', '2017-05-01', '2017-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 101
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '種子島');
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '쿄쿠소텐', '旭蒼天', '2017-11-01', '2020-08-31', NOW() FROM rikishi r
WHERE r.external_api_id = 101
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '旭蒼天');
-- 友風 (Juryo 11 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '미나미', '南', '2017-05-01', '2017-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 102
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '南');
-- 日翔志 (Juryo 7 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '사와다쇼', '沢田翔', '2021-05-01', '2021-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 111
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '沢田翔');
-- 琴栄峰 (Maegashira 6 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '코토테바카리', '琴手計', '2022-01-01', '2024-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 112
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '琴手計');
-- 時不動 (Juryo 13 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '요시이', '吉井', '2019-03-01', '2026-04-30', NOW() FROM rikishi r
WHERE r.external_api_id = 140
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '吉井');
-- 朝紅龍 (Maegashira 9 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이시자키', '石崎', '2021-05-01', '2023-08-31', NOW() FROM rikishi r
WHERE r.external_api_id = 164
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '石崎');
-- 藤ノ川 (Sekiwake 1 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '와카이카리', '若碇', '2023-01-01', '2025-06-30', NOW() FROM rikishi r
WHERE r.external_api_id = 615
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '若碇');
-- 義ノ富士 (Komusubi 1 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '쿠사노', '草野', '2024-05-01', '2025-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 8857
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '草野');
-- 嵐富士 (Maegashira 16 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '마츠이', '松井', '2024-03-01', '2025-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 8858
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '松井');
-- 朝翠龍 (Maegashira 13 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '이시자키', '石崎', '2024-07-01', '2025-08-31', NOW() FROM rikishi r
WHERE r.external_api_id = 8859
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '石崎');
-- 寿之富士 (Maegashira 13 East)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '세이하쿠호', '聖白鵬', '2024-01-01', '2025-12-31', NOW() FROM rikishi r
WHERE r.external_api_id = 8866
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '聖白鵬');
-- 藤凌駕 (Maegashira 1 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '고시마', '五島', '2025-03-01', '2025-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 9051
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '五島');
-- 竜鳳 (Juryo 14 West)
INSERT INTO rikishi_shikona_history (rikishi_id, shikona_kr, shikona_jp, valid_from, valid_to, created_at)
SELECT r.id, '니시무라', '西村', '2025-09-01', '2025-10-31', NOW() FROM rikishi r
WHERE r.external_api_id = 9099
  AND NOT EXISTS (SELECT 1 FROM rikishi_shikona_history h WHERE h.rikishi_id = r.id AND h.shikona_jp = '西村');
