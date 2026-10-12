# 画面・API 一覧

[READMEに戻る](../README.ja.md) | [한국어](api.md)

> 公開画面・JSON API・管理画面の一覧です。READMEから移し(2026.10.12)、その後に追加したものを加えています。

## 画面(View)ルート — Thymeleaf レンダリング

| Method | Path | 説明 |
|---|---|---|
| GET | `/` | メインダッシュボード(最新の幕内番付をデフォルト表示) |
| GET | `/search?keyword=&page=` | 力士の統合検索結果一覧 |
| GET | `/rikishi/{id}` | 力士詳細プロフィール |
| GET | `/torikumi/{id}` | 取組詳細の全体ページ |
| GET | `/torikumi/{id}/fragment` | 上記と同じ内容の断片(fragment)— スライドパネル挿入用 |
| GET | `/bookmark` | お気に入りページ(LocalStorage の ID 配列でカード一覧を構成) |
| GET | `/updates` | 更新情報(`resources/updates.json`、日韓) |
| GET | `/privacy` | 個人情報の取り扱い(日韓) |
| GET | `/robots.txt`, `/sitemap.xml` | 検索エンジン向け(サイトマップは幕内・十両力士のプロフィール + 日韓のURL) |
| GET | `/admin/stats?days=7|30|90` | 管理者用アクセス統計(日別の訪問者・PV、よく見られた力士、参照元。IPは保存しない) |
| GET / POST | `/admin/login` | 管理者ログインフォーム表示 / ログイン処理(成功時 `/admin/comments` へ移動) |
| POST | `/admin/logout` | 管理者ログアウト(セッション無効化) |
| GET | `/admin/comments?page=` | 管理者コメント管理ダッシュボード(`/admin/**`、セッション `isAdmin` ガード) |
| GET | `/admin/rikishi?keyword=&page=` | 管理者用力士一覧/検索 |
| GET / POST | `/admin/rikishi/{id}/edit` | 力士プロフィール編集フォーム表示 / 保存 |
| GET | `/admin/basho` | 管理者用場所一覧(番付・取組の件数を含む) |
| GET / POST | `/admin/basho/new`, `/admin/basho` | 新規場所作成フォーム表示 / 作成(年・月の重複を防止) |
| POST | `/admin/basho/import?fromYear=&toYear=` | sumo-api から指定年範囲の場所メタ情報をインポート(本場所6場所、未開催分はスキップ) |
| POST | `/admin/basho/import-year?year=` | 指定年の全場所の全階級(幕内〜序ノ口)の番付(公式の勝敗・休場数を含む)、取組、優勝・三賞・金星を一括インポート |
| POST | `/admin/basho/{bashoId}/import-all` | 1場所分の全データ(番付・取組・表彰)を一括インポート |
| POST | `/admin/basho/{bashoId}/banzuke/import?division=` , `/admin/basho/{bashoId}/torikumi/import?division=` | 場所・地位区分単位の番付 / 取組インポート(未マッチの力士はスキップ) |
| POST | `/admin/basho/{bashoId}/awards/import` | 優勝・三賞(sumo-api)+ 金星(番付・取組から算出)を更新(再実行可) |
| GET / POST | `/admin/basho/{bashoId}/edit`, `.../{bashoId}` , `.../{bashoId}/delete` | 場所期間編集フォーム / 初日・千秋楽の日付修正 / 削除(番付・取組が空の場合のみ削除可) |
| GET | `/admin/basho/{bashoId}/banzuke?division=` | 場所ごとの番付管理(地位区分タブ + 行追加フォーム) |
| POST | `/admin/basho/{bashoId}/banzuke` | 番付行の追加(力士・場所の重複を防止) |
| POST | `/admin/basho/{bashoId}/banzuke/{banzukeId}` , `.../delete` | 番付行の編集 / 削除 |
| GET | `/admin/basho/{bashoId}/torikumi?day=&division=` | 場所ごとの取組一覧(日目リンク + 地位区分フィルター) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/new`, `/admin/basho/{bashoId}/torikumi` | 新規取組フォーム表示 / 作成(同日・同じ東西の組み合わせの重複を防止) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/{id}/edit`, `.../{id}` , `.../{id}/delete` | 取組編集フォーム / 編集 / 削除 |

## API ルート — JSON

| Method | Path | 説明 |
|---|---|---|
| GET | `/api/banzuke?division=&bashoId=` | 地位区分ごとの番付一覧(タブ切り替え・場所選択時に非同期呼び出し。`bashoId` 省略時は最新の場所) |
| GET | `/api/search?keyword=&page=` | 力士検索(ページネーション) |
| GET | `/api/rikishiEntity?ids=3,15,24` | ごひいきページ用 — `{basho, cards}`: 基準となる場所 + リクエスト順のカード(今場所の番付・成績・直近の取組・次の取組)。最大50件 |
| GET | `/api/torikumi/day?bashoId=&division=&day=` | トップの「取組」タブ — 1日分の結果・予定の取組(取組順)、各力士のその一番の前までの成績、日付一覧と状態 |
| GET | `/api/torikumi/{torikumiId}/comments` | 該当取組のコメント全件取得 |
| POST | `/api/torikumi/{torikumiId}/comments` | コメント投稿(nickname, password, content) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/delete` | 自分のコメントを削除(password 検証) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/blind` | 管理者によるブラインド(セッション `isAdmin` が必要) |

> コメント API は投稿・削除・ブラインドのいずれも、**その取組の更新後のコメント一覧全体**をレスポンスとして返すため、
> フロントエンドはレスポンスをそのまま再レンダリングするだけで済むように設計しています。

