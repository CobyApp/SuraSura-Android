# SuraSura Android

リアルタイム音声翻訳アプリ（Android 版）。マイクから取り込んだ音声をストリーミングで文字起こしし、対向言語へ翻訳して読み上げます。対面会話を想定した上下 2 パネル構成の UI を備えています。

## 主な機能

- **ストリーミング音声認識**: Google Cloud Speech-to-Text v1（gRPC）でインタリム + final を逐次受信
- **自動翻訳**: 認識結果を Google Cloud Translation v2（REST）で翻訳。final 直後は短いデバウンス、interim のみの間は長めのデバウンスで反映
- **読み上げ**: Google Cloud Text-to-Speech v1（REST）。言語ごとの推奨性別（FEMALE/NEUTRAL）を内蔵
- **対面モード**: 上下パネルを反転表示し、向かい合った 2 人がそれぞれのマイクで話せる構成
- **拡大表示**: 各パネルをフルスクリーン化して大きく見せる／読み上げる
- **多言語対応**: 21 言語（日本語・英語・韓国語・中国語簡体/繁体・スペイン語・フランス語・ドイツ語・イタリア語・ポルトガル語・ロシア語・アラビア語・オランダ語・トルコ語・ベトナム語・インドネシア語・タイ語・ポーランド語・ヒンディー語・スウェーデン語・ネパール語）
- **設定の永続化**: 選択した上下言語を DataStore で保存

## 技術スタック

| レイヤ | 採用技術 |
|---|---|
| 言語 | Kotlin 2.2.21 |
| UI | Jetpack Compose（BOM 2025.01）, Material 3, Navigation Compose |
| DI | Hilt 2.59 + KSP |
| 非同期 | Kotlin Coroutines / Flow |
| ネットワーク | gRPC 1.71（Speech）/ REST（Translation, TTS） |
| ビルド | AGP 9.1 / Gradle Version Catalog |
| 最低 SDK | API 26（Android 8.0） |
| ターゲット SDK | API 35 |

## プロジェクト構成

```
app/src/main/java/com/coby/surasura/
├── MainActivity.kt
├── SuraSuraApp.kt              # Hilt エントリポイント
├── data/
│   ├── client/                 # Google Cloud クライアント
│   │   ├── GoogleSpeechClient.kt
│   │   ├── GoogleTranslationClient.kt
│   │   ├── GoogleTTSClient.kt
│   │   └── GoogleCloudGrpcApiKeyInterceptor.kt
│   ├── model/                  # AppState / SupportedLanguage / SttStreamSegment
│   └── prefs/                  # LanguagePreferenceStore (DataStore)
└── ui/
    ├── home/
    │   ├── HomeScreen.kt
    │   ├── HomeViewModel.kt    # STT / 翻訳 / TTS の統合状態管理
    │   └── components/         # MicButton, ExpandedTextView, LanguagePickerOverlay
    └── theme/                  # Color / Theme / Type
```

## セットアップ

### 1. Google Cloud API キーの準備

以下の API を有効化したプロジェクトで API キーを発行してください。

- Cloud Speech-to-Text API
- Cloud Translation API
- Cloud Text-to-Speech API

API キー制限は **Android アプリ** で `applicationId = com.coby.surasura` と署名 SHA-1 を登録します。
※ debug ビルドでも `applicationIdSuffix` を付けていません。GCP の Android キー制限が applicationId 完全一致のため、suffix を付けると `PERMISSION_DENIED` の原因になります。

### 2. ローカル設定ファイル

リポジトリ直下の `local.properties.example` を `local.properties` にコピーし、キーを記入します。

```properties
GOOGLE_CLOUD_API_KEY=AIza...your_key...
# 任意: 将来の GCP 機能用
# GOOGLE_CLOUD_PROJECT_ID=your-project-id
```

環境変数 `GOOGLE_CLOUD_API_KEY` または Gradle プロパティでも注入可能です。

### 3. ビルド & 実行

```bash
./gradlew installDebug
```

または Android Studio から `app` をそのまま実行してください。

## 必要権限

`AndroidManifest.xml` で以下を要求します。`RECORD_AUDIO` は実行時許可が必要です。

- `android.permission.INTERNET`
- `android.permission.RECORD_AUDIO`
- `android.permission.MODIFY_AUDIO_SETTINGS`

## 設計メモ

- **マイクと翻訳方向**: `activeMic` が `BOTTOM` のとき bottomLanguage を認識して topLanguage に翻訳、`TOP` のときはその逆。
- **gRPC / protobuf の整合性**: Speech 4.x と Translate/TTS 2.x で protobuf のマイナーが異なると初回 RPC で `LinkageError` になるため、`protobuf-java` を 3.25.3 に固定しています（`app/build.gradle.kts` の `resolutionStrategy`）。
- **AudioRecord の二重起動防止**: マイク再起動時は前回ジョブを `cancelAndJoin` で完全に解放してから次の `startStreaming` を開始します。
- **無音の自動停止**: STT 側のサイレンス検知でセッションを自動終了し、停止時に未翻訳の最終テキストを翻訳・必要なら読み上げます。
- **META-INF 重複**: gRPC/protobuf JAR の `INDEX.LIST` / `DEPENDENCIES` / `io.netty.versions.properties` を `pickFirsts` でマージ。

## ライセンス

社内利用。外部公開時は別途ライセンスを設定してください。
