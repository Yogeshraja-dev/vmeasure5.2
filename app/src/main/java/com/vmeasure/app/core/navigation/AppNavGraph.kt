package com.vmeasure.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vmeasure.app.feature.calendar.CalendarScreen
import com.vmeasure.app.feature.lists.ListsScreen
import com.vmeasure.app.feature.profile.ProfileScreen
import com.vmeasure.app.feature.settings.SettingsScreen
import com.vmeasure.app.feature.userform.AddUserScreen
import androidx.navigation.NavType
//import androidx.navigation.compose.navArgument
import com.vmeasure.app.feature.details.DetailsScreen
import androidx.navigation.navArgument
import com.vmeasure.app.feature.settings.DriveSyncViewModel

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
        composable(Routes.SETTINGS) {
            val vm: DriveSyncViewModel = viewModel()
            SettingsScreen(vm)
        }

//        composable(Routes.SETTINGS) { SettingsScreen(vm: DriveSyncViewModel) }
        composable(Routes.CALENDAR) { CalendarScreen() }
        composable(Routes.PROFILE) { ProfileScreen() }

        composable(
            route = "${Routes.DETAILS}/{${Routes.DETAILS_ARG_USER_ID}}",
            arguments = listOf(navArgument(Routes.DETAILS_ARG_USER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Routes.DETAILS_ARG_USER_ID)!!
            DetailsScreen(
                publicUserId = userId,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("lists_refresh", true)
                    navController.popBackStack()
                }
            )
        }

    }
}
