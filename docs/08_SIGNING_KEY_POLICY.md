# 署名鍵の扱い（このリポジトリには入れていません）

このリポジトリは**公開**です。そのため引き継ぎZIPに含まれていた次のファイルは、意図的にコミットしていません。

| 除外したもの | 理由 |
| --- | --- |
| `release-signing.jks` | APK署名用の秘密鍵。漏えいすると第三者が同じ署名のAPKを作れる |
| `SIGNING.txt` | 鍵のエイリアスとパスワード |
| `koe-de-tepra-update-kit-1.0.0.zip` | 上記の鍵とパスワードを含む |
| `build-apk.sh` 内のパスワード | 環境変数 `KEYSTORE_PASSWORD` で渡す形に変更 |

## 原本の保管

- 原本は引き継ぎZIP `koe-de-tepra-complete-handoff-20261017.zip` の `source-expanded/` にあります。
- **鍵を失うと既存インストールへ上書き更新できなくなります。** ZIP をパスワードマネージャーや個人用の非公開ストレージなど、2か所以上にバックアップしてください。
- 証明書 SHA-256（公開して問題ない値）: `61256bf31586fd7c266a11c0de663c515fbbdb0667970ad833e62c14f8ce2c79`

## 手元で更新APKを作る

```bash
cd source/koe-de-tepra
cp /path/to/handoff/source-expanded/release-signing.jks .   # .gitignore 済み
export ANDROID_SDK_ROOT=/path/to/android-sdk
export KEYSTORE_PASSWORD='（SIGNING.txt の値）'
./build-apk.sh
```

鍵ファイルが無い場合、スクリプトは新しい鍵を作らずに停止します（別署名のAPKを誤って配らないため）。

## CI で署名する場合（構想 Phase 1）

GitHub の Settings → Secrets and variables → Actions に次を登録します（値はリポジトリに書かない）。

- `LABEL_KEYSTORE_B64`：`base64 -w0 release-signing.jks` の出力（Mac は `base64 -i release-signing.jks`）
- `LABEL_KEYSTORE_PASSWORD`、`LABEL_KEY_ALIAS`
