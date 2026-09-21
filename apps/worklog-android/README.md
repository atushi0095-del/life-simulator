# WorkLog ― 今日何時間働いた？

出勤・休憩・退勤を押すだけ。自分の勤務時間をかんたん記録する Android アプリ。

会社向けの勤怠管理システムではありません。給与計算も人事管理もシフト提出もしません。
**自分が今日何時間働いたかを記録する、いちばん簡単なアプリ** を目指しています。

| | |
|---|---|
| package | `com.ajuworks.worklog` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| 言語 | Kotlin 2.3.21 + Jetpack Compose (Material 3) |
| データ | 端末内のみ (Room / DataStore) |
| ログイン | 不要 |
| 固定費 | 0円 |

---

## モジュール構成

```
apps/worklog-android/
├── core/   純粋な Kotlin/JVM。勤務時間の計算がすべてここにある
└── app/    Android。UI・Room・WorkManager・ウィジェット
```

`core` に Android 依存はひとつもありません。勤務時間の計算・集計・検証・CSV 生成は
すべてここにあり、`./gradlew :core:test` だけで検証できます。Android SDK も
エミュレータも不要です。UI から Room や SQL を直接触る箇所はありません。

### なぜ分けているか

集計が間違わないことがこのアプリの生命線です。計算を Android から切り離すと、
日跨ぎ・月跨ぎ・年跨ぎ・うるう日・タイムゾーン変更といった条件を、
エミュレータなしで秒単位のテストとして書けます。

---

## 仕様上の重要な決定

### 1. 常駐タイマーを持たない

バックグラウンドで動くタイマーはありません。保存するのは

- 出勤日時
- 退勤日時
- 休憩開始日時 / 休憩終了日時

だけです。経過時間は画面を描くたびに

```
経過 = 現在日時 − 出勤日時 − 休憩時間
```

として計算します。結果として、

- バッテリーを消費しない
- OS のバックグラウンド制限を受けない
- アプリを強制終了しても勤務中状態が失われない
- 端末を再起動しても復元される

という性質が「実装の努力」ではなく「構造」として保証されます。
勤務中かどうかは **退勤日時が NULL の行が DB にあるか** だけで決まり、
メモリ上にはどこにも保持されません。

### 2. 日跨ぎ勤務は「出勤した日」に計上する

```
9月20日 22:00 出勤
9月21日 06:00 退勤
→ 8時間勤務、9月20日 に計上
```

週・月・年の集計もすべて出勤日基準です。日付をまたいで分割しません。
分割すると、勤務中に日付が変わった瞬間「今日の勤務時間」が遡って減るという、
利用者から見て理解不能な挙動になるためです。
月末の夜勤は翌月ではなく **当月** に入ります。

### 3. タイムゾーンが変わっても過去の記録が書き換わらない

日時は「絶対時刻 (epoch millis)」と「打刻時点の UTC オフセット」の 2 つを保存します
(`core/Stamp.kt`)。文字列では保存しません。

日本で 09:00 に出勤した記録は、その後ロンドンに移動しても 09:00 のままで、
所属する日も変わりません。経過時間は絶対時刻から計算するので、どちらにせよ正確です。
サマータイムをまたぐ勤務も実時間で計算されます。

### 4. 休憩の重なりは二重に引かない

休憩区間はマージしてから合計します。ウィジェットとアプリの同時タップや、
手動編集で重なった休憩を作っても、同じ時間が 2 回引かれることはありません。
退勤時刻を超える休憩は勤務時間内にクランプされ、実働がマイナスになることもありません。

### 5. 手動追加の「休憩合計のみ」入力について

履歴の「勤務を追加」で、休憩を**合計分数だけ**入力した場合
（例: 休憩 60分）、その休憩は **勤務時間の中央に1件の区間として合成** されます。

- 休憩の**合計は正確**です。実働時間・集計は必ず正しくなります。
- ただし詳細画面に表示される休憩の**開始・終了時刻は便宜的な値**であり、
  実際にその時刻に休憩したことを意味しません。

正確な休憩時刻を残したい場合は、編集画面で休憩を個別に追加してください。

初版ではこの仕様を維持しています。休憩を「合計分数」と「時刻区間」の
どちらでも保持できるようデータモデルを変更することは可能ですが、
DB スキーマの変更を伴い、得られる利益に対してリスクが見合わないと判断しました。

