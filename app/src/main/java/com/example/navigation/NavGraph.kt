package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.state.ApiKeyViewModel
import com.example.state.AppThemeViewModel
import com.example.state.BannerCreationViewModel
import com.example.state.HistoryViewModel
import com.example.state.ImageEditorViewModel
import com.example.ui.screens.ApiKeySetupScreen
import com.example.ui.screens.GenerationSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageEditorScreen
import com.example.state.PromptChatViewModel
import com.example.ui.screens.PromptChatScreen
import com.example.ui.screens.PromptBuilderScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.state.AuthViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val API_KEY_SETUP = "api-key-setup"
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROMPT_CHAT = "prompt-chat"
    const val PROMPT_BUILDER = "prompt-builder"
    const val GENERATION_SETTINGS = "generation-settings"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val IMAGE_STUDIO = "image-studio"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    bannerViewModel: BannerCreationViewModel,
    apiKeyViewModel: ApiKeyViewModel,
    appThemeViewModel: AppThemeViewModel
) {
    val themeState by appThemeViewModel.state.collectAsState()
    val authViewModel: AuthViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val imageEditorViewModel: ImageEditorViewModel = viewModel()
    val promptChatViewModel: PromptChatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                apiKeyViewModel = apiKeyViewModel,
                onNavigateToApiKeySetup = {
                    navController.navigate(Routes.API_KEY_SETUP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.API_KEY_SETUP) {
            ApiKeySetupScreen(
                apiKeyViewModel = apiKeyViewModel,
                onContinueToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.API_KEY_SETUP) { inclusive = true }
                    }
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME)
                    }
                },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                bannerViewModel = bannerViewModel,
                onStartNewBanner = {
                    bannerViewModel.resetToDefaults()
                    navController.navigate(Routes.PROMPT_BUILDER)
                },
                onNavigateToPromptChat = {
                    navController.navigate(Routes.PROMPT_CHAT)
                },
                onNavigateToImageStudio = {
                    navController.navigate(Routes.IMAGE_STUDIO)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() },
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.PROMPT_CHAT) {
            PromptChatScreen(
                promptChatViewModel = promptChatViewModel,
                apiKeyViewModel = apiKeyViewModel,
                bannerViewModel = bannerViewModel,
                onUsePromptInBanner = {
                    navController.navigate(Routes.PROMPT_BUILDER)
                },
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.PROMPT_BUILDER) {
            PromptBuilderScreen(
                bannerViewModel = bannerViewModel,
                onNavigateNext = {
                    navController.navigate(Routes.GENERATION_SETTINGS)
                },
                onNavigateToPromptChat = {
                    navController.navigate(Routes.PROMPT_CHAT)
                },
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.GENERATION_SETTINGS) {
            GenerationSettingsScreen(
                bannerViewModel = bannerViewModel,
                apiKeyViewModel = apiKeyViewModel,
                onGenerateBanner = {
                    navController.navigate(Routes.RESULT)
                },
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                bannerViewModel = bannerViewModel,
                apiKeyViewModel = apiKeyViewModel,
                onStartOver = {
                    bannerViewModel.resetToDefaults()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onTweakStyle = {
                    navController.navigate(Routes.PROMPT_BUILDER)
                },
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                historyViewModel = historyViewModel,
                bannerViewModel = bannerViewModel,
                onSelectBanner = {
                    navController.navigate(Routes.RESULT)
                },
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.IMAGE_STUDIO) {
            ImageEditorScreen(
                imageEditorViewModel = imageEditorViewModel,
                apiKeyViewModel = apiKeyViewModel,
                onBack = { navController.popBackStack() },
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                apiKeyViewModel = apiKeyViewModel,
                appThemeViewModel = appThemeViewModel,
                onManageApiKey = {
                    navController.navigate(Routes.API_KEY_SETUP)
                },
                onBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
    }
}
