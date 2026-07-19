package com.garvjain.reelrot.detector

import android.os.Build
import android.view.accessibility.AccessibilityEvent

class YoutubeReelDetector : ReelDetector {
    override fun detect(event: AccessibilityEvent): Boolean {
        if (event.packageName != "com.google.android.youtube") return false
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        
        // scrollDelta properties require API 28+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val scrollDeltaY = event.scrollDeltaY
            val scrollDeltaX = event.scrollDeltaX
            // YouTube Shorts scroll vertically with large delta, no horizontal movement
            scrollDeltaY > 0 && scrollDeltaX == 0
        } else {
            // Fallback for older APIs: count all vertical scrolls
            true
        }
    }
}
