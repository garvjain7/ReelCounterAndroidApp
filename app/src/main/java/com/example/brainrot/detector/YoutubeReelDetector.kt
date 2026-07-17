package com.example.brainrot.detector

import android.view.accessibility.AccessibilityEvent

class YoutubeReelDetector : ReelDetector {
    override fun detect(event: AccessibilityEvent): Boolean {
        // Simple heuristic for YouTube Shorts
        if (event.packageName == "com.google.android.youtube") {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                // Heuristic for scroll in YouTube
                return true
            }
        }
        return false
    }
}
