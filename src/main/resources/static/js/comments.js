/* =====================================================================
 * 익명 댓글 위젯. 토리쿠미 전체 페이지와 리키시 프로필 슬라이드 패널에서 함께 쓴다.
 *
 * 서버 프래그먼트가 처음 목록을 그려주고, 등록/삭제가 일어나면 API가 "갱신된 전체 목록"을
 * 돌려주므로 이 스크립트는 그 배열로 <ul class="js-comment-list">만 다시 그린다.
 *
 * 슬라이드 패널은 innerHTML로 조각을 갈아끼우기 때문에 조각 안의 <script>는 실행되지 않는다.
 * 그래서 모든 처리는 여기(외부 파일)에 두고, 조각을 넣은 뒤 SumoComments.bind(container)를 부른다.
 * ===================================================================== */
(function () {
	'use strict';

	var endpoint = function (id) { return '/api/torikumi/' + id + '/comments'; };

	// 화면 문구 (한·일). 페이지 <html lang>으로 고른다 - 토리쿠미 페이지와 리키시 프로필(슬라이드 패널) 모두 th:lang을 쓴다.
	// 서버가 처음 그리는 같은 문구는 messages*.properties의 torikumi.comments.*에 있으니 바꿀 때 같이 바꾼다.
	// API 오류·신고 안내 문구는 서버가 요청 언어로 만들어 보내므로 여기엔 응답이 비었을 때의 대체 문구만 둔다.
	var TEXT = {
		ko: {
			empty: '아직 댓글이 없습니다. 첫 댓글을 남겨보세요.',
			deletedByUser: '작성자에 의해 삭제된 댓글입니다',
			blinded: '관리자에 의해 블라인드 처리된 댓글입니다',
			del: '삭제',
			report: '신고',
			pin: '비밀번호 4자리',
			ok: '확인',
			cancel: '취소',
			failed: '요청을 처리하지 못했습니다.',
			reportConfirm: '이 댓글을 신고할까요? 관리자가 확인 후 조치합니다.',
			reported: '신고가 접수되었습니다.'
		},
		ja: {
			empty: 'まだコメントはありません。最初のコメントを書いてみましょう。',
			deletedByUser: '投稿者により削除されたコメントです',
			blinded: '管理者により非表示にされたコメントです',
			del: '削除',
			report: '通報',
			pin: '暗証番号（数字4桁）',
			ok: '確認',
			cancel: 'キャンセル',
			failed: 'リクエストを処理できませんでした。',
			reportConfirm: 'このコメントを通報しますか？管理者が確認のうえ対応します。',
			reported: '通報を受け付けました。'
		}
	};
	var T = (document.documentElement.lang || '').indexOf('ja') === 0 ? TEXT.ja : TEXT.ko;

	function esc(s) {
		return String(s == null ? '' : s)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	function scopeOf(el) {
		return el.closest('.tk-comments') || el.closest('.tk') || document;
	}

	function renderList(listEl, comments) {
		if (!comments || comments.length === 0) {
			listEl.innerHTML = '<li class="tk-clist-empty">' + esc(T.empty) + '</li>';
			return;
		}
		listEl.innerHTML = comments.map(function (c) {
			var head =
				'<div class="tk-comment-head">' +
				'<span class="tk-comment-nick">' + (c.deleted ? '—' : esc(c.nickName)) + '</span>' +
				'<span class="tk-comment-date">' + esc(c.createdAt) + '</span>' +
				'</div>';
			// 삭제된 댓글 안내 문구는 서버 DTO(한국어) 대신 화면 언어로
			var text = !c.deleted ? c.displayContent : (c.blinded ? T.blinded : T.deletedByUser);
			var body = '<p class="tk-comment-body">' + esc(text) + '</p>';
			var actions = c.deleted
				? ''
				: '<div class="tk-comment-actions">' +
					'<button type="button" class="js-comment-delete tk-comment-del">' + esc(T.del) + '</button>' +
					'<button type="button" class="js-comment-report tk-comment-del">' + esc(T.report) + '</button>' +
				'</div>';
			return (
				'<li class="tk-comment' + (c.deleted ? ' is-deleted' : '') + '" data-comment-id="' + c.id + '">' +
				head + body + actions +
				'</li>'
			);
		}).join('');
	}

	function refresh(scope, comments) {
		var listEl = scope.querySelector('.js-comment-list');
		if (listEl) renderList(listEl, comments);
		var badge = scope.querySelector('.tk-comments-count');
		if (badge) badge.textContent = comments.length;
	}

	function postForm(url, data) {
		return fetch(url, {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
			body: new URLSearchParams(data).toString()
		}).then(function (res) {
			return res.json().catch(function () { return null; }).then(function (payload) {
				if (!res.ok) {
					var msg = payload && payload.message ? payload.message : T.failed;
					throw new Error(msg);
				}
				return payload;
			});
		});
	}

	function onSubmit(e) {
		var form = e.target.closest('.js-comment-form');
		if (!form) return;
		e.preventDefault();

		var id = form.getAttribute('data-torikumi-id');
		var errEl = form.querySelector('.js-comment-error');
		var btn = form.querySelector('[type="submit"]');
		if (errEl) { errEl.hidden = true; errEl.textContent = ''; }
		if (btn) btn.disabled = true;

		postForm(endpoint(id), {
			nickname: form.elements.nickname.value,
			password: form.elements.password.value,
			content: form.elements.content.value
		}).then(function (comments) {
			form.reset();
			refresh(scopeOf(form), comments);
		}).catch(function (err) {
			if (errEl) { errEl.textContent = err.message; errEl.hidden = false; }
		}).finally(function () {
			if (btn) btn.disabled = false;
		});
	}

	function openDeleteBox(li) {
		if (!li || li.querySelector('.tk-comment-delbox')) return;
		var box = document.createElement('div');
		box.className = 'tk-comment-delbox';
		box.innerHTML =
			'<input type="text" class="js-comment-del-pw tk-comment-delpw" inputmode="numeric" maxlength="4" placeholder="' + esc(T.pin) + '">' +
			'<button type="button" class="js-comment-del-confirm tk-comment-delok">' + esc(T.ok) + '</button>' +
			'<button type="button" class="js-comment-del-cancel tk-comment-delcancel">' + esc(T.cancel) + '</button>' +
			'<span class="js-comment-del-err tk-comment-delerr" hidden></span>';
		li.querySelector('.tk-comment-actions').appendChild(box);
		var pw = box.querySelector('.js-comment-del-pw');
		if (pw) pw.focus();
	}

	function submitDelete(li) {
		var listEl = li.closest('.js-comment-list');
		var id = listEl.getAttribute('data-torikumi-id');
		var commentId = li.getAttribute('data-comment-id');
		var pw = li.querySelector('.js-comment-del-pw');
		var errEl = li.querySelector('.js-comment-del-err');
		if (errEl) errEl.hidden = true;

		postForm(endpoint(id) + '/' + commentId + '/delete', { password: pw ? pw.value : '' })
			.then(function (comments) {
				refresh(scopeOf(li), comments);
			})
			.catch(function (err) {
				if (errEl) { errEl.textContent = err.message; errEl.hidden = false; }
			});
	}

	// 신고는 목록을 바꾸지 않으므로 버튼 자리에 결과 문구만 남긴다 (같은 댓글을 연달아 누르지 않게).
	function submitReport(li) {
		if (!li || !confirm(T.reportConfirm)) return;
		var id = li.closest('.js-comment-list').getAttribute('data-torikumi-id');
		var commentId = li.getAttribute('data-comment-id');
		var btn = li.querySelector('.js-comment-report');
		if (btn) btn.disabled = true;

		postForm(endpoint(id) + '/' + commentId + '/report', {})
			.then(function (payload) {
				showReportMessage(li, btn, payload && payload.message ? payload.message : T.reported);
			})
			.catch(function (err) {
				if (btn) btn.disabled = false;
				showReportMessage(li, null, err.message);
			});
	}

	function showReportMessage(li, btnToRemove, text) {
		var msg = li.querySelector('.tk-comment-reportmsg');
		if (!msg) {
			msg = document.createElement('span');
			msg.className = 'tk-comment-reportmsg';
			li.querySelector('.tk-comment-actions').appendChild(msg);
		}
		msg.textContent = text;
		if (btnToRemove) btnToRemove.remove();
	}

	function onClick(e) {
		if (e.target.closest('.js-comment-delete')) {
			openDeleteBox(e.target.closest('.tk-comment'));
		} else if (e.target.closest('.js-comment-report')) {
			submitReport(e.target.closest('.tk-comment'));
		} else if (e.target.closest('.js-comment-del-confirm')) {
			submitDelete(e.target.closest('.tk-comment'));
		} else if (e.target.closest('.js-comment-del-cancel')) {
			var box = e.target.closest('.tk-comment-delbox');
			if (box) box.remove();
		}
	}

	window.SumoComments = {
		// container: 조각이 들어간 요소(슬라이드 패널) 또는 document(전체 페이지).
		// 이벤트 위임이라 innerHTML 교체 후에도 다시 bind 할 필요는 없지만, 중복 bind는 방어한다.
		bind: function (container) {
			var root = container || document;
			if (root.__sumoCommentsBound) return;
			root.__sumoCommentsBound = true;
			root.addEventListener('submit', onSubmit);
			root.addEventListener('click', onClick);
		}
	};

	document.addEventListener('DOMContentLoaded', function () {
		if (document.querySelector('.js-comment-form')) {
			window.SumoComments.bind(document);
		}
	});
})();
