package com.garvjain.reelrot

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.garvjain.reelrot.data.ReelDatabase
import com.garvjain.reelrot.service.ReelAccessibilityService
import com.garvjain.reelrot.ui.DashboardScreen
import com.garvjain.reelrot.ui.SettingsScreen
import com.garvjain.reelrot.ui.WelcomeScreen
import com.garvjain.reelrot.ui.theme.ReelRotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = ReelDatabase.getDatabase(this)
        val reelDao = database.reelDao()

        enableEdgeToEdge()
        setContent {
            ReelRotTheme {
                val context = LocalContext.current
                var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
                
                // Re-check permission whenever the user returns to the app
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isServiceEnabled = isAccessibilityServiceEnabled(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (!isServiceEnabled) {
                    WelcomeScreen()
                } else {
                    MainAppContent(reelDao)
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Use .name instead of canonicalName for stable identification
        val service = "${context.packageName}/${ReelAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServices == null) return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(service, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}

@Composable
fun MainAppContent(reelDao: com.garvjain.reelrot.data.ReelDao) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(reelDao = reelDao)
                1 -> SettingsScreen()
            }
        }
    }
}
