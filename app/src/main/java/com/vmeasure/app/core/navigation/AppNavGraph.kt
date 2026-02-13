package com.vmeasure.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vmeasure.app.feature.calendar.CalendarScreen
import com.vmeasure.app.feature.lists.ListsScreen
import com.vmeasure.app.feature.profile.ProfileScreen
import com.vmeasure.app.feature.settings.SettingsScreen
import com.vmeasure.app.feature.userform.AddUserScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LISTS
    ) {
        composable(Routes.LISTS) {
            ListsScreen(
                navController = navController,
                onAddUser = { navController.navigate(Routes.ADD_USER) }
            )
        }

        composable(Routes.ADD_USER) {
            AddUserScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    // Tell the previous screen (Lists) to refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("lists_refresh", true)

                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(Routes.CALENDAR) { CalendarScreen() }
        composable(Routes.PROFILE) { ProfileScreen() }
    }
}
