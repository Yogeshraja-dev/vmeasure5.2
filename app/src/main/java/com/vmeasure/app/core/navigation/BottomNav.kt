package com.vmeasure.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
    BottomNavItem(Routes.LISTS, "Lists", Icons.Outlined.List),
    BottomNavItem(Routes.CALENDAR, "Calendar", Icons.Outlined.CalendarMonth),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Outlined.Person),
)
