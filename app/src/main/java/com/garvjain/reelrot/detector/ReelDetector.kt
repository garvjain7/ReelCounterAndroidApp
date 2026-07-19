package com.garvjain.reelrot.detector

import android.view.accessibility.AccessibilityEvent

interface ReelDetector {
    fun detect(event: AccessibilityEvent): Boolean
}