### 6. 退勤 < 出勤 は「翌日退勤」と解釈する

「翌日にまたがる」チェックボックスはありません。
退勤時刻が出勤時刻以下なら翌日として解釈し、そのことを画面上で通知します
(`Issue.OvernightAssumed`)。実在する勤務ではこの解釈しかありえず、
夜勤の入力が無操作で済みます。

---

## ビルド

```bash
# 計算ロジックのテスト。Android SDK 不要
./gradlew :core:test

# アプリ本体。Android SDK が必要
./gradlew :app:assembleDebug
./gradlew :app:lintRelease
./gradlew :app:bundleRelease     # Google Play 用 AAB
```

ビルドに必要なもの:

| | |
|---|---|
| JDK | **21**（Robolectric が Android SDK 36 のサンドボックスに Java 21 を要求するため。アプリ自体の bytecode は 17） |
| Gradle | 8.14.3（wrapper 同梱） |
| AGP | 8.13.0 |
| Android SDK | Platform 36 / Build Tools 36.0.0 / platform-tools |

`settings.gradle.kts` は **Android SDK が見つかるときだけ `:app` を include します**。
`ANDROID_HOME` / `ANDROID_SDK_ROOT` / `local.properties` の `sdk.dir` のいずれかを
設定してください。SDK のない環境 (CI サンドボックス等) では `:app` が自動的に外れ、
`:core:test` だけが動きます。

### 署名

鍵とパスワードはリポジトリに入れません。`keystore.properties`
(git-ignore 済み) か環境変数から読みます。

```properties
# apps/worklog-android/keystore.properties
store.file=../ajuworks-upload.jks
store.password=***
key.alias=ajuworks-upload
key.password=***
```

環境変数の場合は `WORKLOG_KEYSTORE_FILE` / `WORKLOG_KEYSTORE_PASSWORD` /
`WORKLOG_KEY_ALIAS` / `WORKLOG_KEY_PASSWORD`。

どちらも無い場合、release ビルドは debug 署名にフォールバックします。
これは R8 有効の状態を鍵なしで検証できるようにするためで、
**Play へのアップロードには使えません**。

### AdMob

既定では Google の公開テスト ID が入っています。本番 ID は以下の順で探します。

1. `apps/worklog-android/keystore.properties`（git-ignore 済み）
2. Gradle プロパティ（`~/.gradle/gradle.properties` または `-P`）
3. 環境変数

キー名は `admob.appId` / `admob.bannerUnitId` / `admob.interstitialUnitId`、
環境変数は `WORKLOG_ADMOB_APP_ID` / `WORKLOG_ADMOB_BANNER_UNIT_ID` /
`WORKLOG_ADMOB_INTERSTITIAL_UNIT_ID`。

例（`~/.gradle/gradle.properties`）:

