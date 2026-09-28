# 🏯 sumoarchive

[한국어](README.md) | **日本語**

韓国語で大相撲の情報を手軽に調べられるアーカイブWebサービスです。

---

## 1. 企画意図

大相撲には番付・四股名・決まり手など独自の用語が多く、公式情報のほとんどが日本語でのみ提供されているため、
韓国語ユーザーにとってはアクセスしづらいのが現状です。

**sumoarchiveは、韓国語ユーザーが予備知識なしでも力士のプロフィール、番付、取組結果を
気軽に調べて楽しめるようにすることを目標としたプロジェクトです。**

MVP(現段階)以降は、次のような方向でサービスを拡張する予定です。

- **決まり手事典**: 現在は円グラフで統計のみを表示している決まり手に、技の説明や動きを
  テキスト・画像で解説する事典機能を追加
- **番付予測シミュレーター**: 直前の場所の成績をもとに、次の場所の番付の変動を
  重み付け計算で予測するシミュレーター

---

## 2. 目標シナリオ

機能仕様書(`docs/기능명세서.txt`)に基づき、ユーザーは次のことができます。

1. **メイン画面**でログインなしに最新の幕内番付をすぐに確認できる。`番付` の下にある
   **年・場所・階級の3つのドロップダウン**で見たい番付を選び、非同期で読み込む — 年を変えると
   その年に開催された場所だけが場所ドロップダウンに再設定され、階級(幕内/十両/幕下/三段目/序二段/
   序ノ口)は独立して選択できる。初期値は最新の場所・幕内(本場所開催前でも発表済みの番付であれば表示)。
2. 番付表の**力士名をクリック**して、その力士のプロフィールページへ移動できる。
3. **検索欄**で韓国語の四股名 / 日本語の四股名 / 過去に名乗っていた四股名までまとめて検索でき、同名の力士や
   受け継がれた四股名(襲名)があっても、本名・活動時期・最高位・現在の状態(現役/引退/親方)を見て
   目的の力士を正確に選べる。
4. **力士プロフィールページ**で、基本情報(本名・生年月日・出身地・所属部屋・身長/体重など)、
   キャリア成績(通算成績・優勝・三賞・金星)、決まり手別の勝ち技統計(円グラフ)をひと目で確認できる。
5. プロフィール下部の**星取表**で、初土俵以降の全場所の成績を日ごとに確認でき、個別の取組
   アイコン(○/●/休)をクリックすると、その取組の詳細をスライドパネルですぐに開ける。
6. **取組詳細ページ**で、場所情報(場所・日目)、東/西の対戦、決まり手(韓国語・日本語併記)、
   YouTubeのハイライト動画を確認できる。
7. 取組詳細画面で**ニックネームと4桁の数字パスワードによる匿名コメント**を投稿でき、自分のコメントを
   パスワードで削除できる。
8. *(管理者)* `/admin/login` でログインするとコメント管理ダッシュボード(`/admin/comments`)へ移動し、
   サイト全体の取組のコメントを新しい順にまとめて確認して、不適切なコメントをパスワード確認なしで即座に
   非表示(ブラインド)にできる。
9. **お気に入りページ**(`/bookmark`)で気になる力士をブックマークし(LocalStorageベース、ログイン不要)、
   自分だけのリストを作ってドラッグ&ドロップで並べ替えられる。
10. *(管理者)* `/admin/rikishi` で力士を検索し、プロフィール情報(基本情報/階級/現役・引退/写真)を
    直接編集できる。
11. *(管理者)* `/admin/basho` で新しい場所を作成し、その場所の番付を力士一人ずつ
    (地位区分/階級/東西/枚数)追加・編集・削除できる。番付が階級の source of truth であるため、
    編集結果は力士詳細ページやメインダッシュボードにそのまま反映される。
12. *(管理者)* `/admin/basho/{id}/torikumi` で、その場所の取組を日目・地位区分ごとに追加・編集・削除
    できる(東/西の力士、勝者、決まり手、不戦勝・不戦敗、優勝決定戦かどうか、YouTube URL、韓国語・日本語の解説)。
    入力した取組は取組詳細ページ・力士の星取表・決まり手統計にそのまま反映される。

---

## 3. 技術スタック

