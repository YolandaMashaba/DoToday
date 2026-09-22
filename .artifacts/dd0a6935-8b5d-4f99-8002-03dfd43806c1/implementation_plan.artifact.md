# Todoist API Integration

Integrate the Todoist REST API v2 into the DoToday app to allow users to sync their tasks. This includes fetching tasks for the Inbox and creating new tasks.

## User Review Required

> [!IMPORTANT]
> The API token `dc12d217e45f5eafae3fe10327fd92c7770726b9` will be stored in a `SecretConfig` object for now. In a production app, this should be handled via a secure login flow or encrypted storage.

> [!NOTE]
> I will add a `RecyclerView` to the `layout_inbox.xml` to display the fetched tasks, replacing the current static placeholder.

## Proposed Changes

### Dependencies

#### [MODIFY] [libs.versions.toml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/gradle/libs.versions.toml)
Add Retrofit, Gson converter, and OkHttp dependencies.

#### [MODIFY] [build.gradle.kts](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/build.gradle.kts)
Apply the new dependencies to the app module.

---

### Data & Network Layer

#### [NEW] [TodoistTask.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/data/TodoistTask.kt)
Define the data model for Todoist tasks.

#### [NEW] [TodoistApi.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/api/TodoistApi.kt)
Define the Retrofit interface for Todoist REST API v2.

#### [NEW] [TodoistRepository.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/data/TodoistRepository.kt)
Implement a repository to handle API calls and token management.

---

### UI Layer

#### [MODIFY] [layout_inbox.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/layout/layout_inbox.xml)
Add a `RecyclerView` to display the list of tasks.

#### [NEW] [item_inbox_task.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/layout/item_inbox_task.xml)
Create a layout for individual task items in the Inbox.

#### [MODIFY] [MainActivity.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/MainActivity.kt)
- Initialize the `TodoistRepository`.
- Implement fetching tasks and updating the UI when the Inbox tab is selected.
- Wire up the "New Inbox Task" button to the API.

---

### Manifest

#### [MODIFY] [AndroidManifest.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/AndroidManifest.xml)
Add `INTERNET` permission.

## Verification Plan

### Automated Tests
- I will add a basic unit test for the `TodoistRepository` using a mock API if time permits, or focus on manual verification.

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the "Inbox" tab.
- Verify that tasks are fetched from Todoist and displayed.
- Tap "+ New Inbox Task", enter a title, and verify it appears in the list (and on Todoist).
