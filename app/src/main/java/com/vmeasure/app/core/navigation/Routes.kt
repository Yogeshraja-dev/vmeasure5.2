package com.vmeasure.app.core.navigation

object Routes {
    const val LISTS = "lists"
    const val ADD_USER = "add_user"
    const val DETAILS = "details"
    const val DETAILS_ARG_USER_ID = "publicUserId"
    const val SETTINGS = "settings"
    const val CALENDAR = "calendar"
    const val PROFILE = "profile"

    fun details(publicUserId: String) = "$DETAILS/$publicUserId"
}