| 区分 | スタック |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Spring MVC, Spring Data JPA) |
| View | Thymeleaf(サーバーサイドレンダリング + fragment の再利用) |
| DB | MySQL 8.0 (`mysql-connector-j`), Hibernate ORM |
| Frontend | Vanilla JS(fetch による部分更新)、Chart.js(CDN、決まり手ドーナツグラフ)、Google Fonts |
| 認証 | Spring Security 不使用 — セッション(`HttpSession`)+ カスタムインターセプターによる軽量実装 |
| ビルド | Gradle(Gradle Wrapper 同梱) |
| その他 | Lombok |

---

## 4. プロジェクト構成

```
sumoarchive/
├── docs/                                  # 機能・テーブル仕様書、ERD、DBスキーマDDL
│   ├── 기능명세서.txt
│   ├── 테이블명세서_v1.txt / v2.txt
│   ├── sumoarchive_db_schema.sql
│   └── erd.png
└── src/main/java/com/torikumilab/sumoarchive/
    ├── controller/            # 画面(View)コントローラー — Thymeleaf のビュー名を返す
    │   ├── advice/              # ViewExceptionHandler(画面側の404など共通例外処理)
    │   └── api/                 # JSON API コントローラー + ApiExceptionHandler
    ├── config/                # WebConfig, AdminAuthInterceptor(/admin/** のセッションガード)
    ├── domain/
    │   ├── entity/              # JPA エンティティ(+ constant 配下の enum)
    │   └── dto/                 # 画面/レスポンス専用 DTO(表示用文字列はサーバー側で組み立て)
    ├── repository/            # Spring Data JPA リポジトリ(+ QueryDSL を使わないカスタム実装)
    ├── service/               # ビジネスロジック(+ exception 配下のドメイン例外)
    ├── util/                  # RankDisplayUtil, KimariteDisplayUtil, YoutubeUrlUtil など
    ├── client/                # SumoApiClient(sumo-api.com 呼び出しラッパー)
    └── DataSeeder.java        # 開発用ダミーデータシーダー(デフォルト無効 — app.seed-demo=true の場合のみ)
```

`src/main/resources/`
```
templates/       # index, search/result, rikishi/detail, torikumi/detail, admin/login, error/404
static/          # css, js, images
application.properties                  # コミット対象 — 実際のパスワードなし
application-local.properties.example    # コミット対象 — ローカル設定テンプレート
application-local.properties            # git 管理外 — 実際のDB/管理者パスワード
```

---

## 5. API 概要

### 画面(View)ルート — Thymeleaf レンダリング

