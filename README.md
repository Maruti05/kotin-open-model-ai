<div align="center">
  <img src="app/src/main/res/drawable/open_models.png" alt="OpenModels Logo" width="120" height="120"/>

  # OpenModels

  **Run Open-Source LLMs Privately on Your Android Device**

  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.3.21-purple?logo=kotlin" alt="Kotlin"/>
    <img src="https://img.shields.io/badge/Compose-2026.05.01-blue?logo=jetpackcompose" alt="Compose"/>
    <img src="https://img.shields.io/badge/AGP-9.2.1-green?logo=android" alt="AGP"/>
    <img src="https://img.shields.io/badge/Room-2.8.4-orange" alt="Room"/>
    <img src="https://img.shields.io/badge/Dagger-2.59.2-red?logo=dagger" alt="Dagger"/>
    <img src="https://img.shields.io/badge/minSdk-26-important" alt="minSdk"/>
    <img src="https://img.shields.io/badge/license-MIT-blue" alt="License"/>
  </p>

  <p>
    <a href="#features">Features</a> •
    <a href="#architecture">Architecture</a> •
    <a href="#project-structure">Project Structure</a> •
    <a href="#screens">Screens</a> •
    <a href="#model-loading-and-inference">Model Loading</a> •
    <a href="#download-system">Download System</a> •
    <a href="#building">Building</a> •
    <a href="#tech-stack">Tech Stack</a>
  </p>
</div>

---

## Features

