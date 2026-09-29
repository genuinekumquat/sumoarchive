#!/usr/bin/env bash
# 로컬 DB의 데이터만(스키마 제외) 덤프해서 운영 DB로 옮길 때 쓴다.
# 스키마는 Flyway(V1__init_schema.sql)가 만들기 때문에 CREATE TABLE 없이 INSERT만 담는다.
#
# 사용법:
#   MYSQL_PWD=비밀번호 ./scripts/export-data.sh [출력파일]      (기본: sumo-data.sql)
#   DB_USER, DB_HOST, DB_NAME 환경변수로 바꿀 수 있음 (기본: root / localhost / sumo)
#
# 제외하는 테이블:
#   - flyway_schema_history : 운영 DB는 자기 이력을 따로 가진다
#   - comment               : 로컬에서 테스트로 단 댓글이 운영에 올라가지 않도록
set -euo pipefail

OUT="${1:-sumo-data.sql}"
DB_USER="${DB_USER:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_NAME="${DB_NAME:-sumo}"

mysqldump -h "$DB_HOST" -u "$DB_USER" \
  --no-create-info --skip-triggers --complete-insert \
  --single-transaction --default-character-set=utf8mb4 \
  --ignore-table="$DB_NAME.flyway_schema_history" \
  --ignore-table="$DB_NAME.comment" \
  "$DB_NAME" > "$OUT"

echo "덤프 완료: $OUT ($(wc -c < "$OUT") bytes)"
