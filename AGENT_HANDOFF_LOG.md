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

---

## 2026-09-21 (2) — GitHub Actions でビルド。**AAB 生成に到達**

担当: Claude Code (session `018TMiDr32NLsyJ9ej6wry2a`)
ブランチ: `claude/worklog-android-app-xyu3yf`

### 結論

Claude の実行環境では `dl.google.com` が遮断されたままなので、ビルドを
**GitHub Actions へ移した**。`.github/workflows/worklog-android.yml` を追加。

**結果: 全ステップ green。release AAB の生成まで到達した。**

core test → assembleDebug → app unit test → lint → assembleRelease (R8) →
bundleRelease がすべて成功。

### 実コンパイルで発見・修正した不具合

`:app` はこれまで一度もコンパイルされていなかったため、以下は **実際に動かして
初めて分かったもの**。14 回 run を回して 1 つずつ潰した。

| # | 症状 | 原因 | 対応 |
|---|---|---|---|
| 1 | `:app:kspDebugKotlin` 失敗。Ads SDK の `.kotlin_module` が全て読めない | play-services-ads 25.5.0 は Kotlin metadata 2.3.0。Kotlin 2.0.21 は 2.0.0 までしか読めない | Kotlin 2.0.21 → **2.3.21**、KSP → **2.3.12**（KSP は 2.3.0 から Kotlin 版数を冠さない独立採番に変わっていた） |
| 2 | `:app` の設定自体が失敗 | Kotlin 2.3 で `android { kotlinOptions { } }` が削除（警告ではなくエラー） | トップレベルの `kotlin { compilerOptions { jvmTarget } }` へ移行 |
| 3 | `kspDebugKotlin` が `IllegalStateException: unexpected jvm signature V` でクラッシュ | Room 2.6.1 の processor が KSP2 に非対応 | Room **2.7.2** へ |
| 4 | Robolectric が 1 件も実行できない。`targetSdkVersion=36 > maxSdkVersion=35` | Robolectric 4.14.1 は SDK 35 まで | Robolectric **4.17** へ（`@Config(sdk=35)` で逃げると、狙っている targetSdk と違う OS でテストすることになるので採用せず） |
| 5 | `Android SDK 36 requires Java 21 (have Java 17)` | SDK 36 の android-all は Java 21 bytecode | CI の JDK を **21** へ。アプリの bytecode target は 17 のまま（別設定） |
| 6 | 全テストが setup で落ちる。`Failed to interact with raw FileDescriptor internals` | Android 16 の `ApplicationSharedMemory` が `jdk.internal.access.SharedSecrets` を触る。modular JDK では既定で拒否 | unit test JVM にのみ `--add-exports` / `--add-opens` を付与 |
| 7 | 全テストが StackOverflow。同じフレームの無限反復 | **テスト側のバグ。** `object : Clock()` の中で `zone` と書くと、Clock の `getZone()` 由来の合成プロパティに束縛され、override が自分を呼ぶ | `this@WorkRepositoryTest.zone` に修飾 |
| 8 | `breaks can be taken more than once in a shift` が 8h30m を期待して 8h15m | **テスト側の計算ミス。** 打刻の合計は 9h15m、休憩 1h なので 8h15m が正しい | 仕様書の例（09:00→18:00 / 休憩 12:00-12:45・15:00-15:15 = 8時間）に合わせて timeline を修正。gross も assert するようにした |
| 9 | lint が 9 件で失敗（`warningsAsErrors = true`） | 下記 | 下記 |

lint の内訳と対応:

- **InvalidFragmentVersionForActivityResult (Fatal)** — 推移的に fragment < 1.3.0 が入る。
  `registerForActivityResult` の最低要件。dependency **constraint** で下限だけ 1.3.0 に引き上げ
  （上限は固定しないので、他が新しい fragment を要求すればそちらが勝つ）。
- **PluralsCandidate ×6** — 実際の個数（勤務日数・通知までの時間・勤務時間・通知本文）は
  `<plurals>` へ移行。英語は one/other、日本語は other のみ。
  ただし「%d番目の休憩」は個数ではなく**序数**なので plurals にするのは誤り。
  `tools:ignore` + コメントで明示。
- **ObsoleteSdkInt** — minSdk 26 なので `mipmap-anydpi-v26` → `mipmap-anydpi` にリネーム。
- **UnusedResources** — `app_tagline` を削除（ストア文言は `store/store-listing.txt` にある）。
- **UseKtx** — `Uri.parse` → `String.toUri`。
- **UnusedAttribute ×2** — `targetCellWidth/Height` は API 31+ 用で意図的。局所的に ignore。
- **OldTargetApi** — lint が targetSdk より上の API を知っているため発火。
  36 は現在 Play が新規アプリに要求する水準なので、lint 設定で無効化。

### 確認済みバージョン（CI の probe ステップ出力、2026-09-21 時点の最新）

```
compose-bom      2026.09.00   room            2.8.5
glance           1.2.0        work            2.11.2
lifecycle        2.11.0       activity-compose 1.13.0
navigation       2.10.1       datastore       1.2.1
core-ktx         1.19.0       fragment        1.9.0
play-services-ads 25.5.0      ump             4.0.0
```