- **🔒 100% Private & Offline**: All inference runs on-device. No data ever leaves your phone.
- **🧠 20+ Models**: From 135MB ultra-tiny models to 7B parameter beasts — GGUF format with multiple quantization levels.
- **📊 Telemetry Dashboard**: Real-time health score, RAM usage, CPU cores, battery monitoring, device tier classification.
- **📥 Model Repository**: Browse, search, filter by tier (T1/T2/T3) or download status. Device-aware buttons disabled for incompatible models.
- **⚙️ Inference Controls**: Temperature, Top-P, Top-K, Max Tokens, presets (Precise/Balanced/Creative).
- **💬 Chat Interface**: Rich markdown rendering (bold, italic, strikethrough, inline code, clickable links), syntax-highlighted code blocks with copy button, long-press context menu, system prompt toggle, session management with SQLite persistence.
- **🎨 Theme System**: Dark, Light, and System themes with Material3 adaptive color.
- **🏃 Benchmark Runner**: On-device performance benchmarking with history stored in Room.
- **⬇️ Download Manager**: Real-time progress with speed (Mbps), auto-completion state machine, error recovery.
- **📝 Prompt Presets**: Architect, Writer, Creative, Security, Tutor — predefined system prompt templates.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                  │
│  ┌──────────┐  ┌────────────┐  ┌────────┐  ┌────────┐  │
│  │ Telemetry│  │ Repository  │  │  Chat   │  │Settings│  │
│  │ Dashboard│  │  (Models)   │  │        │  │        │  │
│  └────┬─────┘  └─────┬──────┘  └───┬────┘  └───┬────┘  │
│       │              │             │           │        │
│  ┌────┴──────────────┴─────────────┴───────────┴────┐   │
│  │           ViewModels + StateFlows                 │   │
│  └────────────────────────┬─────────────────────────┘   │
├───────────────────────────┼─────────────────────────────┤
│              Domain Layer │                             │
│  ┌────────────────────────┼─────────────────────────┐   │
│  │  InferenceEngine       │  ModelDownloader        │   │
│  │  (GGUF + Simulated)    │  (OkHttp + File I/O)    │   │
│  ├────────────────────────┼─────────────────────────┤   │
│  │  HybridModelManager    │  HardwareChecker        │   │
│  │  BenchmarkRunner       │  PromptTemplateService  │   │
│  │  LlmOutputParser       │                         │   │
│  └────────────────────────┴─────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                    Data Layer                            │
│  ┌──────────────────────┬──────────────────────────┐   │
│  │   Room Database      │   DataStore Preferences   │   │
│  │  ┌────────────────┐  │  ┌─────────────────────┐  │   │
│  │  │ ChatSession    │  │  │ Theme mode          │  │   │
│  │  │ ChatMessage    │  │  │ Inference params    │  │   │
│  │  │ Benchmark      │  │  │ Downloaded model IDs │  │   │
│  │  │ FileContext    │  │  │ System prompt       │  │   │
│  │  └────────────────┘  │  └─────────────────────┘  │   │
│  └──────────────────────┴──────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                    DI Layer (Dagger Hilt)                │
│              AppModule.kt — @Singleton providers        │
└─────────────────────────────────────────────────────────┘
```

### Clean Architecture Layers

| Layer | Package | Responsibility |
|-------|---------|---------------|
| **UI** | `ui/` | Compose screens, ViewModels, theme, navigation, reusable components |
| **Domain** | `domain/` | Business logic — inference engines, downloader, hardware checks, parsers |
| **Data** | `data/` | Room DB entities/DAOs, DataStore preferences, repositories |
| **DI** | `di/` | Dagger Hilt module providing all singletons |

---

## Project Structure

```
app/src/main/java/com/vedica/labs/ind/app/chat/openmodels/
├── MainActivity.kt              # Entry point — SplashScreen + Compose host
├── OpenModelsApp.kt             # @HiltAndroidApp Application class
│
├── di/
│   └── AppModule.kt             # Dagger Hilt DI — all @Provides @Singleton
│
├── data/
│   ├── model/                   # Domain data classes
│   │   ├── ModelCatalog.kt      # 20+ GGUF model definitions with metadata
│   │   ├── ModelInfo.kt         # Model metadata data class
│   │   ├── ModelFormat.kt       # GGUF, TFLITE, ONNX enum
│   │   ├── ModelDownloadState.kt# Progress state machine (DOWNLOADING/COMPLETED/ERROR)
│   │   ├── InferenceParams.kt   # Temperature, Top-P, Top-K, MaxTokens, etc.
│   │   ├── DiagnosticsInfo.kt   # RAM, cores, Vulkan/NNAPI support
│   │   ├── BenchmarkResult.kt   # Benchmark timing data
│   │   ├── ChatSession.kt       # Chat session metadata
│   │   ├── ChatMessage.kt       # Individual message with role + content
│   │   ├── FileContext.kt       # File attachment model
│   │   └── PromptPreset.kt      # System prompt preset model
│   │
│   ├── local/                   # Persistence layer
│   │   ├── AppDatabase.kt       # Room DB (version 3, destructive migration)
│   │   ├── dao/                 # Data Access Objects
│   │   │   ├── ChatSessionDao.kt   # CRUD for sessions
│   │   │   ├── ChatMessageDao.kt   # Paginated message queries
│   │   │   ├── BenchmarkDao.kt     # INSERT + SELECT recent/latest
│   │   │   └── FileContextDao.kt   # File attachment operations
│   │   ├── entity/              # Room @Entity classes
│   │   │   ├── ChatSessionEntity.kt
│   │   │   ├── ChatMessageEntity.kt
│   │   │   ├── BenchmarkEntity.kt
│   │   │   └── FileContextEntity.kt
│   │   └── preferences/
│   │       └── AppPreferences.kt# DataStore — theme, params, model IDs
│   │
│   └── repository/              # Data layer — bridges data sources to domain
│       ├── ModelRepository.kt   # Download state machine, model paths, file contexts
│       ├── ChatRepository.kt    # Session + message CRUD with pagination
│       ├── BenchmarkRepository.kt # Benchmark save/load with entity mapping
│       └── SettingsRepository.kt   # Inference params + theme via DataStore flows
│
├── domain/
│   ├── download/
│   │   └── ModelDownloader.kt   # OkHttp streaming downloader with progress callbacks
│   ├── inference/
│   │   ├── InferenceEngine.kt   # Interface: loadModel, generateChat, stopGeneration
│   │   ├── SimulatedInferenceEngine.kt  # Offline fallback — deterministic responses
│   │   ├── GGUFInferenceEngine.kt       # Llamatik (LlamaBridge API) — real LLM inference
│   │   └── HybridModelManager.kt        # Auto-selects engine by model format
│   ├── benchmark/
│   │   └── BenchmarkRunner.kt   # CPU/RAM stress test for performance scoring
│   ├── parser/
│   │   └── LlmOutputParser.kt   # Parses streaming tokens into markdown/code blocks
│   └── util/
│       ├── HardwareChecker.kt   # RAM, cores, Vulkan, NNAPI diagnostics
│       └── PromptTemplateService.kt  # 5 system prompt presets
│
└── ui/
    ├── navigation/
    │   └── ShellLayout.kt       # Bottom nav with animated tab transitions
    ├── dashboard/
    │   ├── DashboardScreen.kt   # Health score, stat grid, benchmark, history
    │   └── DashboardViewModel.kt# Collects diagnostics, runs benchmarks
    ├── modelmanager/
    │   ├── ModelManagerScreen.kt# Search, tier filter, download filter, model cards
    │   └── ModelManagerViewModel.kt# Device capabilities, filtering, download triggers
    ├── chat/
    │   ├── ChatScreen.kt        # Message list, input bar, session management
    │   ├── ChatViewModel.kt     # Message streaming, session state, token tracking
    │   ├── MarkdownText.kt      # Pure Compose markdown renderer (bold, italic, strikethrough, inline code, links)
    │   └── CodeBlock.kt         # Code block viewer with syntax highlighting (14+ languages) + copy button
    ├── settings/
    │   ├── SettingsScreen.kt    # Inference params, theme, toggles, presets, support/legal/about
    │   └── SettingsViewModel.kt # Persists all inference params to DataStore
    ├── legal/
    │   ├── LegalScreen.kt       # Combined privacy + terms screen
    │   ├── PrivacyPolicyScreen.kt
    │   └── TermsConditionsScreen.kt
    ├── components/              # Reusable UI components
    │   ├── StyledCard.kt        # Themed card with 16dp radius, gradient support
    │   ├── StatusBadge.kt       # Active/inactive status indicator
    │   ├── EmptyState.kt        # Icon + title + subtitle + action button
    │   ├── CollapsibleSection.kt# Expandable/collapsible section
    │   └── InfoGuard.kt         # Conditional rendering wrapper
    └── theme/
        ├── Color.kt             # Dark/light palettes, functional colors (NeonCyan, etc.)
        ├── Theme.kt             # Material3 dark/light color schemes
        ├── Type.kt              # Typography definitions
        └── ThemeViewModel.kt    # Theme mode state holder
