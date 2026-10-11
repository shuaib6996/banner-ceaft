package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.navigation.AppNavHost
import com.example.state.ApiKeyViewModel
import com.example.state.AppThemeViewModel
import com.example.state.BannerCreationViewModel
import com.example.ui.theme.BannerCraftTheme

class MainActivity : ComponentActivity() {

    private val bannerViewModel: BannerCreationViewModel by viewModels()
    private val apiKeyViewModel: ApiKeyViewModel by viewModels()
    private val appThemeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by appThemeViewModel.state.collectAsState()
            BannerCraftTheme(darkTheme = themeState.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        bannerViewModel = bannerViewModel,
                        apiKeyViewModel = apiKeyViewModel,
                        appThemeViewModel = appThemeViewModel
                    )
                }
            }
        }
    }
}