```properties
admob.appId=ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx
admob.bannerUnitId=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
admob.interstitialUnitId=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

**本番 ID をコミットしないでください。**

なお **debug ビルドは設定に関わらず必ずテスト ID を使います**
（`app/build.gradle.kts` の `buildTypes.debug` で上書き）。
debug ビルドから本番広告をリクエストするのはポリシー違反のため、
運用ルールではなく構造で防いでいます。

### 広告 SDK のバージョン

| SDK | version | 備考 |
|---|---|---|
| Google Mobile Ads SDK | 25.5.0 | 25.0.0 の破壊的変更はメディエーションアダプタ／カスタムイベント向けで、本アプリは未使用。必要 minSdk は 23（本アプリは 26） |
| User Messaging Platform | 4.0.0 | 必要 minSdk は 23。`setConsentSyncId()` が追加された以外、本アプリが使う API に変更なし |

25.x では `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize()` が
非推奨（削除ではない）になり、large anchored adaptive API が推奨されています。
本アプリは現時点では従来 API のまま `@Suppress("DEPRECATION")` を付けています
（理由は `ads/AdBanner.kt` のコメント参照）。実ビルドできる環境で差し替えてください。

---

## 広告の方針

- **打刻を広告で遮らない。** 出勤・退勤・休憩に広告は一切紐づけません。
- バナーは履歴画面の下部のみ。読み込めるまで **高さ 0** で、失敗したら消えます
  (レイアウトが飛びません)。
- インタースティシャルは履歴を 12 回閲覧するごとに 1 回だけ、かつ事前読み込み済みの
  ときだけ。なければ黙ってスキップします。
- 広告・UMP が失敗しても、出勤/退勤/休憩/集計/履歴/CSV/ウィジェットはすべて動きます。
  `AdsController` はどの経路でも例外を外に出しません。

---

## プライバシー

- 勤務履歴は端末内のみ。独自サーバーへ送信しません。
- アカウント登録不要。氏名・メール・電話番号・位置情報を取得しません。
- 位置情報による自動出勤は **実装しません**。
- CSV は利用者が共有操作をしたときだけ、Android の共有シートに渡されます。
  Google Drive API 等は使いません。
- 広告のために Google Mobile Ads SDK が広告関連データを処理することがあります。

詳細は [`docs/privacy-policy.html`](docs/privacy-policy.html) (日本語/English)。

---

## テスト

| 種別 | 場所 | 実行 |
|---|---|---|
| 計算ロジック (54件) | `core/src/test` | `./gradlew :core:test` |
| 永続化 (Robolectric, 11件) | `app/src/test` | `./gradlew :app:testDebugUnitTest` |
| 実機フロー | `app/src/androidTest` | `./gradlew :app:connectedDebugAndroidTest` |

`core` のテストは仕様書 §31 の全ケース (通常勤務・複数休憩・日跨ぎ・月跨ぎ・年跨ぎ・
うるう日・編集後の再集計・削除後の再集計) を含みます。
強制終了と端末再起動からの復元は `app/src/test` の `WorkRepositoryTest` で、
同じ DB の上に repository を作り直すことで検証します。

---

## CI（GitHub Actions）

`.github/workflows/worklog-android.yml` が、

core test → assembleDebug → app unit test → lint → assembleRelease (R8) → bundleRelease

をすべて実行します。成果物は artifact として保存されます。

| artifact | 内容 |
|---|---|
| `worklog-release` | `app-release.aab` / release APK / R8 の `mapping.txt` |
| `worklog-reports` | lint レポート、テストレポート |

署名鍵は GitHub Secrets から復元します（値はログに出力しません）。

| Secret | 内容 |
|---|---|
| `WORKLOG_KEYSTORE_BASE64` | upload keystore を base64 にしたもの |
| `WORKLOG_KEYSTORE_PASSWORD` | keystore のパスワード |
| `WORKLOG_KEY_ALIAS` | 鍵の alias |
| `WORKLOG_KEY_PASSWORD` | 鍵のパスワード |
| `WORKLOG_ADMOB_APP_ID` 他 | 本番 AdMob ID（任意） |

**Secrets が未設定でもビルドは止まりません。** その場合 release は debug 署名に
フォールバックし、ジョブサマリーに `Signed with upload key: false` と表示されます。
その AAB は **Play へアップロードできません**。

instrumented test は CI に載せていません（hosted runner の emulator は遅く不安定なため）。
ローカルの Android Studio で `./gradlew :app:connectedDebugAndroidTest` を実行してください。

## Data Safety（Play Console 転記用）

**「データを収集しない」と申告しないこと。** 勤務記録は端末外に出ませんが、
AdMob SDK が扱うデータがあります。

| 項目 | 申告内容 |
|---|---|
| アプリ自身が収集・送信するデータ | なし。勤務記録・メモ・設定はすべて端末内 |
| 第三者 SDK が扱うデータ | Google Mobile Ads SDK (AdMob) が広告 ID・おおよその位置情報（IP由来）・端末情報などを広告配信のために処理 |
| データの種類（想定） | 「位置情報 > おおよその位置情報」「アプリのアクティビティ」「デバイス ID またはその他の ID > デバイスまたはその他の ID」 |
| 目的 | 広告またはマーケティング |
| 共有の有無 | あり（Google へ）。開発者は個人を特定できる形で受け取らない |
| 収集は必須か | 広告 ID は必須ではない（ユーザーは端末設定でリセット・オプトアウト可能） |
| 転送時の暗号化 | あり（HTTPS） |
| 削除要請 | アンインストールで端末内データは削除。広告 ID は端末設定から |

**必ず最新の Google Mobile Ads SDK 公式ガイダンスと照合してから申告してください。**
AdMob は Play Console のデータセーフティ用の申告ガイドを公開しており、
SDK バージョンによって該当項目が変わります。

## ストア素材

[`store/`](store/) に `store-listing.txt`、アイコン、スクリーンショット、
フィーチャーグラフィックの下書きがあります。