```

---

## Screens

### 1. Telemetry Dashboard (`DashboardScreen.kt`)

| Component | Description |
|-----------|-------------|
| **Health Score** | Animated donut chart (0–100) with color-coded label (Excellent/Fair/Limited) |
| **Stat Cards** | RAM Usage, CPU Cores, Performance Tier (T1–T3), Free RAM — 2×2 grid with gradient backgrounds |
| **Acceleration** | Vulkan / NNAPI support badges |
| **Tier Progress** | Animated linear bar showing RAM utilization percentage |
| **Benchmark** | "RUN BENCHMARK" button on its own line → results (Tokens/sec, Prompt Eval, Generation, RAM Used) |
| **Benchmark History** | Last 4 benchmark results from Room DB |

**ViewModel:** `DashboardViewModel.kt` — collects `DiagnosticsInfo` from `HardwareChecker`, runs `BenchmarkRunner`, exposes `StateFlow<DiagnosticsInfo>` and `StateFlow<BenchmarkResult?>`.

### 2. Model Repository (`ModelManagerScreen.kt`)

| Component | Description |
|-----------|-------------|
| **Device Banner** | Animated bar showing available RAM, storage, battery level |
| **Search** | Text field with clear button — filters by model name/ID |
| **Tier Filter** | `FlowRow` of chips: All, T1 (high-end), T2 (mid-range), T3 (ultra-tiny) — wraps on narrow screens |
| **Download Filter** | Dropdown: All, Downloaded, Not Downloaded |
| **Model Cards** | Name, size (GB), params, tier, context window — with `FlowRow` info chips that wrap |
| **State Machine** | GET MODEL → Downloading (progress bar with %) → COMPLETED (LOAD TO RAM + DELETE) → ERROR (Retry/Dismiss) |
| **Device Checks** | "GET MODEL" disabled and shows NEEDS MORE RAM / NO SPACE / LOW BATTERY when incompatible |

**ViewModel:** `ModelManagerViewModel.kt` — `DeviceCapabilities` from BatteryManager + StatFs + HardwareChecker, `canRunModel()` / `canDownloadModel()` logic, `getIncompatibilityReason()`.

### 3. Chat (`ChatScreen.kt`)

| Component | Description |
|-----------|-------------|
| **Session List** | Side panel or list of chat sessions with model name, message count, last preview |
| **Message History** | Paginated lazy list — content-width bubbles with asymmetric rounded corners (18dp, tail 4dp), user indigo right-aligned, AI surface left-aligned |
| **Markdown Rendering** | Pure Compose `MarkdownText` — **bold**, *italic*, ~~strikethrough~~, `inline code`, clickable [links](url) — no external markdown library |
| **Code Blocks** | `CodeBlockView` with dark terminal background, language label, syntax highlighting for 14+ languages (keywords pink, strings blue, comments gray, numbers light blue, types teal, functions yellow, annotations purple), copy button with "Copied!" feedback |
| **Long-Press Menu** | Per-bubble dropdown: Copy, Copy Code Blocks, Select All |
| **Streaming Cursor** | Blinking `▊` cursor during generation (suppressed for mixed text/code content) |
| **Input Bar** | Pill-shaped multi-line field with top-rounded bar (20dp), keyboard-aware via `imePadding()` — stays above soft keyboard |
| **Send / Stop** | Circular filled buttons — neon send when idle, red stop during generation |
| **System Prompt Toggle** | Enable/disable system prompt per conversation |
| **Token Tracking** | Real-time tokens-per-second display during generation |
| **Throttled Streaming** | UI updates throttled to ~20/s to prevent main-thread overload (ANR prevention) |

**ViewModel:** `ChatViewModel.kt` — manages sessions via `ChatRepository`, streams tokens from `HybridModelManager`, tracks generation state.

### 4. Settings (`SettingsScreen.kt`)

| Section | Components |
|---------|------------|
| **Appearance** | Theme chips (Dark/Light/System) with `Modifier.weight(1f)` |
| **Inference Profile** | Quick Presets (Precise/Balanced/Creative) + sliders (Temperature, Top-P, Top-K, Max Tokens) in one grouped card |
| **System Prompt** | OutlinedTextField with character count |
| **Display Options** | Toggle rows with subtitle (Show Thinking, Show Reasoning) |
| **Prompt Presets** | Clickable cards: Architect, Writer, Creative, Security, Tutor — active checkmark |
| **Support** | Rate the App (Play Store), Share the App (share sheet) |
| **Legal** | Privacy Policy, Terms & Conditions (open in browser) |
| **About** | App name, version, description |

**ViewModel:** `SettingsViewModel.kt` — reads/writes `InferenceParams` and theme mode to `DataStore` via `SettingsRepository`.

---

## Model Loading and Inference

### HybridModelManager

`HybridModelManager` is the central orchestrator that loads models into RAM and delegates inference to the appropriate engine.

```
HybridModelManager.loadModelToRam(modelId, hyperparams?)
    │
    ├── Resolves model path from ModelRepository
    ├── Verifies file exists (throws if missing/corrupt)
    ├── Detects format (GGUF → GGUFEngine, else → SimulatedEngine)
    ├── Calls engine.loadModel()
    └── Returns success/failure
