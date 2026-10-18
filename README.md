# GemOff 💎

**GemOff** is a fully offline AI chat assistant for Android. It runs Google's
**Gemma** language models **entirely on your device** using the MediaPipe LLM
Inference API. No internet, no account, no telemetry — the app declares **zero
Android permissions**.

> "GemOff" = **Gem**ma + **Off**line.

## Features

- 💬 Multi-turn chat with streaming responses
- 🔒 100% on-device inference — nothing ever leaves your phone
- 🚫 Zero permissions declared in the manifest (great for Play Store review)
- 📁 One-time model import: pick a `.task` / `.litertlm` file from storage
- 🌓 Light/dark Material 3 UI
- 🧹 Fresh-session button to reset the model's context window

## How it works

GemOff uses
[MediaPipe GenAI LLM Inference](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android)
(`com.google.mediapipe:tasks-genai`) to run quantized Gemma models on the
device CPU/GPU. A `LlmInferenceSession` keeps the conversation in the model's
context window, so follow-up questions work like a real assistant.

```
User prompt → LlmInferenceSession.addQueryChunk()
           → session.generateResponseAsync()  → streamed to the chat UI
```

## Getting a model

The model is far too large to bundle in an APK, so the user imports it once:

1. Download a MediaPipe/LiteRT-compatible Gemma model, for example
   **Gemma 3 1B (int4)** or **Gemma 3n E2B** in `.task` / `.litertlm` format
   from the [LiteRT Community on Hugging Face](https://huggingface.co/google)
   (search for "litert gemma").
2. Copy it to the phone (USB, cloud drive, browser download, ...).
3. In GemOff, tap **Import model file** and pick it.

For quick testing you can also push a model via adb:

```bash
adb shell mkdir -p /data/local/tmp/llm/
adb push gemma-3-1b.task /data/local/tmp/llm/gemma.task
```

## Building

Requirements: **Android Studio Ladybug+**, **JDK 17 to 25** (the Gradle/AGP toolchain runs fine on JDK 25; Studio's bundled JDK also works),
an arm64 device or emulator with at least 4 GB RAM (MediaPipe GenAI targets
high-end devices such as Pixel 8 / Galaxy S23 class; emulators are unreliable).

```bash
git clone https://github.com/sofoniasjohn8-a11y/GemOff.git
# Open in Android Studio, let it sync, press Run
# or from the command line:
./gradlew :app:assembleDebug
```

Release build (signed AAB for Play Store):

```bash
./gradlew :app:bundleRelease
```

## Project structure

```
app/src/main/java/com/sofonias/gemoff/
├── MainActivity.kt          # Compose entry point
├── ChatViewModel.kt        # State holder: model status, messages, streaming
├── llm/
│   ├── InferenceEngine.kt  # MediaPipe LlmInference + session wrapper
│   └── ModelStore.kt       # Model file import/copy/persist
└── ui/
    ├── GemOffApp.kt        # Screen routing (setup vs. chat)
    ├── ChatScreen.kt      # Chat UI (Material 3, streaming bubbles)
    ├── SetupScreen.kt     # First-run model import flow
    └── Theme.kt           # Gem purple light/dark theme
```

## Publishing to Google Play — checklist

1. **Play Console account** — one-time $25 fee at
   [play.google.com/console](https://play.google.com/console).
2. **Signing** — generate a release keystore and add a `keystore.properties`
   (kept out of git!) + signing config in `app/build.gradle.kts`, then
   `./gradlew bundleRelease` to produce an `.aab`.
3. **Store listing** — app icon (512×512), feature graphic, screenshots,
   description. Tagline suggestion: *"Your AI. Your phone. No internet."*
4. **Data safety form** — easy: GemOff collects **no data** and needs **no
   permissions**. You still need a privacy policy page (it can be short —
   state that all processing is on-device).
5. **Model licensing** — the Gemma models are governed by Google's
   [Gemma Terms of Use](https://ai.google.dev/gemma/terms); distributing an
   app that lets users side-load the model keeps you out of bundling issues,
   but review the terms and the
   [Generative AI Prohibited Use Policy](https://policies.google.com/terms/generative-ai/use-policy)
   before publishing, and don't redistribute model weights yourself unless
   the license allows it.
6. Internal testing track first, then closed → open → production rollout.

## Roadmap ideas

- In-app model download (from Hugging Face) with progress + checksum
- Model manager: multiple models, size/quality picker
- Migration to **LiteRT-LM** (MediaPipe GenAI is in maintenance mode;
  [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) is its successor)
- Settings: temperature, top-k, system prompt
- Chat export & multi-conversation history

## License

App code: MIT — see [LICENSE](LICENSE).
Gemma model weights have their own license (Gemma Terms of Use) and are **not**
included in this repository.