本アプリは動作確認できた組み合わせを維持しており、上記最新への一括更新は
**あえて行っていない**（今回の目的は AAB 生成であり、機能追加・一括更新ではないため）。
更新する場合は CI で 1 つずつ確認すること。

### 署名について

**既存の Ajuworks upload key は見つかっていない。** GitHub Secrets にも未設定。
そのため現在の AAB は **debug 署名**であり、**Play へアップロードできない**。

鍵を用意したら以下の Secrets を設定すれば、同じ workflow がそのまま署名済み AAB を出す:

- `WORKLOG_KEYSTORE_BASE64`（`base64 -w0 upload-keystore.jks` の出力）
- `WORKLOG_KEYSTORE_PASSWORD` / `WORKLOG_KEY_ALIAS` / `WORKLOG_KEY_PASSWORD`

**新しい鍵は作っていない。** 既存鍵との競合を避けるため、鍵の用意は人間の判断に委ねる。

### まだ残っている人間の作業

1. upload keystore の用意と Secrets 登録（→ 署名済み AAB）
2. 本番 AdMob ID の発行と Secrets 登録（現在はテスト ID でビルドしている）
3. `docs/privacy-policy.html` のホスティングと URL 反映（現在の URL は仮）
4. 実機/エミュレータでの instrumented test と UI 確認（CI には載せていない）
5. `store/screenshots/` を実機キャプチャへ差し替え
6. Play Console の全作業

---

## 2026-09-21 (3) — upload key: **作成していない。ブロッカーあり**

担当: Claude Code (session `018TMiDr32NLsyJ9ej6wry2a`)
ブランチ: `claude/worklog-android-app-xyu3yf`

### 結論

本番 upload key の作成と GitHub Secrets 登録を指示されたが、**実施していない。**
理由は 2 つで、どちらも回避不能。

**1. Claude は GitHub Secrets に書き込めない（技術的に不可能）**

agent proxy が Actions の secrets 系エンドポイントを遮断している:

```
/actions/secrets            -> 403
/actions/secrets/public-key -> 403
/actions/variables          -> 403
/environments               -> 403
（同じトークンでリポジトリ本体の読み取りは 200）
```

MCP の github ツールにも secrets 管理用のものは無い。
これは「エージェントにリポジトリの秘密情報を書かせない」という意図的なガードで、
セッション内で回避する手段は無い。

**2. この環境で鍵を作るのは割に合わない**

`keytool` は使えるので鍵の生成自体は可能。しかし:

- この環境は**使い捨てのコンテナ**で、セッション終了時に破棄される。
  ここで作った鍵はチャット経由で運び出さない限り消える
- 運び出すということは、**今後すべての Ajuworks アプリを署名する鍵**が
  チャットのログに残るということ。署名鍵は「本人だけが持っている」ことに
  価値があるので、これは鍵の価値そのものを損なう
- そして上記 1 のため、**鍵を作っても署名済み AAB には到達しない**。
  リスクだけ増えて前進しない

したがって「作れるから作る」ではなく、**作らない**判断をした。
ユーザーのローカル PC で 1 コマンドで作れる。

### 代わりにやったこと

**`apps/worklog-android/docs/signing-setup.md` を追加**（手順書）。
Windows / macOS / Linux 両対応で、鍵の作成・権限設定・base64 化・Secrets 登録まで。

Windows の落とし穴を明記: **`certutil -encode` は使わない**
（`-----BEGIN CERTIFICATE-----` ヘッダが付いて base64 が壊れる）。
`[Convert]::ToBase64String([IO.File]::ReadAllBytes(...))` を使う。

**workflow に署名検証ゲートを追加**（`Verify the bundle signature`）:

- 毎回 `jarsigner -verify -verbose:summary -certs` と証明書 SHA-256 を出力
- **secrets を設定したのに debug 署名になっていたらジョブを失敗させる。**
  設定ミスや stale な configuration cache のせいで、署名されていないものが
  release として素通りするのを防ぐ
- secrets 未設定のときは warning を出して通す（R8 の smoke test は続けたいため）

**artifact 名が署名状態を表すようにした**:

| 状態 | artifact 名 |
|---|---|
| upload key で署名済み | `worklog-signed-release` |
| 未署名（debug 署名） | `worklog-release-UNSIGNED` |

ダウンロード後に取り違えないようにするため。

### 次にやること（人間）

1. `apps/worklog-android/docs/signing-setup.md` の手順でローカルに鍵を作る
2. 4 つの Secrets を登録する
   （`WORKLOG_KEYSTORE_BASE64` / `WORKLOG_KEYSTORE_PASSWORD` /
   `WORKLOG_KEY_ALIAS` / `WORKLOG_KEY_PASSWORD`）
3. workflow を再実行する

3 が終われば artifact は `worklog-signed-release` になり、
検証ステップが署名を確認する。そこまで来たら Claude に再開を依頼すれば、
run を確認して最終報告できる。

**鍵・パスワード・base64 は Git にもこのログにも入れていない。**
