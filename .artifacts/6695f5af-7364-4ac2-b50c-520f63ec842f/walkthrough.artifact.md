# Walkthrough - SSO Login, Offline Mode with Sync, & System UI / ANR Fixes

## Changes Summary

### Authentication
- Added **Sign in with Google / SSO** button to `activity_login.xml`.
- Implemented `attemptSsoLogin()` in `LoginActivity.kt` with OAuth2 / OpenID Connect simulation, structured logging, and task stack clearing.

### Offline Mode & Database
- Created `DoTodayDbHelper.kt` (SQLiteOpenHelper) managing local task persistence, offline caching, and `is_synced` mutation tracking.

### REST API & Offline-First Repository
- Refactored `TodoistRepository.kt` to provide an offline-first data layer: serves cached SQLite data instantly while performing background REST API synchronization with OkHttp 5-second timeout protection against ANRs.

### Code Documentation & Logging
- Added comprehensive inline documentation, architectural citations, and structured Logcat logging across all backend, database, and authentication components.

## Verification Results
- **Build**: `./gradlew app:assembleDebug` completed successfully.
- **Tests**: `./gradlew app:testDebugUnitTest` passed successfully.
