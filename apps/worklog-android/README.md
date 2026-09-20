# WorkLog ― 今日何時間働いた？

出勤・休憩・退勤を押すだけ。自分の勤務時間をかんたん記録する Android アプリ。

会社向けの勤怠管理システムではありません。給与計算も人事管理もシフト提出もしません。
**自分が今日何時間働いたかを記録する、いちばん簡単なアプリ** を目指しています。

| | |
|---|---|
| package | `com.ajuworks.worklog` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 |
| 言語 | Kotlin + Jetpack Compose (Material 3) |
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

### 5. 退勤 < 出勤 は「翌日退勤」と解釈する

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

既定では Google の公開テスト ID が入っています。本番 ID は署名と同じ仕組みで
差し替えます (`admob.appId` / `admob.bannerUnitId` / `admob.interstitialUnitId`、
または `WORKLOG_ADMOB_*`)。本番 ID をコミットしないでください。

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
| 計算ロジック (48件) | `core/src/test` | `./gradlew :core:test` |
| 永続化 (Robolectric) | `app/src/test` | `./gradlew :app:testDebugUnitTest` |
| 実機フロー | `app/src/androidTest` | `./gradlew :app:connectedDebugAndroidTest` |

`core` のテストは仕様書 §31 の全ケース (通常勤務・複数休憩・日跨ぎ・月跨ぎ・年跨ぎ・
うるう日・編集後の再集計・削除後の再集計) を含みます。
強制終了と端末再起動からの復元は `app/src/test` の `WorkRepositoryTest` で、
同じ DB の上に repository を作り直すことで検証します。

---

## ストア素材

[`store/`](store/) に `store-listing.txt`、アイコン、スクリーンショット、
フィーチャーグラフィックの下書きがあります。
