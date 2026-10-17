# 声でテプラ — 引き継ぎ開始地点

このフォルダーは、KING JIM「テプラ」PRO **SR5900P** 用Androidアプリ「声でテプラ」v1.0.0を、別スレッド・別担当者へ引き継ぐための完全版パッケージです。

## 最初に読む順番

1. `REQUIREMENTS_AND_CONCEPT.md` — ユーザーの要望原文とアプリの狙い
2. `IMPLEMENTATION_STATUS.md` — 何が実装済みか、構成と仕様
3. `KNOWN_ISSUES_AND_NEXT_STEPS.md` — 未検証事項と次に行う作業
4. `REAL_DEVICE_TEST_CHECKLIST.md` — SR5900P実機での確認手順
5. `AI_HANDOFF_PROMPT.md` — 次のAIスレッドへ貼り付ける依頼文

## 収録内容

- `original-deliverables/`
  - インストール可能なv1.0.0 APK
  - 元の更新用ソースZIP
  - 元成果物のSHA-256一覧
- `source-expanded/`
  - 更新用ソースZIPを展開したもの
  - APKの上書き更新に必要な署名鍵と署名情報
- この引き継ぎ用ドキュメント一式

## 重要な状態

- 対象プリンターは **SR5900Pのみ** です。SR-R2500P / SR-R5600P向けの別プロジェクトを混ぜないでください。
- APKは完成済みですが、**実機SR5900Pとの最終通信・印刷は未検証**です。
- ソースだけから再ビルドするには、KING JIM公式の TEPRA-Print SDK for Android v1.4.0 を別途取得して配置します。
- `source-expanded/release-signing.jks` は今後の上書き更新に必須の秘密鍵です。

## セキュリティ上の注意

このパッケージにはAPK署名用の秘密鍵とパスワード情報が含まれます。公開リポジトリ、掲示板、不特定多数が閲覧できるクラウドへ置かないでください。署名鍵を失うと同じアプリとして上書き更新できず、漏えいすると第三者が同じ署名のAPKを作れる可能性があります。

## 現在の成果物

- アプリ名: 声でテプラ
- パッケージ名: `jp.yaegaki.tepravoice`
- バージョン: `1.0.0`（versionCode `10000`）
- Android: 11以上（minSdk 30 / targetSdk 35）
- APK: `original-deliverables/koe-de-tepra-1.0.0.apk`

