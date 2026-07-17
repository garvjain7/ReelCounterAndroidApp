package com.garvjain.reelrot.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.garvjain.reelrot.data.ReelDatabase
import com.garvjain.reelrot.data.ReelEvent
import com.garvjain.reelrot.detector.InstagramReelDetector
import com.garvjain.reelrot.detector.YoutubeReelDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = ReelDatabase.getDatabase(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        if (packageName != lastPackage) {
            // Flush and stop if leaving a target app
            if (lastPackage in targetPackages.keys) {
                flushSessionToDatabase(lastPackage!!)
                stopOverlay()
            }
            // Start if entering a target app
            if (packageName in targetPackages.keys) {
                startOverlay(targetPackages[packageName]!!)
            }
            lastPackage = packageName
        }

        if (packageName in targetPackages.keys) {
            for (detector in detectors) {
                if (detector.detect(event)) {
                    val platform = targetPackages[packageName]!!
                    ReelSessionManager.increment(platform)
                    break
                }
            }
        }
    }

    private fun startOverlay(platform: String) {
        val intent = Intent(this, ReelOverlayService::class.java).apply {
            putExtra("PLATFORM", platform)
        }
        startService(intent)
    }

    private fun stopOverlay() {
        val intent = Intent(this, ReelOverlayService::class.java)
        stopService(intent)
    }

    private fun flushSessionToDatabase(platformPackage: String) {
        val platform = targetPackages[platformPackage] ?: return
        val sessionData = ReelSessionManager.reset()
        val countToSave = sessionData[platform] ?: 0
        
        if (countToSave > 0) {
            serviceScope.launch {
                repeat(countToSave) {
                    database.reelDao().insert(ReelEvent(platform = platform))
                }
            }
        }
    }

    override fun onInterrupt() {
        lastPackage?.let { flushSessionToDatabase(it) }
        stopOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        lastPackage?.let { flushSessionToDatabase(it) }
        stopOverlay()
    }
}