```

### Inference Engines

#### SimulatedInferenceEngine (Always Available)

- **Purpose**: Fallback engine when native libraries are unavailable or model format isn't GGUF.
- **Behavior**: Generates deterministic, context-aware responses based on keyword matching in the user's query.
- **Templates**: Supports `chatml`, `phi`, `gemma`, `llama2` prompt templates.
- **Speed Simulation**: Adds configurable delay per token (based on temperature) to simulate real inference.
- **No native code required** — pure Kotlin.

#### GGUFInferenceEngine (Llamatik)

- **Purpose**: Real neural network inference using GGUF format models via [Llamatik](https://github.com/ferranpons/llamatik) — a Kotlin Multiplatform library wrapping llama.cpp.
- **Library**: `com.llamatik:library:1.6.0` (KMP, no JNI boilerplate, no C++ build step).
- **Model Validation**: Validates GGUF magic bytes (`GGUF`), checks file size ≥ 8KB, deletes corrupt files.
- **Prompt Building**: Uses `LlamaBridge.applyChatTemplate()` for automatic chat template formatting, falls back to ChatML.
- **KV Cache Management**: Calls `LlamaBridge.sessionReset()` between generations to prevent native memory overflow across turns.
- **Context Overflow Protection**: `truncateMessages()` estimates token count (chars/3.5), reserves 55% for response, drops oldest conversation turns while keeping system + latest messages.
- **Async Cancellation**: Generation runs on `Dispatchers.IO` via `launch` inside a `callbackFlow`; `LlamaBridge.nativeCancelGenerate()` stops the native generation loop.
- **Thread Safety**: `@Volatile var _generating` guard prevents overlapping `generateStream` calls.
- **Crash Resilience**: Comprehensive Timber logging at every step (model load, generation, memory, errors) + `Thread.setDefaultUncaughtExceptionHandler` in `OpenModelsApp`.
- **No native build required** — the library ships prebuilt `.so` files for all Android ABIs.

#### InferenceEngine Interface

```kotlin
interface InferenceEngine {
    val isLoaded: Boolean
    val loaderName: String
    val currentModelId: String?
    val currentTemplate: String?

