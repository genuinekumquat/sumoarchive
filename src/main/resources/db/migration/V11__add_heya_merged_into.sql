-- 헤야 합치기(개명·흡수). sumo-api가 개명을 늦게/일부만 반영해서(예: 常盤山→湊川, 2026-10 기준 기존 선수는 아직 Tokiwayama)
-- 관리자가 "옛 헤야 → 새 헤야"로 합쳐 두면 소속 선수를 옮기고, 이후 로스터 임포트가 옛 영문명을 받아도 새 헤야로 넣는다.
ALTER TABLE `heya`
  ADD COLUMN `merged_into_heya_id` int DEFAULT NULL,
  ADD CONSTRAINT `fk_heya_merged_into` FOREIGN KEY (`merged_into_heya_id`) REFERENCES `heya` (`id`);
