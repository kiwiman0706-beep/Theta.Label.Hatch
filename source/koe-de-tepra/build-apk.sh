#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$PROJECT_DIR/../android-sdk}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-35.0.0}"
PLATFORM_VERSION="${PLATFORM_VERSION:-35}"
BUILD_TOOLS="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-$PLATFORM_VERSION/android.jar"
TEPRA_JAR="$PROJECT_DIR/app/libs/TepraPrint.jar"
TEMP_DIR="$(mktemp -d)"
OUTPUT_DIR="$PROJECT_DIR/dist"

# 署名鍵とパスワードは公開リポジトリに置かない。手元の引き継ぎZIP（SIGNING.txt）の値を環境変数で渡す。
KEYSTORE="${KEYSTORE:-$PROJECT_DIR/release-signing.jks}"
KEY_ALIAS="${KEY_ALIAS:-tepra-voice}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:?KEYSTORE_PASSWORD を設定してください（SIGNING.txt 参照）}"

if [[ ! -f "$ANDROID_JAR" || ! -x "$BUILD_TOOLS/aapt2" ]]; then
  echo "Android SDK platform $PLATFORM_VERSION / build-tools $BUILD_TOOLS_VERSION が必要です。" >&2
  exit 1
fi
if [[ ! -f "$TEPRA_JAR" ]]; then
  echo "公式SDKの TepraPrint.jar を app/libs に配置してください。" >&2
  exit 1
fi

cleanup() {
  if command -v trash-put >/dev/null 2>&1; then
    trash-put "$TEMP_DIR"
  else
    rm -rf -- "$TEMP_DIR"
  fi
}
trap cleanup EXIT

mkdir -p "$TEMP_DIR/gen" "$TEMP_DIR/classes" "$TEMP_DIR/dex" "$OUTPUT_DIR"

"$BUILD_TOOLS/aapt2" compile \
  --dir "$PROJECT_DIR/app/src/main/res" \
  -o "$TEMP_DIR/resources.zip"

"$BUILD_TOOLS/aapt2" link \
  -o "$TEMP_DIR/base.apk" \
  --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  --min-sdk-version 30 \
  --target-sdk-version 35 \
  --version-code 10000 \
  --version-name 1.0.0 \
  --java "$TEMP_DIR/gen" \
  "$TEMP_DIR/resources.zip"

find "$PROJECT_DIR/app/src/main/java" "$TEMP_DIR/gen" -name '*.java' -print > "$TEMP_DIR/java-sources.txt"
java -m jdk.compiler/com.sun.tools.javac.Main \
  -encoding UTF-8 \
  -source 11 -target 11 \
  -classpath "$ANDROID_JAR:$TEPRA_JAR" \
  -d "$TEMP_DIR/classes" \
  @"$TEMP_DIR/java-sources.txt"

java -m jdk.jartool/sun.tools.jar.Main \
  --create --file "$TEMP_DIR/app-classes.jar" \
  -C "$TEMP_DIR/classes" .
"$BUILD_TOOLS/d8" \
  --min-api 30 \
  --lib "$ANDROID_JAR" \
  --output "$TEMP_DIR/dex" \
  "$TEMP_DIR/app-classes.jar" "$TEPRA_JAR"

cp "$TEMP_DIR/base.apk" "$TEMP_DIR/unsigned-unaligned.apk"
(
  cd "$TEMP_DIR/dex"
  zip -q "$TEMP_DIR/unsigned-unaligned.apk" classes.dex
)
mkdir -p "$TEMP_DIR/lib"
cp -R "$PROJECT_DIR/app/src/main/jniLibs/." "$TEMP_DIR/lib/"
(
  cd "$TEMP_DIR"
  zip -q -r "$TEMP_DIR/unsigned-unaligned.apk" lib
)

"$BUILD_TOOLS/zipalign" -P 16 -f 4 \
  "$TEMP_DIR/unsigned-unaligned.apk" \
  "$TEMP_DIR/aligned.apk"

# 鍵が無いときに新しい鍵を自動生成すると、既存インストールへ上書きできないAPKができてしまうため止める。
if [[ ! -f "$KEYSTORE" ]]; then
  echo "署名鍵 $KEYSTORE がありません。引き継ぎZIPの release-signing.jks を配置してください。" >&2
  exit 1
fi

FINAL_APK="$OUTPUT_DIR/koe-de-tepra-1.0.0.apk"
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASSWORD" \
  --key-pass "pass:$KEYSTORE_PASSWORD" \
  --out "$FINAL_APK" \
  "$TEMP_DIR/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$FINAL_APK"
echo "生成完了: $FINAL_APK"
