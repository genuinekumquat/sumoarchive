-- 2026-10-26 이용자 제보로 본명·출신지·시코나 표기 수정 (external_api_id 기준이라 로컬/운영 공통).
-- 일본인은 일본어판 위키피디아, 몽골인은 키릴 문자 발음을 제보자가 확인한 값.

-- 본명
UPDATE rikishi SET name = '마츠이 카나토' WHERE external_api_id = 8858; -- 嵐富士
UPDATE rikishi SET name = '샤그다르수렝 달라이바타르' WHERE external_api_id = 8855; -- 旭海雄
UPDATE rikishi SET name = '스미키 겐타' WHERE external_api_id = 78; -- 風賢央
UPDATE rikishi SET name = '타츠 료야' WHERE external_api_id = 29; -- 輝 (기존 '타츠 아야야')
UPDATE rikishi SET name = '카요 야스토키' WHERE external_api_id = 142; -- 嘉陽 (기존 '카요 요시무네')
UPDATE rikishi SET name = '나바타메 타츠야' WHERE external_api_id = 117; -- 生田目
UPDATE rikishi SET name = '탄지 쥰' WHERE external_api_id = 287; -- 丹治
UPDATE rikishi SET name = '이하라 카즈나리' WHERE external_api_id = 34; -- 翠富士 (기존 '이오하라 카즈나리')
UPDATE rikishi SET name = '시라이시 마사히토' WHERE external_api_id = 54; -- 東白龍
UPDATE rikishi SET name = '바트체첵인 오치르사이한' WHERE external_api_id = 9103; -- 旭富士
UPDATE rikishi SET name = '요시이 코오' WHERE external_api_id = 140; -- 時不動
UPDATE rikishi SET name = '하츠야마 쇼' WHERE external_api_id = 77; -- 羽出山
UPDATE rikishi SET name = '니시무라 카즈마' WHERE external_api_id = 9099; -- 竜鳳

-- 출신지 원문. birthplace_manual = 1이라 로스터 동기화가 다시 덮어쓰지 않는다 (V9).
-- 狼雅: sumo-api 값 'Mongolia, Ulaanbaatar - Russia' → 러시아 투바 공화국 크즐
UPDATE rikishi SET birthplace = 'Russia, Tuva Republic, Kyzyl', birthplace_manual = b'1' WHERE external_api_id = 55;
-- 旭富士: sumo-api 값 'Mongolia' (도시 없음) → 울란바토르
UPDATE rikishi SET birthplace = 'Mongolia, Ulaanbaatar', birthplace_manual = b'1' WHERE external_api_id = 9103;

-- 한국어 시코나. 자동 음차(Nihonyanagi → 니호냐나기)였으므로 수동 검수값으로 표시한다.
UPDATE rikishi SET shikona_kr = '니혼야나기', shikona_kr_auto = b'0' WHERE external_api_id = 145; -- 二本柳
