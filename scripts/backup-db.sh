#!/usr/bin/env bash
# 운영 DB(docker compose의 sumoarchive-db 컨테이너)를 통째로 덤프해 gzip으로 보관한다.
# export-data.sh와 달리 스키마·flyway 이력·댓글까지 전부 담는 "복구용" 백업이다.
#
# 사용법 (서버, 프로젝트 루트에서):
#   ./scripts/backup-db.sh
#   BACKUP_DIR, KEEP_DAYS, DB_CONTAINER, DB_NAME 환경변수로 바꿀 수 있음
#   (기본: /var/backups/sumoarchive / 14일 / sumoarchive-db / sumo)
#
# cron 예시 (매일 새벽 4시, DEPLOYMENT.md 6절 참고):
#   0 4 * * * /home/ubuntu/sumoarchive/scripts/backup-db.sh >> /var/log/sumoarchive-backup.log 2>&1
#
# 비밀번호는 컨테이너에 이미 있는 MYSQL_ROOT_PASSWORD를 MYSQL_PWD로 넘긴다.
# -p<비밀번호>처럼 명령줄 인자로 넘기면 ps로 누구나 볼 수 있기 때문.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/sumoarchive}"
KEEP_DAYS="${KEEP_DAYS:-14}"
DB_CONTAINER="${DB_CONTAINER:-sumoarchive-db}"
DB_NAME="${DB_NAME:-sumo}"

mkdir -p "$BACKUP_DIR"
OUT="$BACKUP_DIR/sumo_$(date +%Y%m%d_%H%M%S).sql.gz"
TMP="$OUT.part"

# 덤프 도중 실패하면 반쪽짜리 파일이 정상 백업처럼 남지 않도록 .part로 쓰고 끝나면 이름을 바꾼다.
trap 'rm -f "$TMP"' EXIT

docker exec "$DB_CONTAINER" sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -u root \
     --single-transaction --routines --triggers \
     --default-character-set=utf8mb4 --databases "$1"' _ "$DB_NAME" \
  | gzip > "$TMP"

# 덤프가 끝까지 쓰였는지 확인 (mysqldump는 정상 종료 시 마지막 줄에 "Dump completed"를 남긴다)
if ! gzip -dc "$TMP" | tail -n 1 | grep -q "Dump completed"; then
  echo "[$(date '+%F %T')] 백업 실패: 덤프가 완료되지 않음" >&2
  exit 1
fi

mv "$TMP" "$OUT"
chmod 600 "$OUT"
echo "[$(date '+%F %T')] 백업 완료: $OUT ($(wc -c < "$OUT") bytes)"

# 보관 기간이 지난 백업 삭제
find "$BACKUP_DIR" -name 'sumo_*.sql.gz' -type f -mtime +"$KEEP_DAYS" -delete