    suspend fun loadModel(modelId, modelPath, hyperparams?): Boolean
    fun generateChat(messages, template?, params): Flow<String>
    suspend fun stopGeneration()
    suspend fun unloadModel()
    fun dispose()
}
```

### Generation Flow

```
User sends message
    │
    ├── ChatViewModel
    │   ├── Builds ChatMessage list (system + history + user, context-truncated)
    │   ├── Calls HybridModelManager.generateChat()
    │   │   └── Delegates to GGUFInferenceEngine.generateChat()
    │   │       ├── LlamaBridge.updateGenerateParams() — set temperature, top-p, etc.
    │   │       ├── LlamaBridge.sessionReset() — clear KV cache between turns
    │   │       ├── Builds prompt via LlamaBridge.applyChatTemplate()
    │   │       ├── launch(Dispatchers.IO) { LlamaBridge.generateStream() }
    │   │       │   ├── onDelta(token) → trySend to callbackFlow
    │   │       │   ├── onComplete() → emits [DONE]
    │   │       │   └── onError(msg) → closes flow with exception
    │   │       └── awaitClose() → nativeCancelGenerate() + cancel job
    │   └── Collects tokens (throttled every 50ms) → appends to streamingContent
    │
    └── Message saved to Room via ChatRepository (with real UUID)
```

---

## Download System

### State Machine

```
                     ┌──────────────┐
                     │    IDLE      │
                     └──────┬───────┘
                            │ click GET MODEL
                            ▼
                     ┌──────────────┐
                     │  DOWNLOADING │ ◄── onProgress() updates
                     │  (progress)  │     percentage + speed
                     └──┬───────┬───┘
                        │       │
               success  │       │  error
                        ▼       ▼
                ┌──────────┐ ┌──────────┐
                │COMPLETED │ │  ERROR   │
                │100%      │ │  retry   │
                │addToDl'd │ │ dismiss  │
                └──────────┘ └──────────┘
