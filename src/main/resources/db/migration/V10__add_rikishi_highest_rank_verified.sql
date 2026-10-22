-- 2019년(기록 시작) 전 데뷔인데, 데뷔부터의 전체 계급 이력(sumo-api /api/ranks)과 대조해
-- 반즈케 계산 최고위가 실제 최고위와 같다고 확인된 리키시. 1이면 프로필의 「2019년~ 기록 기준」 안내를 뺀다.
-- (실제 최고위가 더 높았던 리키시는 highest_rank에 직접 입력해 두었다 — 2026-10-22)
ALTER TABLE `rikishi`
  ADD COLUMN `highest_rank_verified` bit(1) NOT NULL DEFAULT b'0';