| Method | Path | 説明 |
|---|---|---|
| GET | `/` | メインダッシュボード(最新の幕内番付をデフォルト表示) |
| GET | `/search?keyword=&page=` | 力士の統合検索結果一覧 |
| GET | `/rikishi/{id}` | 力士詳細プロフィール |
| GET | `/torikumi/{id}` | 取組詳細の全体ページ |
| GET | `/torikumi/{id}/fragment` | 上記と同じ内容の断片(fragment)— スライドパネル挿入用 |
| GET | `/bookmark` | お気に入りページ(LocalStorage の ID 配列でカード一覧を構成) |
| GET / POST | `/admin/login` | 管理者ログインフォーム表示 / ログイン処理(成功時 `/admin/comments` へ移動) |
| POST | `/admin/logout` | 管理者ログアウト(セッション無効化) |
| GET | `/admin/comments?page=` | 管理者コメント管理ダッシュボード(`/admin/**`、セッション `isAdmin` ガード) |
| GET | `/admin/rikishi?keyword=&page=` | 管理者用力士一覧/検索 |
| GET / POST | `/admin/rikishi/{id}/edit` | 力士プロフィール編集フォーム表示 / 保存 |
| GET | `/admin/basho` | 管理者用場所一覧(番付・取組の件数を含む) |
| GET / POST | `/admin/basho/new`, `/admin/basho` | 新規場所作成フォーム表示 / 作成(年・月の重複を防止) |
| POST | `/admin/basho/import?fromYear=&toYear=` | sumo-api から指定年範囲の場所メタ情報をインポート(本場所6場所、未開催分はスキップ) |
| GET / POST | `/admin/basho/{bashoId}/edit`, `.../{bashoId}` , `.../{bashoId}/delete` | 場所期間編集フォーム / 初日・千秋楽の日付修正 / 削除(番付・取組が空の場合のみ削除可) |
| GET | `/admin/basho/{bashoId}/banzuke?division=` | 場所ごとの番付管理(地位区分タブ + 行追加フォーム) |
| POST | `/admin/basho/{bashoId}/banzuke` | 番付行の追加(力士・場所の重複を防止) |
| POST | `/admin/basho/{bashoId}/banzuke/{banzukeId}` , `.../delete` | 番付行の編集 / 削除 |
| GET | `/admin/basho/{bashoId}/torikumi?day=&division=` | 場所ごとの取組一覧(日目リンク + 地位区分フィルター) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/new`, `/admin/basho/{bashoId}/torikumi` | 新規取組フォーム表示 / 作成(同日・同じ東西の組み合わせの重複を防止) |
| GET / POST | `/admin/basho/{bashoId}/torikumi/{id}/edit`, `.../{id}` , `.../{id}/delete` | 取組編集フォーム / 編集 / 削除 |

### API ルート — JSON

| Method | Path | 説明 |
|---|---|---|
| GET | `/api/banzuke?division=&bashoId=` | 地位区分ごとの番付一覧(タブ切り替え・場所選択時に非同期呼び出し。`bashoId` 省略時は最新の場所) |
| GET | `/api/search?keyword=&page=` | 力士検索(ページネーション) |
| GET | `/api/rikishiEntity?ids=3,15,24` | お気に入りページ用 — ID リストで力士カードを取得(リクエスト順を維持) |
| GET | `/api/torikumi/{torikumiId}/comments` | 該当取組のコメント全件取得 |
| POST | `/api/torikumi/{torikumiId}/comments` | コメント投稿(nickname, password, content) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/delete` | 自分のコメントを削除(password 検証) |
| POST | `/api/torikumi/{torikumiId}/comments/{commentId}/blind` | 管理者によるブラインド(セッション `isAdmin` が必要) |

> コメント API は投稿・削除・ブラインドのいずれも、**その取組の更新後のコメント一覧全体**をレスポンスとして返すため、
> フロントエンドはレスポンスをそのまま再レンダリングするだけで済むように設計しています。

---

## 6. 主な設計ポイント

- **表示用文字列はサーバー側で完成させて返す**: 「前頭10枚目」「3일째/3日目」のような表記変換は、
  `RankDisplayUtil`/`KimariteDisplayUtil` などのユーティリティを使ってサービス層であらかじめ組み立てて DTO に格納し、
  テンプレート側では最小限の分岐だけで済むように設計しています。
- **Thymeleaf fragment の再利用**: 取組詳細は、単独ページ(`/torikumi/{id}`)と力士プロフィールの
  スライドパネル(`/torikumi/{id}/fragment`)がまったく同じ `th:fragment="content"` を共有しています。
  パネルはその fragment を fetch で受け取ってそのまま挿入し、挿入後に `SumoComments.bind()` を呼び直すだけで
  コメント機能まで再利用します。
- **バッチ取得による N+1 の回避**: 取組や星取表で東西両力士(または複数の対戦相手)の番付を取得する際は、
  `IN` 句のバッチクエリ1回でまとめて取得し、Map にマッピングしています。
- **ソフトデリート + 削除理由の区別**: コメント削除は物理削除を行わず `is_deleted` フラグのみを更新し、
  `DeletedBy`(USER/ADMIN)enum で「投稿者により削除」と「管理者によりブラインド」を区別して
  表示することで、レイアウトと文脈を保ちます。
- **例外処理を画面/API で二分化**: 同じ `EntityNotFoundException` でも、`controller` パッケージ(画面)では
  `ViewExceptionHandler` が404ページに、`controller.api` パッケージでは `ApiExceptionHandler`
  (`@Order(HIGHEST_PRECEDENCE)`)が JSON に、それぞれ異なる形で変換します。
- **軽量な管理者認証**: Spring Security を使わず、セッション属性 `isAdmin` だけで管理者を判別します。
  `AdminAuthInterceptor` + `WebConfig` が `/admin/**` パスをガードし、コメントのブラインド API は
  別途独自にセッションを確認して `AdminOnlyException`(401)を投げる二重ゲート方式にしています。
