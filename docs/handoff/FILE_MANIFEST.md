# ファイル一覧

## 引き継ぎ資料

| ファイル | 内容 |
|---|---|
| `START_HERE.md` | 読み始める場所と重要注意 |
| `AI_HANDOFF_PROMPT.md` | 新しいAIスレッドへ貼り付ける依頼文 |
| `REQUIREMENTS_AND_CONCEPT.md` | 要望原文、必須要件、UXコンセプト、受け入れ条件 |
| `IMPLEMENTATION_STATUS.md` | 実装済み機能、構成、ビルド、署名、検証値 |
| `DEVELOPMENT_HISTORY.md` | 要望からv1.0.0・引き継ぎ作成までの経緯 |
| `KNOWN_ISSUES_AND_NEXT_STEPS.md` | 未検証点、制約、次の優先作業 |
| `REAL_DEVICE_TEST_CHECKLIST.md` | SR5900P実機での確認手順 |
| `FILE_MANIFEST.md` | この一覧 |
| `HANDOFF_SHA256SUMS.txt` | ZIP内の主要バイナリとソースファイルのSHA-256 |

## 元の納品物

| ファイル | 内容 |
|---|---|
| `original-deliverables/koe-de-tepra-1.0.0.apk` | 端末へインストールする署名済みAPK |
| `original-deliverables/koe-de-tepra-update-kit-1.0.0.zip` | 当初の更新用ソース・署名鍵一式 |
| `original-deliverables/SHA256SUMS-koe-de-tepra-1.0.0.txt` | 上記APKと更新用ZIPの元チェックサム |

## 展開済みソース

`source-expanded/` は更新用ZIPの展開版です。Javaソース、AndroidManifest、リソース、ビルドスクリプト、README、第三者通知、更新署名用キーストアを含みます。

KING JIM公式SDKの `TepraPrint.jar` と `libTepraPrint.so` は含みません。再ビルド時に公式配布元から取得してください。コンパイル済みAPKには、許諾条件に従って必要な再頒布対象モジュールが組み込まれています。

## 秘密情報

`source-expanded/release-signing.jks` と `source-expanded/SIGNING.txt` は公開禁止です。このZIP全体を秘密情報として扱ってください。

