package com.garvjain.reelrot.detector

import android.view.accessibility.AccessibilityEvent

class YoutubeReelDetector : ReelDetector {
    override fun detect(event: AccessibilityEvent): Boolean {
        if (event.packageName != "com.google.android.youtube") return false
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        // Only count vertical swipes (Shorts feed)
        // Ignore horizontal scrolls (home feed carousels)
        val scrollDeltaY = event.scrollDeltaY
        val scrollDeltaX = event.scrollDeltaX
        return scrollDeltaY > 0 && scrollDeltaX == 0
    }
}
