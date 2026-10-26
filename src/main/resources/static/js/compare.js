/* =====================================================================
 * 선수 비교(/compare) - 두 칸의 시코나 자동완성. 검색은 /api/search를 그대로 쓴다.
 * 고른 선수는 칸의 hidden 입력(a, b)에 id로 들고 있고, 두 칸이 다 차면 바로 비교 화면으로 이동한다.
 * ===================================================================== */
(function () {
	'use strict';

	var form = document.getElementById('compareForm');
	if (!form) {
		return;
	}
	var ja = document.documentElement.lang === 'ja';
	var noResult = form.getAttribute('data-no-result');
	var STATUS_JA = { '현역': '現役', '은퇴': '引退', '오야카타': '親方' };
	var DEBOUNCE_MS = 200;
	var MAX_RESULTS = 10;

	var slots = Array.prototype.map.call(form.querySelectorAll('.pick'), function (el) {
		return {
			el: el,
			hidden: el.querySelector('input[type=hidden]'),
			chosen: el.querySelector('.pick-chosen'),
			chosenName: el.querySelector('.pick-chosen-name'),
			input: el.querySelector('.pick-input'),
			results: el.querySelector('.pick-results'),
			timer: null,
			seq: 0,
			items: [],
			active: -1
		};
	});

	function other(slot) {
		return slots[0] === slot ? slots[1] : slots[0];
	}

	function displayName(r) {
		return ja ? (r.shikonaJp || r.shikonaKr) : (r.shikonaKr || r.shikonaJp);
	}

	function subLabel(r) {
		var heya = ja ? r.heyaNameJp : r.heyaName;
		var status = ja ? (STATUS_JA[r.statusLabel] || r.statusLabel) : r.statusLabel;
		return [heya, status].filter(Boolean).join(' · ');
	}

	function closeResults(slot) {
		slot.results.hidden = true;
		slot.results.innerHTML = '';
		slot.items = [];
		slot.active = -1;
	}

	function render(slot, list) {
		slot.results.innerHTML = '';
		slot.items = list;
		slot.active = -1;
		if (!list.length) {
			var empty = document.createElement('li');
			empty.className = 'r-empty';
			empty.textContent = noResult;
			slot.results.appendChild(empty);
		}
		list.forEach(function (r, i) {
			var li = document.createElement('li');
			var name = document.createElement('span');
			name.className = 'r-name';
			name.textContent = displayName(r);
			var sub = document.createElement('span');
			sub.className = 'r-sub';
			sub.textContent = subLabel(r);
			li.appendChild(name);
			li.appendChild(sub);
			// mousedown: input의 blur보다 먼저 와야 목록이 닫히기 전에 고를 수 있다
			li.addEventListener('mousedown', function (e) {
				e.preventDefault();
				choose(slot, slot.items[i]);
			});
			slot.results.appendChild(li);
		});
		slot.results.hidden = false;
	}

	function search(slot) {
		var q = slot.input.value.trim();
		if (!q) {
			closeResults(slot);
			return;
		}
		var seq = ++slot.seq;
		fetch('/api/search?keyword=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' } })
			.then(function (res) { return res.ok ? res.json() : { content: [] }; })
			.then(function (page) {
				if (seq !== slot.seq) {
					return; // 그 사이 더 새 입력이 있었다
				}
				var taken = other(slot).hidden.value;
				var list = (page.content || []).filter(function (r) {
					return String(r.rikishiId) !== taken;
				}).slice(0, MAX_RESULTS);
				render(slot, list);
			})
			.catch(function () { /* 검색 실패는 조용히 - 다시 입력하면 재시도 */ });
	}

	function choose(slot, r) {
		slot.hidden.value = r.rikishiId;
		slot.chosenName.textContent = displayName(r);
		slot.chosen.hidden = false;
		slot.input.hidden = true;
		slot.input.value = '';
		closeResults(slot);
		var o = other(slot);
		if (o.hidden.value) {
			form.submit();
		} else {
			o.input.focus();
		}
	}

	function highlight(slot, index) {
		var lis = slot.results.querySelectorAll('li');
		if (!slot.items.length) {
			return;
		}
		slot.active = (index + slot.items.length) % slot.items.length;
		Array.prototype.forEach.call(lis, function (li, i) {
			li.classList.toggle('is-active', i === slot.active);
		});
		lis[slot.active].scrollIntoView({ block: 'nearest' });
	}

	slots.forEach(function (slot) {
		slot.input.addEventListener('input', function () {
			clearTimeout(slot.timer);
			slot.timer = setTimeout(function () { search(slot); }, DEBOUNCE_MS);
		});
		slot.input.addEventListener('keydown', function (e) {
			// 한글 IME 조합 중의 Enter·방향키는 조합을 끝내는 키라 고르기로 받지 않는다
			if (e.isComposing || e.keyCode === 229) {
				return;
			}
			if (e.key === 'ArrowDown') {
				e.preventDefault();
				highlight(slot, slot.active + 1);
			} else if (e.key === 'ArrowUp') {
				e.preventDefault();
				highlight(slot, slot.active - 1);
			} else if (e.key === 'Enter') {
				// 목록에서 고른 게 있으면 그걸, 아니면 첫 결과를 고른다 (폼 제출은 막음)
				e.preventDefault();
				var pick = slot.items[slot.active >= 0 ? slot.active : 0];
				if (pick) {
					choose(slot, pick);
				}
			} else if (e.key === 'Escape') {
				closeResults(slot);
			}
		});
		slot.input.addEventListener('blur', function () {
			closeResults(slot);
		});
		slot.chosen.querySelector('.pick-change').addEventListener('click', function () {
			slot.hidden.value = '';
			slot.chosen.hidden = true;
			slot.input.hidden = false;
			slot.input.focus();
		});
	});

	// 비어 있는 칸은 주소에 남기지 않는다 (?a=1&b= 대신 ?a=1)
	form.addEventListener('submit', function (e) {
		var filled = slots.filter(function (s) { return s.hidden.value; });
		if (!filled.length) {
			e.preventDefault();
			slots[0].input.focus();
			return;
		}
		slots.forEach(function (s) { s.hidden.disabled = !s.hidden.value; });
	});
})();
