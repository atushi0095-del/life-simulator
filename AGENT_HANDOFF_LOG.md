# AGENT_HANDOFF_LOG

エージェント作業の引き継ぎ記録。新しい作業が終わったら **末尾に追記** すること。
既存のエントリは書き換えない。

---

## 2026-09-20 — Phase 2: WorkLog (Android) 新規実装

担当: Claude Code (session `018TMiDr32NLsyJ9ej6wry2a`)
ブランチ: `claude/worklog-android-app-xyu3yf`

### やったこと

`apps/worklog-android/` に Android アプリ **WorkLog ― 今日何時間働いた？**
(`com.ajuworks.worklog`, 1.0.0 / versionCode 1) を新規作成した。

2モジュール構成:

- `core/` — 純粋 Kotlin/JVM。勤務時間の計算・集計・入力検証・CSV生成。Android 依存なし。
- `app/` — Compose UI / Room / DataStore / WorkManager / Glance ウィジェット / AdMob。

実装済みの機能は `apps/worklog-android/README.md` と
`store/store-listing.txt` を参照。

### 検証できたこと

| 項目 | 結果 |
|---|---|
| `./gradlew :core:test` | **48件すべて成功** |
| 仕様書 §31 の計算ケース | すべて `core/src/test` に網羅（通常勤務・複数休憩・日跨ぎ・月跨ぎ・年跨ぎ・うるう日・編集後の再集計・削除後の再集計） |

### 検証できていないこと（重要）

**このビルド環境には Android SDK が無く、入手もできない。**
ネットワークポリシーが `dl.google.com` への接続を拒否しており
（プロキシが CONNECT に 403 を返す）、commandline-tools / platform /
build-tools / emulator のいずれも取得できなかった。
`maven.google.com` と `repo1.maven.org` には到達できるため
AGP と AndroidX の依存解決は可能だが、SDK platform が無いと
`:app` は構成すらできない。

そのため **以下は一度も実行されていない**:

- `:app` のコンパイル（Kotlin の構文エラーが残っている可能性がある）
- `:app:testDebugUnitTest`（Robolectric テストは書いたが未実行）
- `:app:lintRelease`
- debug / release ビルド、R8 smoke test
- **AAB の生成（未生成）**
- 実機・エミュレータでの動作確認
- ウィジェット、通知、CSV共有、広告の実動作確認

`settings.gradle.kts` は **Android SDK が検出できるときだけ `:app` を
include する**ようにしてある。SDK の無い環境でも `:core:test` が
落ちないようにするため。SDK のあるマシンでは自動的に `:app` が入る。

### 次の担当者が最初にやること

SDK のあるマシン（Android Studio が入っている環境）で:

```bash
cd apps/worklog-android
./gradlew :core:test                 # まず通ることを確認
./gradlew :app:assembleDebug         # ここでコンパイルエラーが出る可能性が高い
./gradlew :app:testDebugUnitTest
./gradlew :app:lintRelease
./gradlew :app:assembleRelease       # R8 有効。端末に入れて主要フローを確認
./gradlew :app:bundleRelease         # AAB
```

`:app` は一度もコンパイルされていないので、**import 漏れや API の
シグネチャ違いが残っていることを前提に**取り掛かること。
ロジックのバグではなく機械的な修正で済むはずだが、確認は必要。

### 人間がやる必要があること

1. **署名鍵の確認**（このセッションでは鍵に触れていない）
   既存の Ajuworks upload key があるなら、新しい鍵を作らずにそれを使う。
   `apps/worklog-android/keystore.properties` に設定する（git-ignore 済み）。
   鍵が無ければ Play App Signing の方針を決める必要がある。
2. **AdMob 本番 ID の発行と設定**
   現在は Google の公開テスト ID が入っている。本番 ID は
   `keystore.properties` か `WORKLOG_ADMOB_*` 環境変数から入れる。
   **コミットしないこと。**
3. **プライバシーポリシーのホスティング**
   `docs/privacy-policy.html` を GitHub Pages 等で公開し、
   その URL を `MainActivity.PRIVACY_POLICY_URL` と
   `store/store-listing.txt` に記入する。
   現在の URL (`https://ajuworks.github.io/worklog/privacy-policy.html`) は
   **仮の値で、まだ存在しない。**
4. **スクリーンショットの差し替え**
   `store/screenshots/` は実機キャプチャではなく、実際のレイアウト・文言・
   カラートークンから生成したモックアップ。実機で撮り直すことを推奨。
5. Play Console 上の作業一式（アプリ登録、データセーフティ申告、審査提出）。

### 技術的負債・既知の未実装

- `:app` 未コンパイル（上記）。**これが最大の負債。**
- Pro版（買い切り）は未実装。仕様書 §22 の通り初期公開には不要と判断した。
- グラフ・複数勤務先・詳細集計は未実装（初期版の対象外）。
- Room のマイグレーションは version 1 のみ。`exportSchema = true` にしてあるので
  `app/schemas/` にスキーマが出力される（初回ビルド時に生成）。
- 手動追加フォームで「休憩60分」のように合計だけ入力した場合、
  休憩は勤務時間の中央に1件として合成される（合計は正確、開始時刻は便宜的）。
  詳細は `WorkEditor.fromTotals` のコメント参照。
- 週の集計は `WorkAggregator.inWeekOf` にあるが、ホーム画面では
  当月＋前7日分のレコードから計算している。月初の週で、前月末の勤務が
  7日より前にある場合は週合計に入らない。実害は小さいが厳密ではない。
- インタースティシャルは「履歴12回閲覧ごと」に固定。実データを見て調整の余地あり。

### 費用

サーバー費 0円 / API費 0円 / クラウド費 0円。
使用しているのは Kotlin, Jetpack (Compose, Room, DataStore, WorkManager,
Glance), Material 3, Google Mobile Ads SDK (AdMob), Google UMP SDK のみ。
いずれも無料。独自サーバー・外部DB・生成AI API・有料SDKは使用していない。