- **画面と API の再利用**: コメント管理ダッシュボード(`/admin/comments`)は新たな削除 API を作らず、取組詳細画面で
  使っていたブラインド API(`POST /api/torikumi/{id}/comments/{id}/blind`)を fetch でそのまま呼び出します。
- **サーバーが関知しないパーソナライズ**: お気に入りは DB に一切保存せず、ブラウザの LocalStorage にある ID 配列が
  唯一の保存先です。サーバーはその配列の順序を決して変えずにレスポンス順へ反映するため、ドラッグで
  並べ替えた順序がリロード後も維持されます。
- **`<input type="date">` ではロケールのフォーマットを信用しない**: `@DateTimeFormat(pattern = "yyyy-MM-dd")` を
  明示しないと、Spring がリクエストのロケール(韓国語)基準の short style(「94. 3. 1.」)で値を返してしまい、
  ブラウザが日付を読み取れません。管理者用力士編集フォームの生年月日/初土俵日/引退日はすべてこのパターンを明示しています。

---

## 7. インフラ / 環境設定

- **DB**: MySQL 8.0。スキーマ DDL は `docs/sumoarchive_db_schema.sql` を参照(実際には
  `spring.jpa.hibernate.ddl-auto=update` でエンティティを基準に自動反映しながら開発しています)。
- **シークレットの分離**: `application.properties` はコミットされますが実際の値は含まず、DB アカウントと
  管理者ログインアカウントは `application-local.properties`(git 管理外)でのみ管理します。
  ```properties
  spring.config.import=optional:classpath:application-local.properties
  ```
  により、ローカル設定を任意で読み込む構成です。
- **データソース**: 当初は `DataSeeder`(開発用ダミー)でデータを投入していましたが、現在は **sumo-api.com** から
  実データを取得する方式へ移行中です。`DataSeeder` はデフォルトで無効(`app.seed-demo=false`)で、
  管理者画面 `/admin/rikishi` の「ロスターインポート」ボタンが現役の部屋・力士を、`/admin/basho` の
  「場所インポート」ボタンが指定年範囲の場所メタ情報を取得します(番付/取組のインポートは今後の作業)。
  外部 API 関連の設定: `sumo-api.base-url`, `app.seed-demo`。

### ローカルでの実行方法

```bash
# 1. MySQL にスキーマを作成(例: sumo)
CREATE DATABASE sumo CHARACTER SET utf8mb4;

# 2. ローカル設定ファイルを作成
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# → DB アカウント、任意の管理者 ID/パスワードを入力

# 3. 実行(初回起動時にダミーデータを自動シーディング)
./gradlew bootRun
```

デフォルトの接続先: `http://localhost:8080`
管理者ログイン: `http://localhost:8080/admin/login`(ログイン成功時はコメント管理ダッシュボードへ移動)

---

## 8. 今後の予定(ロードマップ)

- [x] 管理者コメント管理ダッシュボード(`/admin/comments` — 全コメント閲覧 + ブラインド)
- [x] お気に入りページ(`/bookmark`、LocalStorage ベース、ログイン不要、ドラッグで並べ替え)
- [x] 管理者による力士プロフィール編集(`/admin/rikishi` — 一覧/検索 + 編集フォーム)
- [x] 管理者による番付データの手動更新(`/admin/basho` — 場所の作成・期間編集・削除 + 番付行の CRUD)
- [x] 管理者用取組入力 UI(`/admin/basho/{id}/torikumi` — 日目・地位区分ごとの取組 CRUD)
- [ ] 決まり手の詳細解説事典
- [ ] 番付予測シミュレーター
- [ ] 外部相撲データ API(sumo-api.com)との連携 — *進行中*: ロスター・場所のインポート完了、
  番付/取組のインポートは実装予定

---

## 9. ドキュメント

- [`docs/기능명세서.txt`](docs/기능명세서.txt) — 機能仕様書(全体)
- [`docs/테이블명세서_v2.txt`](docs/테이블명세서_v2.txt), [`docs/sumoarchive_db_schema.sql`](docs/sumoarchive_db_schema.sql) — DB 設計
- [`docs/erd.png`](docs/erd.png) — ERD 図

> ※ `docs/` 配下の仕様書は韓国語で作成されています。
