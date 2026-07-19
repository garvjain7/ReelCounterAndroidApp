package com.garvjain.reelrot.detector

import android.view.accessibility.AccessibilityEvent

class InstagramReelDetector : ReelDetector {
    override fun detect(event: AccessibilityEvent): Boolean {
        if (event.packageName != "com.instagram.android") return false
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        // Only count significant vertical scrolls (reel swipe)
        // Ignore horizontal swipes (Stories) and non-scroll events
        val scrollDeltaY = event.scrollDeltaY
        val scrollDeltaX = event.scrollDeltaX
        return scrollDeltaY > 0 && scrollDeltaX == 0
    }
}
