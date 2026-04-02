# MemoDiary

MemoDiary is a simple note-taking application built for Android using Kotlin and Jetpack Compose. The app allows users to create, edit, and delete notes, similar to the iOS "Notes" app, with a timeline feature to view notes chronologically.

## Features

- **Note Creation**: Users can create new notes with titles and content.
- **Note Editing**: Users can edit existing notes.
- **Note Deletion**: Users can delete notes they no longer need.
- **Timeline View**: Notes are displayed in a timeline format, allowing users to see their notes in chronological order.
- **Local Storage**: Utilizes Room for local data storage, ensuring notes are saved even when the app is closed.

## Architecture

The application follows the MVVM (Model-View-ViewModel) architecture, which helps in separating concerns and making the codebase more maintainable.

- **Model**: Represents the data structure (e.g., `MemoEntity`, `Memo`).
- **View**: Composable functions that define the UI (e.g., `MemoDetailScreen`, `TimelineScreen`).
- **ViewModel**: Manages UI-related data and business logic (e.g., `MemoDetailViewModel`, `TimelineViewModel`).

## Technologies Used

- **Kotlin**: The primary programming language for Android development.
- **Jetpack Compose**: A modern toolkit for building native UI.
- **Room**: A persistence library for SQLite database management.
- **Hilt**: A dependency injection library for Android.

## Requirements

- Android 8.0 (API level 26) and above.
- Android Studio (latest stable version).

## Getting Started

1. Clone the repository:
   ```
   git clone <repository-url>
   ```

2. Open the project in Android Studio.

3. Build and run the application on an Android device or emulator.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.