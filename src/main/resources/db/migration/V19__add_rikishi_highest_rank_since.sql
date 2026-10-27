-- 직접 입력한 최고위(highest_rank)에 처음 오른 바쇼. 지금까지는 직접 입력값이면 프로필에 계급만 나오고 바쇼가 없었다.
-- 반즈케는 2019년부터라 그 전에 오른 리키시(예: 타카야스 오제키 2017년 7월)는 반즈케로 못 구하므로,
-- 2026-10-28 직접 입력값이 있던 276명 전원을 sumo-api 전체 계급 이력(/api/ranks)에서 그 지위(번호가 있으면 번호까지)에
-- 처음 오른 바쇼로 채운다. 관리자가 최고위를 바꾸면 이 값은 비워진다 (RikishiEntity.updateProfile).
ALTER TABLE `rikishi`
  ADD COLUMN `highest_rank_since` varchar(6) NULL;

UPDATE rikishi SET highest_rank_since = '202007' WHERE external_api_id = 2 AND highest_rank = 'Ozeki'; -- 아사노야마
UPDATE rikishi SET highest_rank_since = '202609' WHERE external_api_id = 3 AND highest_rank = 'Komusubi'; -- 하쿠노후지
UPDATE rikishi SET highest_rank_since = '202307' WHERE external_api_id = 7 AND highest_rank = 'Ozeki'; -- 키리시마
UPDATE rikishi SET highest_rank_since = '202605' WHERE external_api_id = 8 AND highest_rank = 'Sekiwake'; -- 코토쇼호
UPDATE rikishi SET highest_rank_since = '202009' WHERE external_api_id = 9 AND highest_rank = 'Sekiwake'; -- 다이에이쇼
UPDATE rikishi SET highest_rank_since = '202203' WHERE external_api_id = 12 AND highest_rank = 'Sekiwake'; -- 와카타카카게
UPDATE rikishi SET highest_rank_since = '202305' WHERE external_api_id = 13 AND highest_rank = 'Sekiwake'; -- 와카모토하루
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 14 AND highest_rank = 'Sekiwake'; -- 타마와시
UPDATE rikishi SET highest_rank_since = '202503' WHERE external_api_id = 19 AND highest_rank = 'Yokozuna'; -- 호쇼류
UPDATE rikishi SET highest_rank_since = '202403' WHERE external_api_id = 20 AND highest_rank = 'Ozeki'; -- 코토자쿠라
UPDATE rikishi SET highest_rank_since = '202203' WHERE external_api_id = 22 AND highest_rank = 'Sekiwake'; -- 아비
UPDATE rikishi SET highest_rank_since = '202401' WHERE external_api_id = 28 AND highest_rank = 'Komusubi'; -- 우라
UPDATE rikishi SET highest_rank_since = '202011' WHERE external_api_id = 33 AND highest_rank = 'Ozeki'; -- 쇼다이
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 35 AND highest_rank = 'Maegashira 1'; -- 사다노우미
UPDATE rikishi SET highest_rank_since = '202011' WHERE external_api_id = 37 AND highest_rank = 'Sekiwake'; -- 타카노쇼
UPDATE rikishi SET highest_rank_since = '202503' WHERE external_api_id = 41 AND highest_rank = 'Sekiwake'; -- 오호
UPDATE rikishi SET highest_rank_since = '201707' WHERE external_api_id = 44 AND highest_rank = 'Ozeki'; -- 타카야스
UPDATE rikishi SET highest_rank_since = '202507' WHERE external_api_id = 61 AND highest_rank = 'Komusubi'; -- 오쇼마
UPDATE rikishi SET highest_rank_since = '202605' WHERE external_api_id = 74 AND highest_rank = 'Sekiwake'; -- 아타미후지
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 96 AND highest_rank = 'Makushita 13'; -- 아키노야마
UPDATE rikishi SET highest_rank_since = '201603' WHERE external_api_id = 116 AND highest_rank = 'Makushita 12'; -- 토치노보리
UPDATE rikishi SET highest_rank_since = '201307' WHERE external_api_id = 121 AND highest_rank = 'Maegashira 4'; -- 후지아즈마
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 124 AND highest_rank = 'Maegashira 15'; -- 키타하리마
UPDATE rikishi SET highest_rank_since = '201201' WHERE external_api_id = 129 AND highest_rank = 'Maegashira 16'; -- 니오노우미
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 160 AND highest_rank = 'Makushita 25'; -- 오기노하마
UPDATE rikishi SET highest_rank_since = '200907' WHERE external_api_id = 203 AND highest_rank = 'Makushita 22'; -- 카츠노후지
UPDATE rikishi SET highest_rank_since = '200707' WHERE external_api_id = 218 AND highest_rank = 'Makushita 6'; -- 다이라이도
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 226 AND highest_rank = 'Makushita 23'; -- 코마노쿠니
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 298 AND highest_rank = 'Maegashira 13'; -- 아마카제
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 299 AND highest_rank = 'Sandanme 27'; -- 카야토이와
UPDATE rikishi SET highest_rank_since = '201003' WHERE external_api_id = 304 AND highest_rank = 'Makushita 15'; -- 히타치고
UPDATE rikishi SET highest_rank_since = '201309' WHERE external_api_id = 313 AND highest_rank = 'Makushita 8'; -- 아사텐마이
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 329 AND highest_rank = 'Sandanme 5'; -- 에비스마루
UPDATE rikishi SET highest_rank_since = '201201' WHERE external_api_id = 349 AND highest_rank = 'Maegashira 12'; -- 요시아즈마
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 350 AND highest_rank = 'Sandanme 4'; -- 츠가루우미
UPDATE rikishi SET highest_rank_since = '201407' WHERE external_api_id = 353 AND highest_rank = 'Makushita 5'; -- 무사시우미
UPDATE rikishi SET highest_rank_since = '201307' WHERE external_api_id = 359 AND highest_rank = 'Makushita 49'; -- 사다노쿠니
UPDATE rikishi SET highest_rank_since = '201105' WHERE external_api_id = 383 AND highest_rank = 'Sandanme 55'; -- 반주
UPDATE rikishi SET highest_rank_since = '201211' WHERE external_api_id = 388 AND highest_rank = 'Makushita 17'; -- 하가네
UPDATE rikishi SET highest_rank_since = '201603' WHERE external_api_id = 413 AND highest_rank = 'Makushita 37'; -- 토치하야테
UPDATE rikishi SET highest_rank_since = '200901' WHERE external_api_id = 417 AND highest_rank = 'Makushita 10'; -- 쇼케츠
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 419 AND highest_rank = 'Makushita 29'; -- 히엔리키
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 422 AND highest_rank = 'Sandanme 27'; -- 토치기다케
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 426 AND highest_rank = 'Sandanme 5'; -- 류세이야마
UPDATE rikishi SET highest_rank_since = '201203' WHERE external_api_id = 454 AND highest_rank = 'Sandanme 74'; -- 아다치
UPDATE rikishi SET highest_rank_since = '201709' WHERE external_api_id = 485 AND highest_rank = 'Jonidan 20'; -- 코쿠류나미
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 486 AND highest_rank = 'Jonidan 26'; -- 후지노테루
UPDATE rikishi SET highest_rank_since = '201511' WHERE external_api_id = 494 AND highest_rank = 'Sandanme 62'; -- 치요오우메
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 499 AND highest_rank = 'Sandanme 61'; -- 히로세
UPDATE rikishi SET highest_rank_since = '201501' WHERE external_api_id = 509 AND highest_rank = 'Sandanme 79'; -- 오쿠니야마
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 511 AND highest_rank = 'Sandanme 55'; -- 아즈마나미
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 524 AND highest_rank = 'Makushita 19'; -- 테니치
UPDATE rikishi SET highest_rank_since = '201501' WHERE external_api_id = 525 AND highest_rank = 'Sandanme 95'; -- 오쿠니
UPDATE rikishi SET highest_rank_since = '201511' WHERE external_api_id = 527 AND highest_rank = 'Sandanme 12'; -- 토치카스가
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 529 AND highest_rank = 'Sandanme 7'; -- 히고노류
UPDATE rikishi SET highest_rank_since = '200709' WHERE external_api_id = 542 AND highest_rank = 'Sandanme 79'; -- 테루노사토
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 544 AND highest_rank = 'Jonidan 38'; -- 우루토라
UPDATE rikishi SET highest_rank_since = '201707' WHERE external_api_id = 558 AND highest_rank = 'Jonidan 65'; -- 쿄노사토
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 573 AND highest_rank = 'Sandanme 74'; -- 모토아즈마
UPDATE rikishi SET highest_rank_since = '201303' WHERE external_api_id = 594 AND highest_rank = 'Jonidan 56'; -- 모리우라라
UPDATE rikishi SET highest_rank_since = '201203' WHERE external_api_id = 595 AND highest_rank = 'Jonidan 19'; -- 이타코자쿠라
UPDATE rikishi SET highest_rank_since = '202609' WHERE external_api_id = 615 AND highest_rank = 'Sekiwake'; -- 후지노카와
UPDATE rikishi SET highest_rank_since = '202507' WHERE external_api_id = 8850 AND highest_rank = 'Yokozuna'; -- 오노사토
UPDATE rikishi SET highest_rank_since = '202601' WHERE external_api_id = 8854 AND highest_rank = 'Ozeki'; -- 아오니시키
UPDATE rikishi SET highest_rank_since = '202607' WHERE external_api_id = 8857 AND highest_rank = 'Komusubi'; -- 요시노후지
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 25 AND highest_rank = 'Sekiwake'; -- 타카라후지
UPDATE rikishi SET highest_rank_since = '201805' WHERE external_api_id = 64 AND highest_rank = 'Maegashira 11'; -- 다이아마미
UPDATE rikishi SET highest_rank_since = '201209' WHERE external_api_id = 36 AND highest_rank = 'Sekiwake'; -- 묘기류
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 10 AND highest_rank = 'Komusubi'; -- 오노쇼
UPDATE rikishi SET highest_rank_since = '201411' WHERE external_api_id = 23 AND highest_rank = 'Sekiwake'; -- 아오이야마
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 42 AND highest_rank = 'Maegashira 5'; -- 치요마루
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 168 AND highest_rank = 'Maegashira 5'; -- 다이쇼마루
UPDATE rikishi SET highest_rank_since = '201503' WHERE external_api_id = 47 AND highest_rank = 'Sekiwake'; -- 오키노우미
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 51 AND highest_rank = 'Maegashira 1'; -- 치요노쿠니
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 637 AND highest_rank = 'Komusubi'; -- 치요타이류
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 674 AND highest_rank = 'Sekiwake'; -- 카이세이
UPDATE rikishi SET highest_rank_since = '201301' WHERE external_api_id = 673 AND highest_rank = 'Komusubi'; -- 쇼호잔
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 105 AND highest_rank = 'Juryo 10'; -- 치요아라시
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 671 AND highest_rank = 'Komusubi'; -- 조코류
UPDATE rikishi SET highest_rank_since = '201601' WHERE external_api_id = 148 AND highest_rank = 'Makushita 2'; -- 카이류
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 138 AND highest_rank = 'Juryo 12'; -- 다이세이류
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 377 AND highest_rank = 'Maegashira 8'; -- 쿄쿠타이세이
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3120 AND highest_rank = 'Sekiwake'; -- 이키오이
UPDATE rikishi SET highest_rank_since = '201601' WHERE external_api_id = 672 AND highest_rank = 'Maegashira 4'; -- 쿄쿠슈호
UPDATE rikishi SET highest_rank_since = '201405' WHERE external_api_id = 670 AND highest_rank = 'Komusubi'; -- 치요오토리
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3158 AND highest_rank = 'Sekiwake'; -- 코토유키
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 669 AND highest_rank = 'Juryo 10'; -- 사키가케
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 153 AND highest_rank = 'Makushita 4'; -- 류세이
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 3142 AND highest_rank = 'Ozeki'; -- 코토쇼기쿠
UPDATE rikishi SET highest_rank_since = '201009' WHERE external_api_id = 3195 AND highest_rank = 'Sekiwake'; -- 토치오잔
UPDATE rikishi SET highest_rank_since = '201703' WHERE external_api_id = 3223 AND highest_rank = 'Maegashira 2'; -- 소코쿠라이
UPDATE rikishi SET highest_rank_since = '200809' WHERE external_api_id = 3204 AND highest_rank = 'Sekiwake'; -- 토요노시마
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 198 AND highest_rank = 'Juryo 7'; -- 아사벤케이
UPDATE rikishi SET highest_rank_since = '201805' WHERE external_api_id = 3144 AND highest_rank = 'Makushita 16'; -- 히사노토라
UPDATE rikishi SET highest_rank_since = '201703' WHERE external_api_id = 3255 AND highest_rank = 'Maegashira 2'; -- 타카노이와
UPDATE rikishi SET highest_rank_since = '201601' WHERE external_api_id = 3248 AND highest_rank = 'Sekiwake'; -- 요시카제
UPDATE rikishi SET highest_rank_since = '200709' WHERE external_api_id = 3247 AND highest_rank = 'Sekiwake'; -- 아미니시키
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 3246 AND highest_rank = 'Maegashira 2'; -- 아라와시
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 3273 AND highest_rank = 'Sekiwake'; -- 타케카제
UPDATE rikishi SET highest_rank_since = '201203' WHERE external_api_id = 3222 AND highest_rank = 'Komusubi'; -- 가가마루
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3221 AND highest_rank = 'Maegashira 14'; -- 세이로
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 3114 AND highest_rank = 'Makushita 12'; -- 카라츠우미
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 114 AND highest_rank = 'Makushita 7'; -- 와카타카모토
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 363 AND highest_rank = 'Makushita 40'; -- 류세이오
UPDATE rikishi SET highest_rank_since = '201305' WHERE external_api_id = 549 AND highest_rank = 'Sandanme 5'; -- 토치노시마
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 173 AND highest_rank = 'Makushita 13'; -- 센가쿠
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 410 AND highest_rank = 'Sandanme 85'; -- 아마미다케
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 343 AND highest_rank = 'Makushita 38'; -- 카오루후지
UPDATE rikishi SET highest_rank_since = '201203' WHERE external_api_id = 571 AND highest_rank = 'Makushita 59'; -- 켄호
UPDATE rikishi SET highest_rank_since = '201211' WHERE external_api_id = 604 AND highest_rank = 'Jonidan 57'; -- 히고히카리
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 590 AND highest_rank = 'Sandanme 58'; -- 츠바키후지
UPDATE rikishi SET highest_rank_since = '200511' WHERE external_api_id = 460 AND highest_rank = 'Sandanme 26'; -- 사토노후지
UPDATE rikishi SET highest_rank_since = '201407' WHERE external_api_id = 602 AND highest_rank = 'Jonidan 53'; -- 사와이사무
UPDATE rikishi SET highest_rank_since = '201407' WHERE external_api_id = 266 AND highest_rank = 'Makushita 49'; -- 아오이
UPDATE rikishi SET highest_rank_since = '201211' WHERE external_api_id = 472 AND highest_rank = 'Makushita 34'; -- 코시노류
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 474 AND highest_rank = 'Sandanme 92'; -- 호쿠토나미
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 295 AND highest_rank = 'Makushita 53'; -- 마츠다
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 303 AND highest_rank = 'Makushita 58'; -- 바라키
UPDATE rikishi SET highest_rank_since = '201207' WHERE external_api_id = 327 AND highest_rank = 'Makushita 3'; -- 쇼세이
UPDATE rikishi SET highest_rank_since = '201209' WHERE external_api_id = 246 AND highest_rank = 'Juryo 11'; -- 케이텐카이
UPDATE rikishi SET highest_rank_since = '201801' WHERE external_api_id = 449 AND highest_rank = 'Sandanme 93'; -- 쿠로카게
UPDATE rikishi SET highest_rank_since = '200901' WHERE external_api_id = 464 AND highest_rank = 'Sandanme 92'; -- 모가미니시키
UPDATE rikishi SET highest_rank_since = '201305' WHERE external_api_id = 495 AND highest_rank = 'Jonidan 5'; -- 사쿠라
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 572 AND highest_rank = 'Jonidan 20'; -- 키리마루
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 289 AND highest_rank = 'Makushita 37'; -- 라오
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 551 AND highest_rank = 'Sandanme 66'; -- 하타치조
UPDATE rikishi SET highest_rank_since = '200603' WHERE external_api_id = 553 AND highest_rank = 'Sandanme 47'; -- 신잔
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 491 AND highest_rank = 'Makushita 53'; -- 마에노후지
UPDATE rikishi SET highest_rank_since = '200809' WHERE external_api_id = 319 AND highest_rank = 'Makushita 21'; -- 아사노토사
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 305 AND highest_rank = 'Makushita 43'; -- 다이조
UPDATE rikishi SET highest_rank_since = '201105' WHERE external_api_id = 443 AND highest_rank = 'Sandanme 69'; -- 키리자쿠라
UPDATE rikishi SET highest_rank_since = '201405' WHERE external_api_id = 521 AND highest_rank = 'Sandanme 40'; -- 키류
UPDATE rikishi SET highest_rank_since = '201501' WHERE external_api_id = 502 AND highest_rank = 'Sandanme 98'; -- 아사노시마
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 294 AND highest_rank = 'Makushita 6'; -- 아오조라
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 503 AND highest_rank = 'Jonidan 21'; -- 야마나
UPDATE rikishi SET highest_rank_since = '201209' WHERE external_api_id = 526 AND highest_rank = 'Makushita 29'; -- 덴잔
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 325 AND highest_rank = 'Sandanme 65'; -- 메이코
UPDATE rikishi SET highest_rank_since = '201109' WHERE external_api_id = 568 AND highest_rank = 'Sandanme 60'; -- 코토묘잔
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 389 AND highest_rank = 'Sandanme 10'; -- 아카츠키
UPDATE rikishi SET highest_rank_since = '200205' WHERE external_api_id = 483 AND highest_rank = 'Makushita 53'; -- 카이노와카
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 606 AND highest_rank = 'Jonidan 18'; -- 미호노우미
UPDATE rikishi SET highest_rank_since = '201505' WHERE external_api_id = 632 AND highest_rank = 'Makushita 25'; -- 겐카이호
UPDATE rikishi SET highest_rank_since = '201101' WHERE external_api_id = 652 AND highest_rank = 'Sandanme 30'; -- 카쿠타이키
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 641 AND highest_rank = 'Jonidan 17'; -- 다이쇼하마
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 686 AND highest_rank = 'Makushita 53'; -- 쿄쿠호잔
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 619 AND highest_rank = 'Sandanme 25'; -- 테라오쇼
UPDATE rikishi SET highest_rank_since = '201009' WHERE external_api_id = 659 AND highest_rank = 'Makushita 17'; -- 고리키야마
UPDATE rikishi SET highest_rank_since = '201703' WHERE external_api_id = 643 AND highest_rank = 'Jonidan 7'; -- 코토타이코
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 703 AND highest_rank = 'Makushita 5'; -- 무겐다이
UPDATE rikishi SET highest_rank_since = '201811' WHERE external_api_id = 689 AND highest_rank = 'Juryo 13'; -- 고쿠신도
UPDATE rikishi SET highest_rank_since = '201601' WHERE external_api_id = 666 AND highest_rank = 'Makushita 35'; -- 아즈마사토
UPDATE rikishi SET highest_rank_since = '201307' WHERE external_api_id = 685 AND highest_rank = 'Makushita 27'; -- 아마츠
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 694 AND highest_rank = 'Sandanme 85'; -- 젠다이쇼
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 693 AND highest_rank = 'Sandanme 45'; -- 아리아케
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 687 AND highest_rank = 'Sandanme 3'; -- 스마노우미
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 662 AND highest_rank = 'Makushita 25'; -- 타마콘고
UPDATE rikishi SET highest_rank_since = '201307' WHERE external_api_id = 676 AND highest_rank = 'Sandanme 67'; -- 다이쇼조
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 3077 AND highest_rank = 'Makushita 12'; -- 난카이리키
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 668 AND highest_rank = 'Makushita 25'; -- 아사키신
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 701 AND highest_rank = 'Sandanme 18'; -- 키리츠바사
UPDATE rikishi SET highest_rank_since = '201109' WHERE external_api_id = 684 AND highest_rank = 'Makushita 29'; -- 카스가류
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 655 AND highest_rank = 'Sandanme 40'; -- 미에노사토
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 649 AND highest_rank = 'Sandanme 76'; -- 아리카와
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 642 AND highest_rank = 'Makushita 60'; -- 토호
UPDATE rikishi SET highest_rank_since = '201505' WHERE external_api_id = 675 AND highest_rank = 'Sandanme 77'; -- 이시하라
UPDATE rikishi SET highest_rank_since = '200411' WHERE external_api_id = 679 AND highest_rank = 'Makushita 19'; -- 후지노카제
UPDATE rikishi SET highest_rank_since = '199803' WHERE external_api_id = 644 AND highest_rank = 'Sandanme 56'; -- 하나카제
UPDATE rikishi SET highest_rank_since = '201403' WHERE external_api_id = 688 AND highest_rank = 'Juryo 9'; -- 히고노조
UPDATE rikishi SET highest_rank_since = '201403' WHERE external_api_id = 697 AND highest_rank = 'Makushita 2'; -- 히고아라시
UPDATE rikishi SET highest_rank_since = '201707' WHERE external_api_id = 3130 AND highest_rank = 'Makushita 8'; -- 겐키
UPDATE rikishi SET highest_rank_since = '201109' WHERE external_api_id = 3076 AND highest_rank = 'Maegashira 9'; -- 사가츠카사
UPDATE rikishi SET highest_rank_since = '201801' WHERE external_api_id = 3082 AND highest_rank = 'Jonidan 13'; -- 코토코교쿠
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 3096 AND highest_rank = 'Jonidan 53'; -- 아즈마히카리
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 3075 AND highest_rank = 'Sandanme 32'; -- 무츠카제
UPDATE rikishi SET highest_rank_since = '201211' WHERE external_api_id = 3091 AND highest_rank = 'Maegashira 11'; -- 아사히쇼
UPDATE rikishi SET highest_rank_since = '200811' WHERE external_api_id = 3140 AND highest_rank = 'Maegashira 2'; -- 토요히비키
UPDATE rikishi SET highest_rank_since = '201505' WHERE external_api_id = 3123 AND highest_rank = 'Sandanme 4'; -- 다이쇼와카
UPDATE rikishi SET highest_rank_since = '201611' WHERE external_api_id = 3125 AND highest_rank = 'Jonidan 43'; -- 고케츠야마
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 627 AND highest_rank = 'Maegashira 9'; -- 카가미오
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 3117 AND highest_rank = 'Juryo 13'; -- 타카류
UPDATE rikishi SET highest_rank_since = '201407' WHERE external_api_id = 3129 AND highest_rank = 'Makushita 1'; -- 호류야마
UPDATE rikishi SET highest_rank_since = '201805' WHERE external_api_id = 3089 AND highest_rank = 'Makushita 9'; -- 다이쇼류
UPDATE rikishi SET highest_rank_since = '201211' WHERE external_api_id = 3175 AND highest_rank = 'Maegashira 4'; -- 마스노야마
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 3108 AND highest_rank = 'Sandanme 39'; -- 코토젠쇼
UPDATE rikishi SET highest_rank_since = '201603' WHERE external_api_id = 3122 AND highest_rank = 'Makushita 44'; -- 에츠노하나
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 3127 AND highest_rank = 'Makushita 28'; -- 코토키노
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 3126 AND highest_rank = 'Sandanme 72'; -- 요시무라
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 3124 AND highest_rank = 'Jonidan 58'; -- 다이시류
UPDATE rikishi SET highest_rank_since = '201107' WHERE external_api_id = 3133 AND highest_rank = 'Juryo 6'; -- 카오니시키
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 3102 AND highest_rank = 'Sandanme 66'; -- 아야카제
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 3104 AND highest_rank = 'Jonidan 11'; -- 소라카제
UPDATE rikishi SET highest_rank_since = '201811' WHERE external_api_id = 3194 AND highest_rank = 'Makushita 6'; -- 토미사카에
UPDATE rikishi SET highest_rank_since = '201403' WHERE external_api_id = 3134 AND highest_rank = 'Sandanme 1'; -- 다이쇼카쿠
UPDATE rikishi SET highest_rank_since = '201209' WHERE external_api_id = 3095 AND highest_rank = 'Makushita 20'; -- 오타
UPDATE rikishi SET highest_rank_since = '201801' WHERE external_api_id = 3155 AND highest_rank = 'Makushita 51'; -- 츠유쿠사
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 3136 AND highest_rank = 'Makushita 2'; -- 키주인
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 3174 AND highest_rank = 'Makushita 53'; -- 타카미류
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 3167 AND highest_rank = 'Sandanme 100'; -- 호쿠토류
UPDATE rikishi SET highest_rank_since = '201509' WHERE external_api_id = 3150 AND highest_rank = 'Sandanme 89'; -- 코토오무라
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 3113 AND highest_rank = 'Makushita 2'; -- 키호노우미
UPDATE rikishi SET highest_rank_since = '201107' WHERE external_api_id = 3152 AND highest_rank = 'Juryo 13'; -- 히텐류
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 3153 AND highest_rank = 'Makushita 49'; -- 하루히카리
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 3163 AND highest_rank = 'Sandanme 80'; -- 코토야마토
UPDATE rikishi SET highest_rank_since = '201801' WHERE external_api_id = 3197 AND highest_rank = 'Sandanme 83'; -- 타이가
UPDATE rikishi SET highest_rank_since = '200507' WHERE external_api_id = 3205 AND highest_rank = 'Sandanme 83'; -- 다이고니시키
UPDATE rikishi SET highest_rank_since = '201305' WHERE external_api_id = 3176 AND highest_rank = 'Juryo 11'; -- 키젠류
UPDATE rikishi SET highest_rank_since = '201403' WHERE external_api_id = 3203 AND highest_rank = 'Makushita 10'; -- 테라오
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 3220 AND highest_rank = 'Makushita 7'; -- 고초잔
UPDATE rikishi SET highest_rank_since = '201611' WHERE external_api_id = 3191 AND highest_rank = 'Sandanme 3'; -- 아라토라
UPDATE rikishi SET highest_rank_since = '201303' WHERE external_api_id = 3169 AND highest_rank = 'Makushita 33'; -- 코마키류
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 3190 AND highest_rank = 'Makushita 12'; -- 반코쿠
UPDATE rikishi SET highest_rank_since = '201603' WHERE external_api_id = 3179 AND highest_rank = 'Sandanme 30'; -- 코토리쿠잔
UPDATE rikishi SET highest_rank_since = '201309' WHERE external_api_id = 3193 AND highest_rank = 'Juryo 6'; -- 토쿠신호
UPDATE rikishi SET highest_rank_since = '201105' WHERE external_api_id = 3170 AND highest_rank = 'Makushita 13'; -- 뱟코
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 3189 AND highest_rank = 'Sandanme 11'; -- 쇼부시
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 3201 AND highest_rank = 'Sandanme 72'; -- 사카에후지
UPDATE rikishi SET highest_rank_since = '201303' WHERE external_api_id = 3200 AND highest_rank = 'Sandanme 23'; -- 카스가미네
UPDATE rikishi SET highest_rank_since = '201607' WHERE external_api_id = 3224 AND highest_rank = 'Jonidan 46'; -- 테루주
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 3209 AND highest_rank = 'Sandanme 86'; -- 코토노시마
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 3210 AND highest_rank = 'Jonidan 66'; -- 테츠비쇼
UPDATE rikishi SET highest_rank_since = '200611' WHERE external_api_id = 3207 AND highest_rank = 'Sandanme 42'; -- 바리키
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3217 AND highest_rank = 'Sandanme 9'; -- 타카텐슈
UPDATE rikishi SET highest_rank_since = '201209' WHERE external_api_id = 3214 AND highest_rank = 'Makushita 17'; -- 코토후쿠주노
UPDATE rikishi SET highest_rank_since = '201309' WHERE external_api_id = 3218 AND highest_rank = 'Makushita 38'; -- 하마에이코
UPDATE rikishi SET highest_rank_since = '201505' WHERE external_api_id = 3227 AND highest_rank = 'Sandanme 20'; -- 나루미
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 3228 AND highest_rank = 'Sandanme 19'; -- 카이슈
UPDATE rikishi SET highest_rank_since = '201203' WHERE external_api_id = 3216 AND highest_rank = 'Makushita 29'; -- 렌다이야마
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 3212 AND highest_rank = 'Sandanme 48'; -- 와카코키
UPDATE rikishi SET highest_rank_since = '201601' WHERE external_api_id = 3226 AND highest_rank = 'Jonidan 28'; -- 호쿠토유
UPDATE rikishi SET highest_rank_since = '201801' WHERE external_api_id = 3240 AND highest_rank = 'Sandanme 26'; -- 후쿠노후지
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 3213 AND highest_rank = 'Sandanme 7'; -- 아와지우미
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 3234 AND highest_rank = 'Jonidan 44'; -- 라이가
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 3208 AND highest_rank = 'Jonidan 60'; -- 시마카제
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 3244 AND highest_rank = 'Makushita 3'; -- 이리에
UPDATE rikishi SET highest_rank_since = '201511' WHERE external_api_id = 3252 AND highest_rank = 'Maegashira 6'; -- 호마레후지
UPDATE rikishi SET highest_rank_since = '201805' WHERE external_api_id = 3278 AND highest_rank = 'Sandanme 63'; -- 아카시류
UPDATE rikishi SET highest_rank_since = '201305' WHERE external_api_id = 3250 AND highest_rank = 'Maegashira 16'; -- 다이키호
UPDATE rikishi SET highest_rank_since = '201609' WHERE external_api_id = 3298 AND highest_rank = 'Makushita 26'; -- 무사시쿠니
UPDATE rikishi SET highest_rank_since = '200903' WHERE external_api_id = 3253 AND highest_rank = 'Makushita 1'; -- 카스가쿠니
UPDATE rikishi SET highest_rank_since = '201811' WHERE external_api_id = 3303 AND highest_rank = 'Sandanme 50'; -- 아마츠카사
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 3236 AND highest_rank = 'Jonidan 11'; -- 와카사타케
UPDATE rikishi SET highest_rank_since = '201811' WHERE external_api_id = 3286 AND highest_rank = 'Jonidan 61'; -- 코토스미다
UPDATE rikishi SET highest_rank_since = '201805' WHERE external_api_id = 3238 AND highest_rank = 'Sandanme 50'; -- 하오
UPDATE rikishi SET highest_rank_since = '201707' WHERE external_api_id = 3267 AND highest_rank = 'Sandanme 32'; -- 오키노이와
UPDATE rikishi SET highest_rank_since = '201305' WHERE external_api_id = 3281 AND highest_rank = 'Juryo 7'; -- 토치히류
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3283 AND highest_rank = 'Juryo 9'; -- 데와하야테
UPDATE rikishi SET highest_rank_since = '201507' WHERE external_api_id = 3241 AND highest_rank = 'Makushita 25'; -- 소라노우미
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 3237 AND highest_rank = 'Sandanme 37'; -- 호쿠토신
UPDATE rikishi SET highest_rank_since = '201603' WHERE external_api_id = 3276 AND highest_rank = 'Sandanme 15'; -- 하마미류
UPDATE rikishi SET highest_rank_since = '201411' WHERE external_api_id = 3296 AND highest_rank = 'Makushita 18'; -- 이케루
UPDATE rikishi SET highest_rank_since = '201705' WHERE external_api_id = 3262 AND highest_rank = 'Makushita 22'; -- 슌바
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 3307 AND highest_rank = 'Makushita 5'; -- 후쿠고리키
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3284 AND highest_rank = 'Makushita 6'; -- 아키노하나
UPDATE rikishi SET highest_rank_since = '201409' WHERE external_api_id = 3243 AND highest_rank = 'Makushita 11'; -- 다이쇼이와
UPDATE rikishi SET highest_rank_since = '201311' WHERE external_api_id = 3282 AND highest_rank = 'Maegashira 8'; -- 텐카이호
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3261 AND highest_rank = 'Sandanme 20'; -- 아마니시키
UPDATE rikishi SET highest_rank_since = '201205' WHERE external_api_id = 3293 AND highest_rank = 'Sandanme 20'; -- 호쿠토츠루
UPDATE rikishi SET highest_rank_since = '201407' WHERE external_api_id = 3277 AND highest_rank = 'Makushita 58'; -- 키리아라시
UPDATE rikishi SET highest_rank_since = '201111' WHERE external_api_id = 3321 AND highest_rank = 'Sandanme 4'; -- 부리노사토
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 3274 AND highest_rank = 'Jonidan 29'; -- 나가야
UPDATE rikishi SET highest_rank_since = '201809' WHERE external_api_id = 3291 AND highest_rank = 'Sandanme 93'; -- 스미다가와
UPDATE rikishi SET highest_rank_since = '201303' WHERE external_api_id = 3288 AND highest_rank = 'Jonidan 44'; -- 하마텐세이
UPDATE rikishi SET highest_rank_since = '201401' WHERE external_api_id = 3308 AND highest_rank = 'Makushita 3'; -- 칸세이
UPDATE rikishi SET highest_rank_since = '201703' WHERE external_api_id = 3309 AND highest_rank = 'Makushita 23'; -- 아사히류
UPDATE rikishi SET highest_rank_since = '201711' WHERE external_api_id = 3269 AND highest_rank = 'Sandanme 6'; -- 사이오
UPDATE rikishi SET highest_rank_since = '201707' WHERE external_api_id = 3280 AND highest_rank = 'Makushita 42'; -- 토치미나토
UPDATE rikishi SET highest_rank_since = '201207' WHERE external_api_id = 3295 AND highest_rank = 'Makushita 40'; -- 코토오토리
UPDATE rikishi SET highest_rank_since = '201511' WHERE external_api_id = 3271 AND highest_rank = 'Makushita 21'; -- 사사야마
UPDATE rikishi SET highest_rank_since = '201709' WHERE external_api_id = 3275 AND highest_rank = 'Sandanme 14'; -- 호쿠토하나
UPDATE rikishi SET highest_rank_since = '200211' WHERE external_api_id = 3290 AND highest_rank = 'Makushita 34'; -- 이세노하나
UPDATE rikishi SET highest_rank_since = '200805' WHERE external_api_id = 3259 AND highest_rank = 'Sandanme 7'; -- 다이쇼코
UPDATE rikishi SET highest_rank_since = '201807' WHERE external_api_id = 3260 AND highest_rank = 'Jonidan 47'; -- 타츠노후지
UPDATE rikishi SET highest_rank_since = '201403' WHERE external_api_id = 3311 AND highest_rank = 'Jonidan 41'; -- 다이잇신
UPDATE rikishi SET highest_rank_since = '201605' WHERE external_api_id = 3313 AND highest_rank = 'Sandanme 70'; -- 핫카이류
UPDATE rikishi SET highest_rank_since = '201701' WHERE external_api_id = 3343 AND highest_rank = 'Makushita 11'; -- 다이세이호
UPDATE rikishi SET highest_rank_since = '201803' WHERE external_api_id = 3266 AND highest_rank = 'Sandanme 74'; -- 마스텐류
