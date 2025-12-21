package com.example.thescreenshotbrain.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.thescreenshotbrain.presentation.screens.detail.DetailScreen
import com.example.thescreenshotbrain.presentation.screens.history.HistoryScreen
import com.example.thescreenshotbrain.presentation.screens.setting.SettingScreen


object Screen {
    const val History = "history_screen"
    const val Settings = "settings_screen"
    const val Detail = "detail_screen/{screenshotId}"

    fun passScreenshotId(id: Long): String {
        return "detail_screen/$id"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.History
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // 1.History
        composable(route = Screen.History) {
            HistoryScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToDetail = { screenshotId ->
                    navController.navigate(Screen.passScreenshotId(screenshotId))
                }
            )
        }

        // 2.Detail
        composable(
            route = Screen.Detail,
            arguments = listOf(
                navArgument("screenshotId") {
                    type = NavType.LongType
                }
            )
        ) {

            DetailScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Setting
        composable(route = Screen.Settings) {
            SettingScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
