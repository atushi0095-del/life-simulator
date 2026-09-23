# あと何日（com.ajuworks.atonannichi）

楽しみな日までをホーム画面で美しく数えるカウントダウン。予定管理ではない。データ・写真は端末内のみ。固定費0円。

## ビルド（正本は GitHub Actions）

このモジュールのビルド・テスト・署名済みAABの生成は、リポジトリの
`.github/workflows/countdown-android.yml` が正本。Android SDK が無い環境でも
開発を止めないため、`settings.gradle.kts` は SDK が見つからないときだけ `:app`
を外し、`:core`（純Kotlin/JVM）のテストは常に実行できるようにしてある。

- 暦計算・通知判定・更新スケジュールの単体テスト（Android SDK不要）: `./gradlew :core:test`
- アプリ側の単体テスト（Robolectric）: `./gradlew :app:testDebugUnitTest`
- 計装テスト: `./gradlew :app:connectedDebugAndroidTest`（実機/エミュレータ必要）
- lint: `./gradlew :app:lint`（`warningsAsErrors`）
- 署名済みAAB: Actions の `countdown-signed-release` アーティファクト
  （`countdown-release-UNSIGNED` は debug 署名なので公開成果物ではない）

### GitHub Actions Secrets（署名）
4アプリ共通の Ajuworks upload key を Base64 で渡す。値はログに出さない。

| Secret | 中身 |
|---|---|
| `COUNTDOWN_KEYSTORE_BASE64` | `upload.jks` を Base64 化したもの |
| `COUNTDOWN_KEYSTORE_PASSWORD` | keystore のパスワード |
| `COUNTDOWN_KEY_ALIAS` | `ajuworks-upload` |
| `COUNTDOWN_KEY_PASSWORD` | 鍵のパスワード |

workflow は、Secrets があるのに AAB が Ajuworks upload key
（SHA-256 `20:72:12:77:…:CF:91:4F`）で署名されていなければ **ビルドを失敗させる**。
本番AdMob IDは任意で `COUNTDOWN_ADMOB_APP_ID` /
`COUNTDOWN_ADMOB_BANNER_UNIT_ID` / `COUNTDOWN_ADMOB_INTERSTITIAL_UNIT_ID`。
未設定ならGoogle公式テストIDでビルドが続く（警告つき）。

## 仕様メモ
- 残日数は暦日の差（時刻は見ない）。端末のタイムゾーンの「今日」で数える。当日は TODAY、過ぎたら「○日前」または「終了」（イベントごとに選択）
- ウィジェット: 小(2x1)・中(2x2)・大(4x2)、リサイズ可、複数配置可（ウィジェットごとにイベント・デザインを保存）、置くと設定画面が開く、再設定可。広告なし
- デザイン5種: Minimal / Photo / Dark / Soft / Number。背景はアプリと共通の CardRenderer で描画（ウィジェットは約18万画素以内の Bitmap。RemoteViews の転送上限対策）
- 写真: Photo Picker で選んだ1枚をアプリ専用領域へ縮小コピー（最長辺1440px）。表示時は下側ほどぼかし＋明るさに応じた暗幕＋文字の影
- 更新: 日付変更直後(0:00:30)と朝9時の1日2回だけ RTC アラーム（非wakeup・exact権限不要）。再起動・時刻/TZ変更・アプリ更新で入れ直し。
  保険としてウィジェットの updatePeriodMillis=3時間でも描き直す（App Standby でアラームが遅延した場合）
- 通知: 30/7/3/前日/当日（イベントごと ON/OFF、既定は7日前・前日・当日）。朝9時以降の確認時に送る。送信済みは日付ごとに記録し二重通知しない
- 広告: 常時バナーなし。イベント保存時の3回に1回だけ全画面広告
- Pro（イベント無制限など）は未実装。現在はイベント数の制限なし

## 公開前に必要なこと
1. 署名
2. `~/.gradle/gradle.properties` に `atonannichi.admob.appId / bannerId / interstitialId`（バナーは未使用だが設定項目は共通）
3. AdMob で UMP 同意メッセージ作成
4. `docs/privacy-policy.html` を公開し URL を Play Console へ
5. データセーフティ: アプリ自体は収集なし（写真は端末内のみ）。AdMob の広告ID等を申告

