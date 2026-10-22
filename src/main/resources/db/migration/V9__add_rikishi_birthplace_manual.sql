-- 관리자가 출신지(원문)를 직접 고쳤는지 표시. 1이면 로스터 동기화(sumo-api)가 birthplace를 덮어쓰지 않는다.
-- 기존 행은 모두 0(자동) — 지금까지의 관리자 수정 여부는 기록이 없어 알 수 없다.
ALTER TABLE `rikishi`
  ADD COLUMN `birthplace_manual` bit(1) NOT NULL DEFAULT b'0';
