package com.example.ui.navigation

object THPDestinations {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding"

    // Primary Tabs
    const val HOME = "home"
    const val TASKS = "tasks"
    const val GOALS = "goals"
    const val PROGRESS = "progress"
    const val ME = "me"

    // Sub-screens
    const val GOAL_DETAIL = "goal_detail/{goalId}"
    fun goalDetailRoute(goalId: String) = "goal_detail/$goalId"

    const val SETTINGS = "settings"
    const val ABOUT = "about"
}
