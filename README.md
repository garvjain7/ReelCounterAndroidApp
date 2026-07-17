# ReelRot — Reel Counter & Digital Wellbeing

ReelRot is a specialized Android digital wellbeing tool designed to make you aware of your short-form video consumption. By tracking "reels" (Instagram Reels and YouTube Shorts) in real-time using Android's Accessibility Service, it provides an immediate visual feedback loop to help you break the scroll-loop.

## Key Features
- **Real-time Detection**: Automatically detects when a new reel/short is scrolled into view.
- **Live Overlay**: Displays a non-intrusive "Floating Island" overlay showing your current session and daily counts directly over the apps.
- **Multi-Platform Support**: Specialized detection for Instagram and YouTube.
- **Historical Data**: Persists your counts in a local Room database for long-term tracking.
- **Daily Goals**: Monitor your "ReelRot" level with daily aggregate statistics.

## How to Build & Run
1. Clone the repository.
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle and ensure all dependencies are resolved.
4. Build and install the app on an Android device (API 24+).
5. **Important**: You must manually enable the **ReelRot Accessibility Service** in `Settings > Accessibility` for the app to function.
6. **Overlay Permission**: Allow the app to "Display over other apps" when prompted.

## Permissions Required
- `BIND_ACCESSIBILITY_SERVICE`: To detect scrolling and UI changes in target apps.
- `SYSTEM_ALERT_WINDOW`: To display the live counter overlay.
- `POST_NOTIFICATIONS`: For service status visibility.
