# 実装状況と技術仕様

## 現在地

v1.0.0のAPKと更新用ソースは作成済みです。APKの構造・署名・収録ABIは確認済みですが、実機SR5900Pとの最終通信および実印刷は未検証です。

## 実装済み機能

| 機能 | 状態 | 実装概要 |
|---|---|---|
| 日本語音声入力 | 実装済み | Android `RecognizerIntent`、言語は日本語、候補最大3件 |
| 認識結果の確認・編集 | 実装済み | 大きな編集欄へ反映し、手入力修正可能 |
| 印刷枚数 | 実装済み | 1〜99枚、初期値1枚 |
| SR5900P検索 | 実装済み | `_pdl-datastream._tcp.local.` を検索し、SR5900Pへ絞り込み |
| プリンター選択の保存 | 実装済み | SharedPreferencesへ保存 |
| テープ幅自動取得 | 実装済み | 4 / 6 / 9 / 12 / 18 / 24 / 36mmに対応 |
| ラベル自動レイアウト | 実装済み | フォントサイズ、1行/2行、長さ28〜120mmを自動調整 |
| 複数枚印刷 | 実装済み | SDKの印刷枚数指定を使用 |
| カット設定 | 実装済み | ラベルごとにカット、対応時はハーフカット |
| 最近のラベル | 実装済み | 直近8件を端末内へ保存し再利用 |
| 日本語ステータス表示 | 実装済み | 接続、印刷中、完了、代表的エラーを表示 |
| 権限対応 | 実装済み | Android 12以前の位置情報、Android 13以降の近隣Wi-Fi機器 |
| APK署名 | 実装済み | 専用キーストアで署名。更新時も同じ鍵が必要 |
| SR5900P実機テスト | 未実施 | `REAL_DEVICE_TEST_CHECKLIST.md` を参照 |

## アプリ情報

- アプリ名: `声でテプラ`
- applicationId / package: `jp.yaegaki.tepravoice`
- versionName: `1.0.0`
- versionCode: `10000`
- minSdk: `30`（Android 11）
- targetSdk: `35`
- 画面方向: 縦固定
- 対応ABI: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

## ソース構成

| ファイル | 役割 |
|---|---|
| `source-expanded/app/src/main/java/jp/yaegaki/tepravoice/MainActivity.java` | メインUI、音声入力、履歴、ラベル画像生成、印刷、状態表示 |
| `source-expanded/app/src/main/java/jp/yaegaki/tepravoice/PrinterSearchActivity.java` | SR5900P検索、選択、接続情報の保存 |
| `source-expanded/app/src/main/AndroidManifest.xml` | SDKレベル、権限、Activity定義 |
| `source-expanded/app/src/main/res/` | アイコン、色、文字列、テーマ |
| `source-expanded/build-apk.sh` | aapt2 / javac / d8 / zipalign / apksignerによるビルド |
| `source-expanded/release-signing.jks` | 同じアプリとして更新するための秘密署名鍵 |
| `source-expanded/SIGNING.txt` | 署名鍵のエイリアスとパスワード |
| `source-expanded/THIRD_PARTY_NOTICES.md` | KING JIM SDKに関する表示 |

Gradleプロジェクトではなく、JDKとAndroid SDKコマンドラインツールを直接使う小規模構成です。

## 印刷処理の仕様

- 選択済みプリンターへネットワーク接続する。
- プリンター状態と装着テープ幅を取得する。
- 入力文字列からラベル用Bitmapを生成する。
- 印刷枚数、ラベルごとのカット、高速印刷、濃度0、取得したテープ幅をSDKへ設定する。
- ハーフカット対応機ではハーフカットを有効にする。
- SDKの結果を日本語メッセージへ変換する。

代表的なエラーとして、接続失敗、テープなし、カバー開放、テープ切れ、カッター異常、プリンター使用中、ファームウェア更新中、過熱を扱います。

## 音声・データ・プライバシー

- 音声認識は端末に設定されたAndroid音声認識サービスへ委ねます。
- 本アプリ自身はラベル内容を独自サーバーへ送信しません。
- ラベル履歴とプリンター情報はSharedPreferencesで端末内だけに保存します。
- 音声データの扱いは、端末で選択された音声認識サービスの設定に従います。

## ビルド要件

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0
- KING JIM TEPRA-Print SDK for Android v1.4.0

公式SDKは使用許諾への同意後に取得し、次へ配置します。

```text
source-expanded/app/libs/TepraPrint.jar
source-expanded/app/src/main/jniLibs/arm64-v8a/libTepraPrint.so
source-expanded/app/src/main/jniLibs/armeabi-v7a/libTepraPrint.so
source-expanded/app/src/main/jniLibs/x86/libTepraPrint.so
source-expanded/app/src/main/jniLibs/x86_64/libTepraPrint.so
```

SDK入手先: https://www.kingjim.co.jp/download/tepra/sdk/

配置後のビルド例:

```bash
cd source-expanded
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build-apk.sh
```

生成物は `source-expanded/dist/koe-de-tepra-1.0.0.apk` です。

## 署名情報

更新APKは必ず `source-expanded/release-signing.jks` の同じ鍵で署名してください。具体的なエイリアスとパスワードは `source-expanded/SIGNING.txt` にあります。これを変更・紛失すると、既存インストールへ上書き更新できません。

## 既存成果物の検証値

```text
2497668a10dee6d630c678b277d77d8b2cd5b7e4ebf56a9bc9dd3d8f3b2b368e  koe-de-tepra-1.0.0.apk
482b601d245829846f766a4f43e97da878122bf1d2efc2188d060ba7ce05929a  koe-de-tepra-update-kit-1.0.0.zip
```

APK署名証明書SHA-256:

```text
61256bf31586fd7c266a11c0de663c515fbbdb0667970ad833e62c14f8ce2c79
```