## 公開準備（2026-09-18 最終仕上げ）

### 広告SDK
- Google Mobile Ads SDK **25.5.0** / UMP SDK **4.0.0**（v25 が要求する UMP 版）。
  v24・v25 の破壊的変更（minSdk 23、interscroller・SearchAdView・mediation VersionInfo・NativeAdViewHolder 等の削除）は本アプリの使用API（初期化・全画面広告・UMP）に影響なし（Google 公式リリースノートで確認）。
- 広告の取得・同意確認に失敗しても主要機能は動く（広告は読み込めた時だけ表示）。

### AdMob 本番ID（未設定＝テスト広告）
`~/.gradle/gradle.properties`（Git管理外）に次を書いて `gradlew :app:bundleRelease` するだけで本番化される。ソースへの記載は不要。
```
atonannichi.admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
atonannichi.admob.interstitialId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```
- debug ビルドは設定に関係なく常に Google 公式テストID。
- release で本番IDが未設定なら、ビルド時に `WARNING: AdMob の本番IDが未設定` が出る（テスト広告のAABになる）。

### 署名（upload key）
- 4アプリ共通の upload key：`C:\Users\atush\.ajuworks-signing\upload.jks`（alias `ajuworks-upload`）。パスワードは同フォルダの `upload.properties` のみに保存（リポジトリ・ログには書かない）。
- 読み込み順：環境変数 `AJUWORKS_UPLOAD_STORE_FILE/_STORE_PASSWORD/_KEY_ALIAS/_KEY_PASSWORD` → プロジェクト直下 `keystore.properties` → `~/.gradle/gradle.properties` の `ajuworks.signing.properties`（パス）。
- **`.ajuworks-signing` フォルダは必ず別媒体にバックアップすること**（紛失すると Play でアップロード鍵のリセット申請が必要）。
- Play App Signing を使う（アプリ署名鍵は Google が管理、上記はアップロード鍵）。

### データセーフティ（Play Console 転記用チェックリスト）
根拠：Google 公式「Google Mobile Ads SDK の Play データ開示」(developers.google.com/admob/android/privacy/play-data-disclosure、SDK 25.5.0 時点の記載)。
アプリ本体が扱うデータは端末外へ送信しないため「収集」に当たらない。以下は **Google Mobile Ads SDK が自動で収集・共有するもの** のみ。

- [ ] データを収集または共有しますか → **はい**
- [ ] 送信中のデータは暗号化されていますか → **はい**（SDK は TLS）
- [ ] 位置情報 > **おおよその位置情報**：収集 ✓・共有 ✓／目的：広告またはマーケティング、分析、不正行為防止・セキュリティ・コンプライアンス／必須（IPアドレスからの推定）
- [ ] アプリのアクティビティ > **アプリ内の操作**：収集 ✓・共有 ✓／目的：同上（起動、タップ、動画視聴など）
- [ ] アプリの情報とパフォーマンス > **診断**：収集 ✓・共有 ✓／目的：同上（起動時間、フリーズ率、電力使用量）
- [ ] **デバイスまたはその他のID**：収集 ✓・共有 ✓／目的：同上（広告ID、アプリセットID）
- [ ] 上記以外（個人情報、写真・動画、ファイル、連絡先、メッセージ等）→ **収集しない**
- [ ] データ削除の手段：アカウントなし。端末内データはアプリ内の削除またはアンインストールで消える。広告IDは端末設定でリセット・削除可
- 補足：UMP SDK については Google 公式に独立した開示ページが見当たらない（2026-09-18 確認）。上記4分類で申告し、Play Console の SDK 情報表示で最終確認すること。

アプリ本体のみで端末内に留まるデータ（申告対象外・参考）：イベント、アプリ専用領域にコピーした写真（端末内）
写真へのアクセス：Photo Picker で選ばれた1枚のみ（権限なし）

### プライバシーポリシー
`docs/privacy-policy.html`（アプリ内の表示と同一文面）。主要機能のデータは端末内処理・独自サーバーへの送信なし・広告のため Google Mobile Ads SDK が上記情報を処理、を区別して記載。
