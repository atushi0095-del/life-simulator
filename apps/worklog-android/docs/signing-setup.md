# Ajuworks 共通 upload key のセットアップ

WorkLog を含む Ajuworks の Android アプリは、**共通の upload key** で署名します。
この手順は **あなたのローカル PC で実行してください。**

> **なぜ Claude 側で作らないのか**
>
> 署名鍵は「あなただけが持っている」ことに価値があります。
> Claude の実行環境は使い捨てのクラウドコンテナで、セッション終了時に破棄されます。
> そこで作った鍵は、チャットを経由して運び出さない限り消えます。
> 今後すべての Ajuworks アプリで使う鍵を、使い捨て環境で作ってチャットに流すのは
> 割に合いません。1 コマンドで作れるので、手元で作ってください。
>
> また Claude は GitHub Secrets に書き込めません（API 側で禁止されています）。
> どのみち登録はあなたの操作が必要です。

---

## 1. 保存先を用意する

プロジェクトの外に置きます。Git には絶対に入れません。

### Windows (PowerShell)

```powershell
$dir = "$env:USERPROFILE\.ajuworks-signing"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

# 自分だけがアクセスできるようにする（継承を切って自分のみ許可）
icacls $dir /inheritance:r /grant:r "$($env:USERNAME):(OI)(CI)F" | Out-Null
```

### macOS / Linux

```bash
mkdir -p ~/.ajuworks-signing
chmod 700 ~/.ajuworks-signing
```

---

## 2. 鍵を作る

**既存の keystore を上書きしないよう、ファイルが無いことを確認してから実行してください。**

### Windows (PowerShell)

```powershell
$ks = "$env:USERPROFILE\.ajuworks-signing\ajuworks-upload-key.jks"
if (Test-Path $ks) { throw "すでに存在します。上書きしないでください: $ks" }

keytool -genkeypair -v `
  -keystore $ks `
  -storetype PKCS12 `
  -alias ajuworks-upload `
  -keyalg RSA -keysize 4096 `
  -validity 10950 `
  -dname "CN=Ajuworks, OU=Ajuworks, O=Ajuworks, L=, S=, C=JP"
```

### macOS / Linux

```bash
KS=~/.ajuworks-signing/ajuworks-upload-key.jks
[ -e "$KS" ] && { echo "すでに存在します。上書き禁止: $KS"; exit 1; }

keytool -genkeypair -v \
  -keystore "$KS" \
  -storetype PKCS12 \
  -alias ajuworks-upload \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -dname "CN=Ajuworks, OU=Ajuworks, O=Ajuworks, L=, S=, C=JP"
chmod 600 "$KS"
```

パスワードを聞かれます。**長くランダムなものを使い、パスワードマネージャーに保存してください。**
`-storetype PKCS12` なので keystore と鍵のパスワードは同一になります。
（GitHub Secrets には同じ値を 2 つ登録します。）

- `-keysize 4096` / `-keyalg RSA` — Play の upload key 要件を満たします
- `-validity 10950` — 約 30 年

> **バックアップ**: `ajuworks-upload-key.jks` とパスワードを、別媒体
> （暗号化した外付けドライブ、パスワードマネージャーの添付など）に控えてください。
> upload key を失うと Google へのリセット申請が必要になります。

---

## 3. 証明書情報を確認する（公開情報のみ）

```bash
keytool -list -v -keystore ~/.ajuworks-signing/ajuworks-upload-key.jks -alias ajuworks-upload
```

`SHA-256` / `SHA-1` / `有効期限` を控えておきます。**パスワードは控えない・貼らない。**

---

## 4. keystore を base64 にする

**Windows で `certutil -encode` は使わないでください。** `-----BEGIN CERTIFICATE-----`
のようなヘッダ行が付いて壊れます。以下を使ってください。

### Windows (PowerShell)

```powershell
$ks  = "$env:USERPROFILE\.ajuworks-signing\ajuworks-upload-key.jks"
$out = "$env:USERPROFILE\.ajuworks-signing\ajuworks-upload-key.b64"
[IO.File]::WriteAllText($out, [Convert]::ToBase64String([IO.File]::ReadAllBytes($ks)))
```

改行なしの 1 行で出力されます。

### macOS / Linux

```bash
base64 -w0 ~/.ajuworks-signing/ajuworks-upload-key.jks \
  > ~/.ajuworks-signing/ajuworks-upload-key.b64
# macOS の base64 に -w はありません:
# base64 -i ~/.ajuworks-signing/ajuworks-upload-key.jks | tr -d '\n' > ...b64
```

> workflow 側は `base64 -d` で復号します。改行が入っていても復号できますが、
> ヘッダ行が入っていると壊れます。

---

## 5. GitHub Secrets に登録する

https://github.com/atushi0095-del/life-simulator/settings/secrets/actions

**Repository secrets** に以下の 4 つを追加します（名前は workflow が期待しているものです）。

| Secret 名 | 値 |
|---|---|
| `WORKLOG_KEYSTORE_BASE64` | 手順 4 で作った `.b64` の中身 |
| `WORKLOG_KEYSTORE_PASSWORD` | keystore のパスワード |
| `WORKLOG_KEY_ALIAS` | `ajuworks-upload` |
| `WORKLOG_KEY_PASSWORD` | 鍵のパスワード（PKCS12 なので keystore と同じ） |

登録が終わったら `.b64` は消して構いません。

```powershell
Remove-Item "$env:USERPROFILE\.ajuworks-signing\ajuworks-upload-key.b64"
```

---

## 6. workflow を再実行する

https://github.com/atushi0095-del/life-simulator/actions/workflows/worklog-android.yml
→ **Run workflow** → ブランチ `claude/worklog-android-app-xyu3yf`

成功すると:

- artifact 名が **`worklog-signed-release`** になります
  （未署名のときは `worklog-release-UNSIGNED`）
- `Verify the bundle signature` ステップが `jarsigner -verify` と証明書の
  SHA-256 を出力します
- **secrets を入れたのに debug 署名になっていた場合、ジョブは失敗します。**
  署名できていないものが release として素通りしないようにしてあります

---

## 注意

- `.jks`・`.b64`・パスワードを **Git に commit しない**（`.gitignore` 済み）
- **debug ビルドには upload key を使わない。** debug は Android Debug Key のままです
- この鍵は **upload key** です。Google Play の **app signing key** とは別物で、
  app signing key は Play App Signing に管理させます（新規アプリ作成時の既定）
