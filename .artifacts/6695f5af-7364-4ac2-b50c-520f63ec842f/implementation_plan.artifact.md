# Implementation Plan - SSO Login, Offline Mode with Sync, & System UI / ANR Fixes

Implement and test the SSO login feature, offline-first SQLite database layer with background REST API synchronization, extensive code documentation & logging, and resolution of System UI / ANR errors.

## Proposed Changes

### Authentication & SSO
#### [MODIFY] [activity_login.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/layout/activity_login.xml)
- Added "Sign in with Google / SSO" material button.

#### [MODIFY] [LoginActivity.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/LoginActivity.kt)
- Implemented `attemptSsoLogin()` handling OAuth2 / OpenID Connect SSO authentication flow with logging and task stack clearing.

### Offline Mode & Database
#### [NEW] [DoTodayDbHelper.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/data/DoTodayDbHelper.kt)
- Robust SQLite database helper for local task caching, offline persistence, and tracking sync state (`is_synced`).

### REST API Integration & Sync
#### [MODIFY] [TodoistRepository.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/data/TodoistRepository.kt)
- Refactored to implement offline-first architecture (serves local SQLite cache instantly, syncs with Todoist REST API in background, handles 5-second timeouts to prevent ANRs).

#### [MODIFY] [MainActivity.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/MainActivity.kt)
- Passed context to `TodoistRepository` initialization and ensured all network/database tasks run off the main thread.

## Verification Plan

### Automated Tests
- Ran `./gradlew app:assembleDebug` and `./gradlew app:testDebugUnitTest`.

### Manual Verification
- Verified app builds successfully without errors.
- Tested login screen UI with credentials and SSO login button.
- Verified background thread execution for offline caching and API sync.
