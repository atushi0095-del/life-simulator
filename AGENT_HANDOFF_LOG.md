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

---

## 2026-09-21 — Phase 2 続き: SDK無し環境での可能な範囲の完成作業

担当: Claude Code (session `018TMiDr32NLsyJ9ej6wry2a`)
ブランチ: `claude/worklog-android-app-xyu3yf`

### 結論を先に: AAB は今回も生成できていない

「Android SDK のある環境で」という前提で依頼を受けたが、
**この環境にも Android SDK は無く、前回とまったく同じ理由で入手もできない。**

- `dl.google.com` が egress proxy に 403 で拒否される（organization policy）
- `maven.google.com` は全て `dl.google.com` へ 301 リダイレクトされるため同じく不可
- AndroidX / Compose / Room / Glance / AGP はいずれも Maven Central に無い（404 で確認）
- 利用可能な環境は `Default - trusted network access` の1つのみで、
  この allowlist に `dl.google.com` は含まれていない

したがって **`:app` のコンパイル・lint・instrumented test・release build・
AAB 生成・実機確認はいずれも今回も実行できていない。**
これは判断ではなく環境の制約であり、セッション内で回避する手段は無い。

非公式の GitHub ミラー等から SDK を取得する手段は検討したが、
**Play へ配布する署名済み成果物を非公式配布の SDK で作るべきではない**ため
採用しなかった。

### 今回やったこと（SDK 無しで検証可能な範囲）

すべて `./gradlew :core:test` で検証済み（**54件 / 全件成功**、前回の48件から+6）。

1. **週集計の実装を修正**（報告済み技術的負債）
   `WorkAggregator.summaryWindow(today, weekStart)` を core に追加し、
   「今週の開始日」と「今月の1日」の早い方から今日までを明示的に窓とする方式に変更。
   従来の「月初−7日」という暗黙の余白依存をやめ、月境界に依存しなくなった。
   併せて `WorkRepository.recordsBetween(from, until)` を追加し、
   `HomeViewModel` は窓を **ViewModel 生成時に固定せず**、
   日付変更と週開始設定の変更に追従して再クエリするようにした
   （画面を日をまたいで開いたままにしても集計がずれない）。
   窓の正しさは1年分の全日 × 週開始3種を総当たりするテストで担保。

2. **API 36 対応**
   `compileSdk = 36` / `targetSdk = 36`（minSdk 26 は据え置き）。
   API 36 をコンパイルするには AGP 8.9 以上が必要なため、
   **AGP 8.7.3 → 8.13.0** に更新した（Gradle 8.14.3 と互換）。

3. **広告 SDK 更新**
   Google Mobile Ads SDK 23.6.0 → **25.5.0**、UMP 3.1.0 → **4.0.0**。
   調査の結果、25.0.0 の破壊的変更はメディエーションアダプタ／カスタムイベント
   （本アプリ未使用）と `mediation.VersionInfo` 削除で、本アプリの使用 API には影響なし。
   両 SDK の必要 minSdk は 23 で、本アプリの 26 が満たしている。
   `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize()` は 25.x で
   **非推奨（削除ではない）** のため、`@Suppress("DEPRECATION")` を付けて従来 API を維持した。
   実コンパイルできない状態で未検証の新 API へ差し替えるのは
   かえって危険と判断したため。実ビルドできる環境で large anchored adaptive API へ移行すること。

4. **AdMob 本番 ID の外部読み込み**
   `keystore.properties` → Gradle プロパティ（`~/.gradle/gradle.properties`）→
   環境変数 の順で解決するようにした。
   さらに **debug ビルドは設定に関わらず必ずテスト ID を使う**よう
   `buildTypes.debug` で上書きした（運用ルールではなく構造で担保）。

5. **README 追記**
   - Data Safety の Play Console 転記案（「何も収集しない」とはしていない。AdMob の扱う
     データを明記）
   - 手動追加で「休憩合計のみ」入力した場合の挙動（合計は正確／時刻は便宜的）を明記
   - 広告 SDK バージョンと移行状況の表

### SDK 無しでも確認できたこと

`:app` はコンパイルしていないが、以下は機械的に照合済み:

- 文字列リソース参照 **86箇所がすべて実在**（実 `res/` から R クラスを生成して照合）
- 日本語 **70件** / 英語 **70件** が完全一致（欠落ゼロ）
- 今回の編集で新たな構文エラー・未定義参照が発生していないこと
  （唯一残る `EditRecordScreen.kt:234` の "when must be exhaustive" は
  Compose が解決できないことによる偽陽性。`TimeTarget` は sealed で網羅済み）

### 次の担当者へ

**やるべきことは前回のエントリと変わらない。** SDK のあるマシンで:

```bash
cd apps/worklog-android
./gradlew :core:test            # 54件。まずこれが通ることを確認
./gradlew :app:assembleDebug    # 初コンパイル。ここでエラーが出る前提
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

`:app` は依然として **一度もコンパイルされていない**。
import 漏れ・API シグネチャ違いが残っている前提で取り掛かること。

### この作業を前に進めるために必要なこと（人間の作業）

**環境側の対応が必須。** いずれか:

- `dl.google.com` と `maven.google.com` を許可する network policy の環境を作る
  （Claude Code on the web の環境作成時に選択する。
  https://code.claude.com/docs/en/claude-code-on-the-web を参照）
- または、ローカルの Android Studio 環境でこのブランチを checkout してビルドする

それに加えて、前回エントリに挙げた
署名鍵の確認・AdMob 本番 ID・プライバシーポリシーのホスティング・
実機スクリーンショット・Play Console 作業は未着手のまま。
