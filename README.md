# Theta.Label.Hatch — 声でテプラ

KING JIM「テプラ」PRO **SR5900P** へ、Androidスマートフォンから**音声で**ラベルを印刷するアプリ。
部屋の整理中に「話す → 確認 → 1回押して印刷」だけでテープ幅に合ったラベルが出ることを目指しています。

- 最新版：[GitHub Releases](https://github.com/kiwiman0706-beep/Theta.Label.Hatch/releases)（`koe-de-tepra-1.0.0.apk`）
- Android 11以降 / package `jp.yaegaki.tepravoice` / 署名証明書 SHA-256 `61256bf3…f8ce2c79`
- **実機SR5900Pでの印刷は未検証です。** まず [実機テストチェックリスト](docs/handoff/REAL_DEVICE_TEST_CHECKLIST.md) を一巡してください。

## 最初に読むファイル

1. [docs/handoff/START_HERE.md](docs/handoff/START_HERE.md)：引き継ぎ資料の入口（原文のまま収録）
2. [docs/handoff/REQUIREMENTS_AND_CONCEPT.md](docs/handoff/REQUIREMENTS_AND_CONCEPT.md)：要望原文と受け入れ条件
3. [docs/handoff/IMPLEMENTATION_STATUS.md](docs/handoff/IMPLEMENTATION_STATUS.md)：実装と技術仕様
4. [docs/handoff/KNOWN_ISSUES_AND_NEXT_STEPS.md](docs/handoff/KNOWN_ISSUES_AND_NEXT_STEPS.md)：未検証点と次の作業
5. [docs/09_THETA_HATCH_PLAN.md](docs/09_THETA_HATCH_PLAN.md)：Theta.xxx.Hatch 規約に合わせる構想
6. [docs/08_SIGNING_KEY_POLICY.md](docs/08_SIGNING_KEY_POLICY.md)：署名鍵をこのリポジトリに置かない理由と更新APKの作り方

## 構成

| 場所 | 内容 |
| --- | --- |
| `releases/koe-de-tepra-1.0.0.apk` | 配布済みの署名済みAPK（バイト列無改変） |
| `source/koe-de-tepra/` | 1.0.0 のソース。SDK・鍵は含まない。`build-apk.sh` はパスワードを環境変数で受け取る |
| `docs/handoff/` | 前スレッドの引き継ぎ資料（原文）。中の `source-expanded/…` は本リポジトリでは `source/koe-de-tepra/…` |
| `docs/release-notes/` | GitHub Release 本文 |
| `.github/workflows/release-apk.yml` | APK の署名証明書・package・version を検証して Release を作成 |

## リリース手順（現状）

1. `releases/koe-de-tepra-<version>.apk` と `docs/release-notes/v<version>.md` をコミット
2. Actions → `release-apk` → Run workflow（`version` に `v<version>`）。`-` 付きはプレリリース

## 注意

- このリポジトリは公開です。**署名鍵（`release-signing.jks`）・`SIGNING.txt`・SDK バイナリはコミットしないでください**（`.gitignore` 済み）。
- Powerted by KING JIM TEPRA-Print SDK（SDK 指定表記）。KING JIM、テプラ、TEPRA は株式会社キングジムの商標または登録商標です。
