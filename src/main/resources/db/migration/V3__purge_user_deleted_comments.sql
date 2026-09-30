-- 본인이 삭제한 댓글의 원문을 지운다. 이전 버전은 "삭제"해도 닉네임·내용·비밀번호 해시를 그대로 보관했다.
-- 이제 CommentEntity.softDeleteByUser()가 삭제 시점에 비우므로, 그 전에 삭제된 댓글도 같은 상태로 맞춘다.
-- 관리자 블라인드(deleted_by='ADMIN')는 신고·조치 근거로 남겨둔다.
UPDATE `comment`
SET `nickname` = '', `content` = '', `password` = ''
WHERE `is_deleted` = 1 AND `deleted_by` = 'USER';
