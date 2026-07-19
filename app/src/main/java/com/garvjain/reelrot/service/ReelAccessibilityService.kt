package com.garvjain.reelrot.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.garvjain.reelrot.data.ReelDatabase
import com.garvjain.reelrot.data.ReelEvent
import com.garvjain.reelrot.detector.InstagramReelDetector
import com.garvjain.reelrot.detector.YoutubeReelDetector
import kotlinx.coroutines.*

class ReelAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: ReelDatabase
    
    private val detectors = listOf(
        InstagramReelDetector(),
        YoutubeReelDetector()
    )

    private val targetPackages = mapOf(
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube"
    )
    private var lastPackage: String? = null

    private val lastDetectedTime = mutableMapOf<String, Long>()
    private val DEBOUNCE_MS = 600L

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = ReelDatabase.getDatabase(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        if (packageName != lastPackage) {
            // User left a target app — flush and stop overlay regardless of where they went
            if (lastPackage in targetPackages.keys) {
                flushSessionToDatabase()
                stopOverlay()
            }
            // User entered a target app
            if (packageName in targetPackages.keys) {
                startOverlay(targetPackages[packageName]!!)
            }
            lastPackage = packageName
        }

        // Only count scrolls inside target apps
        if (packageName in targetPackages.keys) {
            for (detector in detectors) {
                if (detector.detect(event)) {
                    val platform = targetPackages[packageName]!!
                    val now = System.currentTimeMillis()
                    val last = lastDetectedTime[platform] ?: 0L
                    if (now - last >= DEBOUNCE_MS) {
                        lastDetectedTime[platform] = now
                        ReelSessionManager.increment(platform)
                    }
                    break
                }
            }
        }
    }

    private fun startOverlay(platform: String) {
        if (!Settings.canDrawOverlays(this)) return  // skip silently, don't crash
        val intent = Intent(this, ReelOverlayService::class.java).apply {
            putExtra("PLATFORM", platform)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlay() {
        val intent = Intent(this, ReelOverlayService::class.java)
        stopService(intent)
    }

    private fun flushSessionToDatabase() {
        val sessionData = ReelSessionManager.reset()
        
        serviceScope.launch {
            sessionData.forEach { (platform, count) ->
                if (count > 0) {
                    repeat(count) {
                        database.reelDao().insert(ReelEvent(platform = platform))
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        flushSessionToDatabase()
        stopOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        flushSessionToDatabase()
        stopOverlay()
        serviceScope.cancel()
    }
}