```

### Key Classes

| Class | File | Role |
|-------|------|------|
| `ModelDownloadState` | `data/model/ModelDownloadState.kt` | Data class with `status` (DOWNLOADING/COMPLETED/ERROR), `downloadedBytes`, `totalBytes`, `progressPercentage`, `downloadSpeedMbps` |
| `ModelDownloader` | `domain/download/ModelDownloader.kt` | OkHttp streaming HTTP download — 8KB buffer, writes to `FileOutputStream`, reports progress via callback |
| `ModelRepository.startDownload()` | `data/repository/ModelRepository.kt` | Creates initial state → calls `ModelDownloader.download()` → sets COMPLETED + calls `addToDownloaded()` on success → sets ERROR on failure |

### UI State Transitions

| State | What User Sees |
|-------|----------------|
| IDLE | **GET MODEL** button |
| DOWNLOADING | Progress bar with `downloadedBytes/totalBytes` + `speed Mbps` |
| COMPLETED | **LOAD TO RAM** + **DELETE** buttons |
| ERROR | Red error banner with **Dismiss** + **Retry** buttons |
| LOADING TO RAM | Spinner + "Loading..." text |
| LOADED (ACTIVE) | **UNLOAD** (amber) + **DELETE** (red) buttons, green ACTIVE badge |

### Device Compatibility Checks

Before enabling the **GET MODEL** or **LOAD TO RAM** buttons, the ViewModel checks:

```kotlin
// Can the model be downloaded?
fun canDownloadModel(modelId): Boolean =
    hasStorage && hasRam && hasBattery

// Can the model be loaded into RAM?
fun canRunModel(modelId): Boolean =
    availableRamGb >= minRamGb * 0.5
