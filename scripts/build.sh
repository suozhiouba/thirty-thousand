#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd -W)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${LOCALAPPDATA//\\//}/Android/Sdk}"
BUILD_TOOLS="$SDK_ROOT/build-tools/36.0.0"
ANDROID_JAR="$SDK_ROOT/platforms/android-36/android.jar"
OUT="$PROJECT_ROOT/build"
mkdir -p "$OUT/generated" "$OUT/classes" "$OUT/dex" "$PROJECT_ROOT/deliverables"
"$BUILD_TOOLS/aapt2.exe" compile --dir "$PROJECT_ROOT/app/src/main/res" -o "$OUT/resources.zip"
sed 's/<manifest /<manifest package="com.thirtythousand.app" /' "$PROJECT_ROOT/app/src/main/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2.exe" link -I "$ANDROID_JAR" --manifest "$OUT/AndroidManifest.xml" --java "$OUT/generated" --min-sdk-version 26 --target-sdk-version 35 --version-code 1 --version-name 1.0.0 -o "$OUT/resources.apk" "$OUT/resources.zip"
find "$PROJECT_ROOT/app/src/main/java" "$OUT/generated" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR" -d "$OUT/classes" "@$OUT/sources.txt"
jar cf "$OUT/classes.jar" -C "$OUT/classes" .
java -cp "$BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --lib "$ANDROID_JAR" --min-api 26 --output "$OUT/dex" "$OUT/classes.jar"
cp "$OUT/resources.apk" "$OUT/unsigned.apk"
jar uf "$OUT/unsigned.apk" -C "$OUT/dex" classes.dex
"$BUILD_TOOLS/zipalign.exe" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
KEYSTORE="$PROJECT_ROOT/build/development.keystore"
if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=ThirtyThousand Development,O=Local,C=CN'
fi
java -jar "$BUILD_TOOLS/lib/apksigner.jar" sign --ks "$KEYSTORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$PROJECT_ROOT/deliverables/thirty-thousand-1.0.0.apk" "$OUT/aligned.apk"
java -jar "$BUILD_TOOLS/lib/apksigner.jar" verify --verbose "$PROJECT_ROOT/deliverables/thirty-thousand-1.0.0.apk"
"$BUILD_TOOLS/aapt.exe" dump badging "$PROJECT_ROOT/deliverables/thirty-thousand-1.0.0.apk" | head -10
