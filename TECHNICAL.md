# Technical Documentation — ReelRot Architecture

## 1. Architecture Overview
The project follows a modular Android architecture utilizing Jetpack Compose for UI, Room for persistence, and Kotlin Coroutines for asynchronous operations. The core logic resides in a background `AccessibilityService`.

## 2. Accessibility Service & Reel Detection
`ReelAccessibilityService` is the heartbeat of the app. It listens for `TYPE_VIEW_SCROLLED` and `TYPE_WINDOW_CONTENT_CHANGED` events.

### Platform Detection Logic:
- **Instagram**: Utilizes `InstagramReelDetector` which looks for specific view hierarchies and content descriptions (e.g., "Reel by...") within the `com.instagram.android` package.
- **YouTube**: Utilizes `YoutubeReelDetector` which identifies the "Shorts" player container and scroll transitions within `com.google.android.youtube`.

The service manages session state via `ReelSessionManager` and triggers the `ReelOverlayService` when a target app is in the foreground.

## 3. Room Database Schema
The app uses a single-table schema for maximum simplicity and query speed:
- **Table**: `reel_events`
- **Fields**:
    - `id`: Primary Key (Auto-generate)
    - `platform`: String ("Instagram" or "YouTube")
    - `timestamp`: Long (System currentTimeMillis)

The `ReelDao` provides reactive `Flow` outputs for "Today's Count" and "Total Count" which the UI and Overlay consume.

## 4. Overlay Service
`ReelOverlayService` uses the `WindowManager` to draw a custom `ComposeView` over other applications. 
- **Island UI**: A "Dynamic Island" inspired UI composed of three segments: Instagram count, Total "ReelRot" (💀), and YouTube count.
- **Lifecycle Management**: Implements `LifecycleOwner` and `SavedStateRegistryOwner` to allow Jetpack Compose to run reliably inside a background `Service`.

## 5. Session Management
`ReelSessionManager` acts as an in-memory buffer. 
- When a user enters a target app, a session starts.
- Counts increment in memory first for zero-latency overlay updates.
- When the user leaves the app or the service is interrupted, the session "flushes" the buffered counts into the Room database in a single transaction.

## 6. Notable Implementation Details
- **Performance**: Accessibility events are filtered by package name before being passed to detectors to minimize CPU overhead.
- **UI/UX**: Uses a custom theme (`ReelRotTheme`) with high-contrast colors (Flame for accents, DeepCharcoal for background) for instant recognition during usage.