```

Incompatibility reasons are surfaced to the user as amber warning banners and contextual button labels (`NEEDS MORE RAM`, `NO SPACE`, `LOW BATTERY`).

---

## Data Persistence

### Room Database (`AppDatabase.kt`)

**File:** `app/src/main/java/.../data/local/AppDatabase.kt`
**Database Name:** `openmodels_chat.db`
**Version:** 3 (uses `fallbackToDestructiveMigration(true)`)

| Entity | DAO | Purpose |
|--------|-----|---------|
| `ChatSessionEntity` | `ChatSessionDao` | Chat sessions — id, modelName, createdAt, systemPromptOverride |
| `ChatMessageEntity` | `ChatMessageDao` | Messages — sessionId, role (user/assistant), content, timestamp, tokensPerSecond. Supports paginated queries. |
| `BenchmarkEntity` | `BenchmarkDao` | Benchmark results — modelName, tokensPerSecond, latencies, ramUsedMb |
| `FileContextEntity` | `FileContextDao` | File attachments — filename, content (text), addedAt |

### DataStore Preferences (`AppPreferences.kt`)

| Key | Type | Default |
|-----|------|---------|
| `theme_mode` | String | `"system"` |
| `temperature` | Double | `0.7` |
| `top_p` | Double | `0.9` |
| `top_k` | Int | `40` |
| `max_tokens` | Int | `512` |
| `system_prompt` | String | `""` |
| `show_thinking` | Boolean | `true` |
| `show_reasoning` | Boolean | `true` |
| `downloaded_model_ids` | Set\<String\> | `emptySet()` |

---

## Theme System

### Color Palette (`Color.kt`)

```
Dark:   Obsidian (#0B0F19), Card (#1E293B), NeonCyan (#00F2FE), Indigo (#4FACFE)
Light:  Porcelain (#F8FAFC), White (#FFFFFF), Navy (#0F172A), Slate (#64748B)
Func:   SuccessGreen (#10B981), WarningAmber (#F59E0B), ErrorRed (#EF4444), InfoBlue (#3B82F6)
Tabs:   Cyan, Indigo, Green, Purple — each screen's accent color
```

### Theme Switching

`ThemeViewModel` stores mode in DataStore → `OpenModelsTheme` composable applies:
- **Dark**: `darkColorScheme` with `DarkObsidian` background, `NeonCyan` primary
- **Light**: `lightColorScheme` with `LightPorcelain` background, adjusted primary
- **System**: Delegates to `isSystemInDarkTheme()`

### Splash Screen

- **Library**: `core-splashscreen:1.2.0`
- **Theme**: `Theme.OpenModels.Splash` — uses `splash_screen.xml` (dark background + `open_models.png` centered at 96dp)
- **Integration**: `installSplashScreen()` in `MainActivity.onCreate()` before `super.onCreate()`
- **Transition**: Seamless fade from splash → Compose content

---

## Build Configuration

### AGP 9.x Migration

Upgraded to AGP 9.2.1 with built-in Kotlin support (enabled by default in AGP 9.x):

| Change | Before | After |
|--------|--------|-------|
| **Kotlin plugin** | `org.jetbrains.kotlin.android` | Removed — built-in Kotlin handles compilation |
| **KSP** | `2.2.10-2.0.2` (tied to Kotlin) | `2.3.8` (independent versioning — KSP2) |
| **Kotlin version** | `2.2.10` | `2.3.21` |
| **DSL type** | `android.newDsl=false` (BaseAppModuleExtension) | Default `true` (ApplicationExtension) |
| **JVM target** | `kotlinOptions { jvmTarget = "17" }` | Derives from `compileOptions.targetCompatibility = VERSION_17` |
| **Built-in Kotlin** | `android.builtInKotlin=false` | Default `true` — AGP manages Kotlin compilation |

---

## UI Components

| Component | File | Usage |
|-----------|------|-------|
| `StyledCard` | `components/StyledCard.kt` | 16dp rounded corner card with border, gradient support, clickable variant |
| `StatusBadge` | `components/StatusBadge.kt` | Pill-shaped badge (green active / red inactive) |
| `EmptyState` | `components/EmptyState.kt` | Icon + title + subtitle + optional action button |
| `CollapsibleSection` | `components/CollapsibleSection.kt` | Expandable/collapsible content sections |
| `InfoGuard` | `components/InfoGuard.kt` | Conditional rendering wrapper for loading/error/empty states |

---

## Building

### Prerequisites

- **Android Studio** Ladybug or newer (2024.3+)
- **JDK 21** (managed by Gradle toolchain — auto-downloaded via `foojay-resolver-convention`)
- **Android SDK** 36
- **Gradle** 9.4.1 (wrapped)

### Build

```bash
./gradlew assembleDebug
```

Builds in ~1–2 minutes (no C++ compilation — Llamatik ships prebuilt binaries).

### Generate Signed APK/App Bundle

```bash
./gradlew assembleRelease
# or
./gradlew bundleRelease
```

### Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Llamatik Integration

This project uses [Llamatik](https://github.com/ferranpons/llamatik) for on-device neural network inference with GGUF format models. Llamatik is a Kotlin Multiplatform library that wraps llama.cpp and exposes a clean Kotlin API — no JNI boilerplate, no CMake build step.

### Library

- **Maven**: `com.llamatik:library:1.6.0`
- **Source**: [github.com/ferranpons/llamatik](https://github.com/ferranpons/llamatik)
- **License**: Apache 2.0
- **Integration**: Single dependency line in `app/build.gradle.kts` (no C++ compilation, no NDK setup)

### Architecture

```
Kotlin (GGUFInferenceEngine)
  │  com.llamatik.library.platform.LlamaBridge
  │  ├── initGenerateModel(path)          — load GGUF model from file
  │  ├── updateGenerateParams(...)         — temperature, top-p, top-k, maxTokens, contextLength, etc.
  │  ├── generateStream(prompt, callback)  — blocking native call on IO dispatcher
  │  ├── sessionReset()                    — clear KV cache between chat turns
  │  ├── nativeCancelGenerate()            — stop the generation loop
  │  ├── applyChatTemplate(msgs, addPrefix)— auto-format with model's chat template
  │  └── shutdown()                        — release native resources
  ▼
Llamatik (KMP Library)
  │  llama.cpp native bindings (JNI)
  │  prebuilt .so files for arm64-v8a, armeabi-v7a, x86_64
  ▼
llama.cpp C API
  │  ggml backend (CPU)
  │  model weights, tokenizer, sampler chain
  ▼
Model file (GGUF on disk, memory-mapped via mmap)
```

### Capabilities

| Feature | Support |
|---------|---------|
| GGUF model loading | ✅ |
| Streaming generation | ✅ (via `GenStream` callback) |
| Chat template formatting | ✅ (`applyChatTemplate` — auto-detects model template) |
| KV cache session management | ✅ (`sessionReset()` between turns) |
| Context length control | ✅ |
| GPU layers (Metal, CUDA) | ✅ (platform-dependent) |
| Flash attention | ✅ |
| Batch processing | ✅ |
| JSON schema / grammar | ✅ |
| Speculative decoding (MTP) | ✅ |
| Embeddings | ✅ |
| Multimodal (vision) | ✅ |
| Prebuilt binaries | ✅ (arm64-v8a, armeabi-v7a, x86_64, x86_64 desktop) |

### Threading Model

```
generateChat()
  │
  └── callbackFlow { ... }.flowOn(Dispatchers.Default)
       │
       ├── updateGenerateParams()     ← runs on Default
       ├── sessionReset()             ← runs on Default
       ├── launch(Dispatchers.IO) {
       │     generateStream(prompt)   ← blocks IO thread
       │     onDelta(token) → trySend ← non-blocking channel send
       │   }
       └── awaitClose { cancelGenerate() }  ← cleanup
```

- `sessionReset()` called before every generation to prevent KV cache overflow.
- Streaming updates throttled to ~20/s in `ChatViewModel` to protect the main thread.
- `nativeCancelGenerate()` serialized via `@Volatile` flag and dedicated dispose path.

---

## Tech Stack

| Category | Library | Version |
|----------|---------|---------|
| **Language** | Kotlin | 2.3.21 |
| **UI** | Jetpack Compose (Material3) | BOM 2026.05.01 |
| **Icons** | Material Icons Extended | via BOM |
| **DI** | Dagger Hilt | 2.59.2 |
| **KSP** | Kotlin Symbol Processing | 2.3.8 |
| **Database** | Room | 2.8.4 |
| **Preferences** | DataStore Preferences | 1.2.0 |
| **Navigation** | Navigation Compose | 2.9.6 |
| **Lifecycle** | Lifecycle Runtime + ViewModel Compose | 2.10.0 |
| **Activity** | Activity Compose | 1.12.2 |
| **Coroutines** | Kotlinx Coroutines | 1.11.0 |
| **Networking** | OkHttp | 4.12.0 |
| **Serialization** | Kotlinx Serialization | 1.7.3 |
| **Inference** | Llamatik (llama.cpp KMP wrapper) | 1.6.0 |
| **Logging** | Timber | 5.0.1 |
| **Image Loading** | Coil Compose | 2.7.0 |
| **HTML Parsing** | Jsoup | 1.18.3 |
| **Build System** | Android Gradle Plugin | 9.2.1 (built-in Kotlin) |
| **Gradle** | Gradle Wrapper | 9.4.1 |
| **Splash Screen** | Core Splashscreen | 1.2.0 |

---

## License

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

<div align="center">
  <p><strong>Built with ❤️ using Kotlin & Jetpack Compose</strong></p>
  <p>
    <a href="#openmodels">Back to top ▲</a>
  </p>
</div>
