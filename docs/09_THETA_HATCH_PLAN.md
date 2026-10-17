# Theta.xxx.Hatch 要件に合わせる次の構想（案）

作成：2026-10-17。対象：声でテプラ 1.0.0（versionCode 10000）を Theta.Label.Hatch リポジトリで継続開発するための構想。
**これは提案です。** 確定要望は `docs/handoff/REQUIREMENTS_AND_CONCEPT.md` の原文だけです。

## 1. 「Theta.xxx.Hatch」の要件をどう読み取ったか

明文化された要件書は見つからなかったため、同シリーズの先行作 **DeskHatch**（`Theta.DeskHatch`）と、
同日に引き継いだ **Theta.Item.Hatch** の `docs/09_THETA_HATCH_PLAN.md` で抽出された共通規約に揃えた。

| # | シリーズ共通の規約 | Theta.Label.Hatch での要件（案） | 1.0.0 の現状 |
| --- | --- | --- | --- |
| T1 | 識別子 `Theta.<Name>Hatch`、Publisher `Theta` | 表示名・識別子を `Theta.LabelHatch` 系に揃える | 「声でテプラ」、package `jp.yaegaki.tepravoice` |
| T2 | 公開リポジトリ＋GitHub Releases で配布 | APK を GitHub Releases で配布 | **v1.0.0 で対応**（release-apk ワークフロー） |
| T3 | タグ or 手動実行で Actions がビルド→Release | CI でビルド・署名・公開まで自動化 | 署名済みAPKの検証・公開のみ |
| T4 | stable／beta（`-beta` タグ＝プレリリース） | 同じ2チャネル運用 | ワークフローは `-` 付きタグをプレリリース扱い済み。運用は未開始 |
| T5 | アプリ内自動更新 | Releases を確認→取得→インストール | 未対応 |
| T6 | パッケージマネージャー登録 | Android 相当：Obtainium 対応 | 未対応 |
| T7 | 日本語／英語 UI | 日英 UI | 日本語のみ |
| T8 | README（バッジ・手順）、CHANGELOG、PUBLISHING.md | 同等の文書一式 | README・CHANGELOG のみ |
| T9 | プライバシーポリシー、GitHub Pages LP・デモ | 同等。デモはブラウザ版ラベルプレビュー | 未対応 |
| T10 | 単体＋E2E テストを CI で実行 | レイアウト計算の単体テスト＋ビルドを CI 化 | テストなし |
| T11 | MIT License | ライセンス明示（SDK 部分は KING JIM 許諾で別扱い） | 未設定 |
| T12 | 署名鍵を Secrets で受けて CI 署名 | 同左 | **鍵は手元にある**（引き継ぎZIP）。Secrets 未登録 |

### Item.Hatch との大きな違い
- **署名鍵が残っている。** 既存インストールへ上書き更新できるので、package `jp.yaegaki.tepravoice` は維持し、表示名だけ変える方針が取れる。
- **KING JIM TEPRA-Print SDK が公開リポジトリに置けない。** CI ビルドには SDK の入手経路を別に用意する必要がある（下記 Phase 1）。
- **実機 SR5900P 未検証。** Theta 化より先に、実機で「話す→確認→印刷」が通ることを確定させる必要がある。

## 2. 最初に決めること（ユーザー判断が必要）

1. **実機テストを先にするか。** 推奨は「はい」。v1.0.0 APK で `REAL_DEVICE_TEST_CHECKLIST.md` を一巡し、結果を Issue 化してから Phase 1 以降に進む。
2. **表示名。** 例：「Label Hatch（声でテプラ）」。「テプラ」はキングジムの登録商標なので、公開配布するアプリ名の主語にせず「for TEPRA PRO SR5900P」のような対応機種表記に留める案を推奨。package は維持。
3. **SDK を CI に渡す方法。**
   - 案A（推奨）：非公開リポジトリ（例 `Theta.Label.Hatch-sdk`）に SDK を置き、読み取り専用トークン（Secrets）で CI から取得。
   - 案B：CI では SDK 無しの「コンパイル確認のみ」とし、署名済みAPKは手元ビルド→`releases/` にコミット→現行ワークフローで公開。
