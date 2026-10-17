# 声でテプラ

「テプラ」PRO SR5900Pへ、Androidスマートフォンから音声でラベルを印刷する個人用アプリです。

## 主な機能

- 日本語の音声入力（端末に設定されたAndroid音声認識サービスを使用）
- 認識内容を大きく表示し、修正・確認後に印刷
- 印刷枚数1〜99枚（初期値1枚）
- 装着テープ幅（4/6/9/12/18/24/36mm）をSR5900Pから自動取得
- テープ幅と文字量に応じた、文字サイズ・1行/2行・ラベル長の自動調整
- 最近印刷した8件を端末内に保存して再利用
- エラー内容、印刷進行状況、装着テープ幅を日本語で表示

## 動作条件

- Android 11以降
- KING JIM「テプラ」PRO SR5900P
- スマートフォンとSR5900Pが同じWi-Fiネットワークに接続されていること

## 初回の使い方

1. アプリ右上の「設定する」を押します。
2. Wi-Fi機器の検索権限を許可します。
3. 見つかったSR5900Pをタップします。
4. 大きなマイクを押し、ラベルにしたい言葉を話します。
5. 表示内容と枚数を確認し、「この内容で印刷」を押します。

## ビルド

KING JIM公式サイトで使用許諾契約に同意し、TEPRA-Print SDK for Android v1.4.0を取得します。次を配置してください。

- `app/libs/TepraPrint.jar`
- `app/src/main/jniLibs/arm64-v8a/libTepraPrint.so`
- `app/src/main/jniLibs/armeabi-v7a/libTepraPrint.so`
- `app/src/main/jniLibs/x86/libTepraPrint.so`
- `app/src/main/jniLibs/x86_64/libTepraPrint.so`

Android SDK Platform 35 / Build Tools 35.0.0とJDK 17を用意し、次を実行します。

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build-apk.sh
```

SDK: https://www.kingjim.co.jp/download/tepra/sdk/

## プライバシー

ラベル履歴とプリンター情報は端末内にだけ保存します。本アプリ自身はラベル内容を外部サーバーへ送信しません。音声認識については、端末で選択されている音声認識サービスの設定に従います。

## SDK表示

Powerted by KING JIM TEPRA-Print SDK

TEPRA-Print SDKの再頒布対象モジュールは、KING JIMの使用許諾条件に従ってAPKへ未改変で組み込んでいます。日本国内で使用してください。
