package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.model.ThemePreference
import com.example.ui.components.THPBottomNavigation
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.screens.goals.GoalDetailScreen
import com.example.ui.screens.goals.GoalViewModel
import com.example.ui.screens.goals.GoalsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.progress.ProgressViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TaskHabitViewModel
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.theme.THPTrackerTheme

@Composable
fun THPAppRoot(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val themePreference = currentUser?.settings?.theme ?: ThemePreference.SYSTEM
    val goalViewModel: GoalViewModel = viewModel()
    val taskHabitViewModel: TaskHabitViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val progressViewModel: ProgressViewModel = viewModel()

    LaunchedEffect(currentUser) {
        currentUser?.let {
            goalViewModel.setUserId(it.id)
            taskHabitViewModel.setUser(it.id, it.timezone)
            homeViewModel.setUser(it.id, it.timezone)
            progressViewModel.setUser(it)
        }
    }

    val activeGoals by goalViewModel.filteredGoals.collectAsState()

    THPTrackerTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: THPDestinations.SPLASH

        val isPrimaryTab = currentRoute in listOf(
            THPDestinations.HOME,
            THPDestinations.TASKS,
            THPDestinations.GOALS,
            THPDestinations.PROGRESS,
            THPDestinations.ME
        )

        Scaffold(
            bottomBar = {
                if (isPrimaryTab) {
                    THPBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { targetRoute ->
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = THPDestinations.SPLASH,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(THPDestinations.SPLASH) {
                        SplashScreen(
                            viewModel = viewModel,
                            onNavigateToHome = {
                                navController.navigate(THPDestinations.HOME) {
                                    popUpTo(THPDestinations.SPLASH) { inclusive = true }
                                }
                            },
                            onNavigateToOnboarding = {
                                navController.navigate(THPDestinations.ONBOARDING) {
                                    popUpTo(THPDestinations.SPLASH) { inclusive = true }
                                }
                            },
                            onNavigateToWelcome = {
                                navController.navigate(THPDestinations.WELCOME) {
                                    popUpTo(THPDestinations.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(THPDestinations.WELCOME) {
                        WelcomeScreen(
                            onNavigateToLogin = { navController.navigate(THPDestinations.LOGIN) },
                            onNavigateToRegister = { navController.navigate(THPDestinations.REGISTER) },
                            onGuestLogin = {
                                viewModel.signInGuest {
                                    navController.navigate(THPDestinations.ONBOARDING) {
                                        popUpTo(THPDestinations.WELCOME) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(THPDestinations.LOGIN) {
                        BackHandler {
                            navController.popBackStack()
                        }
                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRegister = {
                                navController.navigate(THPDestinations.REGISTER) {
                                    popUpTo(THPDestinations.LOGIN) { inclusive = true }
                                }
                            },
                            onNavigateToForgotPassword = { navController.navigate(THPDestinations.FORGOT_PASSWORD) },
                            onLoginSuccess = {
                                val destination = if (currentUser?.onboardingCompleted == true) {
                                    THPDestinations.HOME
                                } else {
                                    THPDestinations.ONBOARDING
                                }
                                navController.navigate(destination) {
                                    popUpTo(THPDestinations.WELCOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(THPDestinations.REGISTER) {
                        BackHandler {
                            navController.popBackStack()
                        }
                        RegisterScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {
                                navController.navigate(THPDestinations.LOGIN) {
                                    popUpTo(THPDestinations.REGISTER) { inclusive = true }
                                }
                            },
                            onRegisterSuccess = {
                                navController.navigate(THPDestinations.ONBOARDING) {
                                    popUpTo(THPDestinations.WELCOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(THPDestinations.FORGOT_PASSWORD) {
                        BackHandler {
                            navController.popBackStack()
                        }
                        ForgotPasswordScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(THPDestinations.ONBOARDING) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onOnboardingComplete = {
                                navController.navigate(THPDestinations.HOME) {
                                    popUpTo(THPDestinations.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Primary Destinations
                    composable(THPDestinations.HOME) {
                        HomeScreen(
                            userProfile = currentUser,
                            viewModel = homeViewModel,
                            onNavigateToGoals = { navController.navigate(THPDestinations.GOALS) },
                            onNavigateToGoalDetail = { goalId ->
                                goalViewModel.selectGoal(goalId)
                                navController.navigate(THPDestinations.goalDetailRoute(goalId))
                            },
                            onNavigateToTasks = { navController.navigate(THPDestinations.TASKS) },
                            onNavigateToProfile = { navController.navigate(THPDestinations.ME) },
                            onNavigateToSettings = { navController.navigate(THPDestinations.SETTINGS) }
                        )
                    }

                    composable(THPDestinations.TASKS) {
                        TasksScreen(
                            userProfile = currentUser,
                            viewModel = taskHabitViewModel,
                            onNavigateToProfile = { navController.navigate(THPDestinations.ME) },
                            onNavigateToSettings = { navController.navigate(THPDestinations.SETTINGS) }
                        )
                    }

                    composable(THPDestinations.GOALS) {
                        GoalsScreen(
                            userProfile = currentUser,
                            viewModel = goalViewModel,
                            onNavigateToGoalDetail = { goalId ->
                                goalViewModel.selectGoal(goalId)
                                navController.navigate(THPDestinations.goalDetailRoute(goalId))
                            },
                            onNavigateToProfile = { navController.navigate(THPDestinations.ME) },
                            onNavigateToSettings = { navController.navigate(THPDestinations.SETTINGS) }
                        )
                    }

                    composable(
                        route = THPDestinations.GOAL_DETAIL,
                        arguments = listOf(navArgument("goalId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
                        LaunchedEffect(goalId) {
                            goalViewModel.selectGoal(goalId)
                        }
                        BackHandler {
                            navController.popBackStack()
                        }
                        GoalDetailScreen(
                            goalId = goalId,
                            viewModel = goalViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(THPDestinations.PROGRESS) {
                        ProgressScreen(
                            userProfile = currentUser,
                            viewModel = progressViewModel,
                            onNavigateToProfile = { navController.navigate(THPDestinations.ME) },
                            onNavigateToSettings = { navController.navigate(THPDestinations.SETTINGS) }
                        )
                    }

                    composable(THPDestinations.ME) {
                        ProfileScreen(
                            userProfile = currentUser,
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate(THPDestinations.SETTINGS) },
                            onSignedOut = {
                                navController.navigate(THPDestinations.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(THPDestinations.SETTINGS) {
                        BackHandler {
                            navController.popBackStack()
                        }
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
