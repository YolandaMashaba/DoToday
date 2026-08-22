# Implementation Plan - Structured Daily Planner (XML)

Build a structured daily planner app with a task list and a notes section, adhering to a specific color palette and supporting both light and dark modes using XML Views.

## Proposed Changes

### Resources

#### [MODIFY] [colors.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/values/colors.xml)
- Add the requested colors: Blue (Main), Green (Headings), Yellow (Accents), and Ashy Grey (Background).
- Define both light and dark variations where necessary.

#### [MODIFY] [themes.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/values/themes.xml)
- Configure the Material3 theme to use the new colors.
- Ensure proper mapping for `colorSurface`, `colorPrimary`, etc.

#### [NEW] [themes.xml (night)](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/values-night/themes.xml)
- Create a dark mode theme file if it doesn't exist, using darker variants of the colors.

#### [MODIFY] [activity_main.xml](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/res/layout/activity_main.xml)
- Implement the "Structured Daily Planner" layout:
    - A header with "DoToday" in Green.
    - A ScrollView containing:
        - A "Today's Schedule" section (List of time slots/tasks).
        - A "Notes" section (EditText or Card with TextView).
    - A FloatingActionButton (Yellow) for adding new tasks.

### Code

#### [MODIFY] [MainActivity.kt](file:///Users/nkokamashaba/AndroidStudioProjects/DoToday/app/src/main/java/com/example/dotoday/MainActivity.kt)
- Wire up the UI components.
- Implement basic task/note handling logic.

## Verification Plan

### Manual Verification
- Deploy to an emulator/device.
- Verify the color scheme in both Light and Dark modes.
- Test adding tasks and writing notes.
- Ensure the layout is scrollable and fits well on different screen sizes.
