package com.coby.surasura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.coby.surasura.ui.home.HomeScreen
import com.coby.surasura.ui.home.HomeViewModel
import com.coby.surasura.ui.theme.SuraSuraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            SuraSuraTheme(
                darkTheme = state.appColorScheme.isDark(this),
                dynamicColor = false
            ) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
