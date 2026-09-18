package com.mindrelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.nav.rememberMindNavController
import com.mindrelay.nav.MindNavHost
import com.mindrelay.ui.appViewModel
import com.mindrelay.ui.theme.MindRelayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MindRelayRoot()
        }
    }
}

@Composable
private fun MindRelayRoot() {
    val vm = appViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val darkTheme = when (settings.theme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }
    MindRelayTheme(darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize()) {
            val navController = rememberMindNavController()
            MindNavHost(vm = vm, nav = navController)
        }
    }
}
