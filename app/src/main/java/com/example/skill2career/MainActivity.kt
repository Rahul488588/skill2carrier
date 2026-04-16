package com.example.skill2career

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.skill2career.ui.theme.SKILL2careerTheme

import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            SKILL2careerTheme {
                // Use a Surface to provide the default background color
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        // LOGIN
                        composable("login") {
                            LoginScreen(
                                mainViewModel = mainViewModel,
                                onLoginClick = {
                                    // Use role returned from server (stored in ViewModel), not selected role
                                    val actualRole = mainViewModel.currentUser.value?.role ?: "Student"
                                    if (actualRole == "Student") {
                                        navController.navigate("studentScreen") {
                                            // Clear entire back stack including login
                                            popUpTo("login") { inclusive = true }
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate("admin") {
                                            // Clear entire back stack including login
                                            popUpTo("login") { inclusive = true }
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onSignUpClick = {
                                    navController.navigate("signup")
                                }
                            )
                        }

                        // SIGNUP
                        composable("signup") {
                            SignUpScreen(
                                mainViewModel = mainViewModel,
                                onSignUpSuccess = {
                                    navController.navigate("login") {
                                        // Clear back stack to prevent going back to signup
                                        popUpTo("signup") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        // STUDENT DASHBOARD
                        composable("studentScreen") {
                            StudentScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // ADMIN DASHBOARD
                        composable("admin") {
                            AdminScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // OPPORTUNITIES with optional filter argument
                        composable(
                            route = "opportunities?filter={filter}",
                            arguments = listOf(
                                navArgument("filter") {
                                    type = NavType.StringType
                                    defaultValue = "All"
                                }
                            )
                        ) { backStackEntry ->
                            val filter = backStackEntry.arguments?.getString("filter") ?: "All"
                            OpportunitiesScreen(navController = navController, initialFilter = filter, mainViewModel = mainViewModel)
                        }

                        // MY APPLICATIONS
                        composable("myApplications") {
                            MyApplicationsScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // RESUME BUILDER
                        composable("resumeBuilder") {
                            ResumeBuilderScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // CGPA TRACKER
                        composable("cgpaTracker") {
                            CGPATrackerScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // SAVED
                        composable("saved") {
                            SavedScreen(navController = navController, mainViewModel = mainViewModel)
                        }

                        // PROFILE
                        composable("profile") {
                            ProfileScreen(navController = navController, mainViewModel = mainViewModel)
                        }
                    }
                }
            }
        }
    }
}
