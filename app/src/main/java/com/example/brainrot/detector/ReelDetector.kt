package com.example.brainrot.detector

import android.view.accessibility.AccessibilityEvent

interface ReelDetector {
    fun detect(event: AccessibilityEvent): Boolean
}
