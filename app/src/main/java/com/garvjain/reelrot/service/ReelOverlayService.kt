package com.garvjain.reelrot.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.garvjain.reelrot.R
import com.garvjain.reelrot.data.ReelDatabase
import com.garvjain.reelrot.ui.theme.ReelRotTheme
import java.util.*

class ReelOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var platformView: ComposeView? = null
    private var totalView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var currentPlatform = mutableStateOf<String?>(null)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    companion object {
        private const val CHANNEL_ID = "reelrot_overlay"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReelRot is active")
            .setContentText("Counting your reels in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ReelRot Overlay",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Shows while ReelRot is counting reels" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val platform = intent?.getStringExtra("PLATFORM")
        currentPlatform.value = platform
        
        if (totalView == null) {
            showBubbles()
        }
        
        platformView?.visibility = if (platform != null) View.VISIBLE else View.GONE
        
        return START_STICKY
    }

    private fun showBubbles() {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val pillWidthPx = (110 * density).toInt()
        val pillHeightPx = (50 * density).toInt()

        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }
        val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }

        val topLimit = statusBarHeight + (72 * density).toInt()
        val bottomLimit = screenHeight - navBarHeight - pillHeightPx - (72 * density).toInt()
        val rightLimit = screenWidth - pillWidthPx - (16 * density).toInt()

        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)

        // 1. Platform Pill
        val pX = prefs.getInt("platform_x", rightLimit)
        val pY = prefs.getInt("platform_y", topLimit)
        platformView = createDraggableOverlay(pX, pY, pillWidthPx, pillHeightPx, "platform") {
            val platform = currentPlatform.value
            if (platform != null) {
                val icon = if (platform == "Instagram") Icons.Rounded.CameraAlt else Icons.Rounded.PlayArrow
                val color = if (platform == "Instagram") Color(0xFFE1306C) else Color(0xFFFF0000)
                IslandBox(platform = platform, icon = icon, color = color)
            }
        }

        // 2. Total Pill
        val tX = prefs.getInt("total_x", rightLimit)
        val tY = prefs.getInt("total_y", bottomLimit)
        totalView = createDraggableOverlay(tX, tY, pillWidthPx, pillHeightPx, "total") {
            IslandBox(platform = "Total", icon = Icons.Rounded.LocalFireDepartment, color = Color(0xFFF0F0F0))
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createDraggableOverlay(
        initialX: Int,
        initialY: Int,
        widthPx: Int,
        heightPx: Int,
        prefsKeyPrefix: String,
        content: @Composable () -> Unit
    ): ComposeView {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ReelOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ReelOverlayService)
            setViewTreeViewModelStoreOwner(this, this@ReelOverlayService)
            setContent {
                ReelRotTheme {
                    content()
                }
            }
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var startX = 0
            private var startY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - initialTouchX).toInt()
                        params.y = startY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        clampAndSave(view, params, prefsKeyPrefix)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(view, params)
        return view
    }

    private fun clampAndSave(view: View, params: WindowManager.LayoutParams, prefix: String) {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val pillWidthPx = (110 * density).toInt()
        val pillHeightPx = (50 * density).toInt()

        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }
        val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }

        val topLimit = statusBarHeight + (72 * density).toInt()
        val bottomLimit = screenHeight - navBarHeight - pillHeightPx - (72 * density).toInt()
        val leftLimit = (16 * density).toInt()
        val rightLimit = screenWidth - pillWidthPx - (16 * density).toInt()

        params.x = params.x.coerceIn(leftLimit, rightLimit)
        params.y = params.y.coerceIn(topLimit, bottomLimit)
        windowManager.updateViewLayout(view, params)

        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("${prefix}_x", params.x)
            .putInt("${prefix}_y", params.y)
            .apply()
    }

    @Composable
    fun IslandBox(platform: String, icon: ImageVector? = null, color: Color) {
        val database = remember { ReelDatabase.getDatabase(this@ReelOverlayService) }
        val todayStart = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val sessionCounts by ReelSessionManager.sessionCounts.collectAsState()
        
        val count = if (platform == "Total") {
            val igDb by database.reelDao().getTodayCountByApp("Instagram", todayStart).collectAsState(initial = 0)
            val ytDb by database.reelDao().getTodayCountByApp("YouTube", todayStart).collectAsState(initial = 0)
            igDb + ytDb + (sessionCounts["Instagram"] ?: 0) + (sessionCounts["YouTube"] ?: 0)
        } else {
            val dbCount by database.reelDao().getTodayCountByApp(platform, todayStart).collectAsState(initial = 0)
            dbCount + (sessionCounts[platform] ?: 0)
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
            color = Color.Black.copy(alpha = 0.85f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = count.toString(),
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        platformView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        totalView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setViewTreeViewModelStoreOwner(view: View, owner: ViewModelStoreOwner) {
        view.setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, owner)
    }
}