4. **ライセンス**（アプリ自身のコードを MIT にするか）。SDK は KING JIM の許諾が優先し、日本国内利用前提の注記を残す。

## 3. 段階的な構想

### Phase 0 — 済（今回）
- 引き継ぎ一式を登録（`docs/handoff/`、`source/koe-de-tepra/`）。鍵・パスワード・鍵入りZIPは除外し、`build-apk.sh` のパスワードを環境変数化。
- `release-apk` ワークフロー：同梱APKを apksigner で検証し、証明書 SHA-256 と package 名・versionName が一致する時だけ Release を作る。
- v1.0.0 APK と SHA256 を GitHub Release として公開。

### Phase 1 — 実機確定と CI 基盤（T3, T4, T10, T12）
- 実機チェックリストを実施し、SDK・mDNS・テープ幅取得の不具合を修正（1.0.x）。
- **Gradle 化**（Android Studio で開けるように）。`build-apk.sh` は互換のため当面残す。
- `MainActivity` のレイアウト計算（フォントサイズ・行数・長さ決定）を純粋関数クラスへ切り出し、JUnit で 7 テープ幅×推奨文字列を検証。
- `build.yml`：push／PR ごとにビルド・単体テスト・lint。
- `release.yml`：Secrets `LABEL_KEYSTORE_B64` 等から鍵を復元し、ビルド→署名→証明書ピン留め確認→Release。versionCode はタグから算出（`1.1.0` → 10100、現行 10000 より必ず大きい）。`v1.1.0-beta.1` はプレリリース。

### Phase 2 — 配布と更新（T5, T6）
- 設定に「アップデートを確認」：GitHub API `releases/latest`（beta 選択時は prerelease 含む）→ versionCode 比較 → APK ダウンロード → SHA-256 検証 → `PackageInstaller`（`REQUEST_INSTALL_PACKAGES`）。自動確認は1日1回・オフ可。
- Obtainium 用に APK 命名規則を固定（`LabelHatch-x.y.z.apk` へ移行するなら CHANGELOG に明記）し、README に追加手順を書く。
- Google Play は SDK の国内限定条件・審査要件を確認したうえで別途判断。

### Phase 3 — Theta ブランドと文書（T1, T8, T9, T11）
- README をシリーズ形式に：ロゴ、バッジ（Release／Downloads／License）、スクリーンショット、インストール手順、対応機種。
- `docs/PUBLISHING.md`（リリース手順・Secrets 登録）、`docs/privacy.html`（ラベル履歴は端末内のみ、音声は端末の認識サービス設定に従う、外部送信なし）。
- GitHub Pages：LP ＋「テープ幅を選んで文字を入れるとラベル見本が出る」ブラウザ版プレビュー（Phase 1 で切り出したレイアウト規則を JS に移植）。

### Phase 4 — 使い勝手の拡張（元要望の範囲内を優先）
- 認識候補（最大3件）を選べるチップ表示。
- 日英 UI（`values-en/`）。ただし SDK は日本国内利用前提のため、英語は在日ユーザー向けの位置付け。
- 接続診断画面とログ書き出し（実機不具合の報告を楽にする）。
- お気に入り・カテゴリ、文字揃え・縦書きなどは要望ではないので、Phase 1 の実機結果を見てから。

## 4. 回帰させないこと（Theta 化でも維持）
「話す→確認→1回押して印刷」の最短フロー、初期枚数1・1〜99枚、空欄で印刷不可、テープ幅自動フィット、SR5900P のみ対象（SR-R2500P / SR-R5600P を混ぜない）、package と署名鍵の継続、`Powerted by KING JIM TEPRA-Print SDK` 表記、鍵・SDK バイナリを公開リポジトリに入れないこと。
