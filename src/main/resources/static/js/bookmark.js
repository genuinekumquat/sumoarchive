/* =====================================================================
 * 북마크(즐겨찾기) - LocalStorage에 리키시 ID 배열만 들고 있는 경량 구현.
 * 리키시 상세 페이지(★ 버튼)와 북마크 페이지(/bookmark, 목록+정렬)가 공유한다.
 * ===================================================================== */
(function () {
	'use strict';

	var STORAGE_KEY = 'sumoarchive_bookmarks';

	function getIds() {
		try {
			var raw = localStorage.getItem(STORAGE_KEY);
			var arr = raw ? JSON.parse(raw) : [];
			return Array.isArray(arr) ? arr.map(Number).filter(Number.isInteger) : [];
		} catch (e) {
			// 프라이빗 모드 등 localStorage 접근이 막힌 환경 - 조용히 빈 목록 취급
			return [];
		}
	}

	function setIds(ids) {
		try {
			localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
		} catch (e) {
			/* 저장 실패는 무시 - 이 세션에서만 반영 안 될 뿐 화면은 계속 동작 */
		}
	}

	function isBookmarked(rikishiId) {
		return getIds().indexOf(Number(rikishiId)) !== -1;
	}

	// 반환값: 방금 추가됐으면 true, 방금 제거됐으면 false
	function toggle(rikishiId) {
		var id = Number(rikishiId);
		var ids = getIds();
		var idx = ids.indexOf(id);
		if (idx >= 0) {
			ids.splice(idx, 1);
		} else {
			ids.push(id);
		}
		setIds(ids);
		return idx < 0;
	}

	window.SumoBookmark = { getIds: getIds, setIds: setIds, isBookmarked: isBookmarked, toggle: toggle };
})();
