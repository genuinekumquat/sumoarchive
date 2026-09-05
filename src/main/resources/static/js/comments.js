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
			listEl.innerHTML = '<li class="tk-clist-empty">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</li>';
			return;
		}
		listEl.innerHTML = comments.map(function (c) {
			var head =
				'<div class="tk-comment-head">' +
				'<span class="tk-comment-nick">' + (c.deleted ? '—' : esc(c.nickName)) + '</span>' +
				'<span class="tk-comment-date">' + esc(c.createdAt) + '</span>' +
				'</div>';
			var body = '<p class="tk-comment-body">' + esc(c.displayContent) + '</p>';
			var actions = c.deleted
				? ''
				: '<div class="tk-comment-actions"><button type="button" class="js-comment-delete tk-comment-del">삭제</button></div>';
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
					var msg = payload && payload.message ? payload.message : '요청을 처리하지 못했습니다.';
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
			'<input type="text" class="js-comment-del-pw tk-comment-delpw" inputmode="numeric" maxlength="4" placeholder="비밀번호 4자리">' +
			'<button type="button" class="js-comment-del-confirm tk-comment-delok">확인</button>' +
			'<button type="button" class="js-comment-del-cancel tk-comment-delcancel">취소</button>' +
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

	function onClick(e) {
		if (e.target.closest('.js-comment-delete')) {
			openDeleteBox(e.target.closest('.tk-comment'));
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
