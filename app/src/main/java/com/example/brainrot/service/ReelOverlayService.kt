package com.example.brainrot.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
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
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.brainrot.data.ReelDatabase
import com.example.brainrot.ui.theme.BrainRotTheme
import java.util.*

class ReelOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var igView: ComposeView? = null
    private var ytView: ComposeView? = null
    private var totalView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (igView == null) {
            showTripleOverlays()
        }
        return START_STICKY
    }

    private fun showTripleOverlays() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        // Calculate dimensions for equal division
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val sideMarginPx = (12 * density).toInt()
        val gapPx = (8 * density).toInt()
        val islandWidthPx = (screenWidth - (2 * sideMarginPx) - (2 * gapPx)) / 3

        fun createParams(gravity: Int) = WindowManager.LayoutParams(
            islandWidthPx,
            (50 * density).toInt(), // Fixed height
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity or Gravity.TOP
            y = (35 * density).toInt() // Position below status bar
        }

        // 1. Instagram Overlay (Left)
        igView = createOverlay(createParams(Gravity.START).apply { x = sideMarginPx }) {
            IslandBox(platform = "Instagram", icon = Icons.Rounded.CameraAlt, color = Color(0xFFE1306C))
        }

        // 2. Total Overlay (Center)
        totalView = createOverlay(createParams(Gravity.CENTER_HORIZONTAL)) {
            IslandBox(platform = "Total", iconText = "💀", color = Color.White)
        }

        // 3. YouTube Overlay (Right)
        ytView = createOverlay(createParams(Gravity.END).apply { x = sideMarginPx }) {
            IslandBox(platform = "YouTube", icon = Icons.Rounded.PlayArrow, color = Color(0xFFFF0000))
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun createOverlay(params: WindowManager.LayoutParams, content: @Composable () -> Unit): ComposeView {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ReelOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ReelOverlayService)
            setViewTreeViewModelStoreOwner(this, this@ReelOverlayService)
            setContent {
                BrainRotTheme {
                    content()
                }
            }
        }
        windowManager.addView(view, params)
        return view
    }

    @Composable
    fun IslandBox(platform: String, icon: ImageVector? = null, iconText: String? = null, color: Color) {
        val database = remember { ReelDatabase.getDatabase(this@ReelOverlayService) }
        val todayStart = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val sessionCounts by ReelSessionManager.sessionCounts.collectAsState()
        
        // Accurate combined count for this specific island
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
            // "Curved from two sides" aesthetic
            shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
            color = Color.Black.copy(alpha = 0.85f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertizontally,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                } else if (iconText != null) {
                    Text(iconText, fontSize = 16.sp)
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
        igView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        ytView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        totalView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setViewTreeViewModelStoreOwner(view: View, owner: ViewModelStoreOwner) {
        view.setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, owner)
    }
}
