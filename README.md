# DoToday - Daily Timeline & Task Manager for Android

**DoToday** is a modern, intuitive Android application designed to help users structure their daily routines through scheduled timeline tracking, interactive calendar planning, time interval monitoring, and seamless synchronization with the Todoist REST API.

---

## Features

- **Interactive Week Picker**: Horizontal week calendar header with day selection, dynamic task indicator dots, and smooth selection animations.
- **Daily Timeline View**: Chronological timeline displaying scheduled tasks, time interval rulers, duration countdowns, and completion toggles.
- **Month Calendar Overview**: Full-screen modal calendar previewing task lists for any selected date in the month.
- **Task Creation & Repeat Engine**: Add single or repeating tasks (Daily or Weekly) with automatic multi-day timeline scheduling and time duration calculation.
- **Todoist REST API Sync**: Asynchronous Inbox task fetching and creation integrated with the Todoist REST API v2 using Retrofit and Kotlin Coroutines, featuring automatic local task fallback for offline resilience.
- **Authentication System**: Login and Register screens featuring client-side form validation, password strength checks, and clean backstack navigation.
- **Material Design 3 & Edge-to-Edge**: Modern Material 3 UI theme support (Light & Dark modes) with safe System UI window bar insets handling.

---

## Tech Stack & Libraries

| Category | Technology / Library |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI Framework** | Native Android Views & Material Design 3 (`MaterialCardView`, `BottomNavigationView`, `FloatingActionButton`, `TextInputEditText`, `MaterialSwitch`) |
| **Architecture** | Repository Pattern, Coroutines (`lifecycleScope`, `Dispatchers.IO`), Sealed Classes (`TimelineEntry`), RecyclerView Adapters |
| **Networking** | [Retrofit 2](https://square.github.io/retrofit/), [OkHttpClient 4](https://square.github.io/okhttp/) (Logging Interceptor + 5s Timeouts), [Gson](https://github.com/google/gson) |
| **Build Tools** | Gradle with Kotlin DSL (`build.gradle.kts`) & Version Catalog (`libs.versions.toml`) |
| **SDK Versions** | `minSdk = 24` (Android 7.0), `compileSdk = 37`, `targetSdk = 37` |
| **YouTube Link** | [YouTube](https://youtube.com/shorts/9E9OtRP0Ah4?feature=share) |

---

## Project Structure

```
DoToday/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/dotoday/
│   │   │   ├── api/
│   │   │   │   └── TodoistApi.kt          # Retrofit REST API interface for Todoist v2
│   │   │   ├── data/
│   │   │   │   ├── TodoistRepository.kt   # Repository handling network calls & local fallbacks
│   │   │   │   └── TodoistTask.kt         # Data model with Gson @SerializedName annotations
│   │   │   ├── LoginActivity.kt           # User sign-in with input validation
│   │   │   ├── RegisterActivity.kt        # User registration with validation
│   │   │   └── MainActivity.kt            # Main activity (Timeline, Week Picker, Inbox, Settings)
│   │   └── res/
│   │       ├── layout/                    # Layout XML definitions (activities, dialogs, items)
│   │       ├── menu/                      # Bottom navigation menu
│   │       └── values/                    # Material 3 colors, themes (Light/Dark), strings
│   └── build.gradle.kts                   # Module build configuration
├── gradle/
│   └── libs.versions.toml                 # Version catalog for project dependencies
├── README.md                              # Project documentation
└── build.gradle.kts                       # Root build configuration
```

---

## Getting Started

### Prerequisites

- **Android Studio**: Ladybug / Jellyfish / Meerkat (2024.1+) or newer.
- **JDK**: Java Development Kit 11 or higher.
- **Android SDK**: API 35 or API 37 installed via Android SDK Manager.

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/DoToday.git
   cd DoToday
   ```
2. **Open in Android Studio**:
   Open Android Studio, select **File > Open**, and select the `DoToday` project directory.
3. **Sync Gradle**:
   Allow Gradle to download dependencies specified in `gradle/libs.versions.toml`.
4. **Run the app**:
   Select an Android Emulator or connected physical device running Android 7.0 (API 24) or higher, then click **Run 'app'** (`Shift + F10`).

---

## Architecture Highlights

- **Edge-to-Edge System Bars Safety**: Window insets (`WindowInsetsCompat.Type.systemBars()`) are safely applied to root layout views (`R.id.main`) to ensure compatibility across gesture navigation and 3-button navigation bars without framework dispatch loops.
- **Offline Resilience**: `TodoistRepository` catches network exceptions and non-200 HTTP responses, seamlessly serving local fallback inbox tasks so the user experience is never interrupted.
- **Coroutines & Thread Safety**: All network operations are explicitly dispatched on `Dispatchers.IO` via `withContext(Dispatchers.IO)` to prevent blocking the Android main thread (UI thread).

---

## License

This project is created for personal productivity and learning purposes. Feel free to modify and adapt it for your own use cases!
