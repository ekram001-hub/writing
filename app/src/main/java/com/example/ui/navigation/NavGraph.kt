package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.database.MicroWriteDatabase
import com.example.ui.feature.contentmanager.ContentManagerScreen
import com.example.ui.feature.contentmanager.ContentManagerViewModel
import com.example.ui.feature.home.HomeScreen
import com.example.ui.feature.home.HomeViewModel
import com.example.ui.feature.library.LibraryScreen
import com.example.ui.feature.library.LibraryViewModel
import com.example.ui.feature.practice.PracticeScreen
import com.example.ui.feature.practice.PracticeViewModel
import com.example.ui.feature.settings.SettingsScreen
import com.example.ui.feature.settings.SettingsViewModel
import com.example.ui.feature.topicdetails.TopicDetailsScreen
import com.example.ui.feature.topicdetails.TopicDetailsViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MicroWriteNavApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val coroutineScope = rememberCoroutineScope()

    // Determine if bottom bar should be visible
    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.PracticeRoot.route,
        Screen.Manage.route
    )

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (screen == Screen.PracticeRoot) {
                                    // Handle Practice Tab: check for active session, otherwise open library
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val db = MicroWriteDatabase.getDatabase(context)
                                        val active = db.topicProgressDao().observeLatestActiveTopic().first()
                                        withContext(Dispatchers.Main) {
                                            if (active != null) {
                                                navController.navigate(
                                                    Screen.PracticeSession.createRoute(
                                                        active.topicId,
                                                        active.writingMode,
                                                        active.currentSectionId
                                                    )
                                                )
                                            } else {
                                                navController.navigate(Screen.Library.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToTopicDetails = { topicId ->
                        navController.navigate(Screen.TopicDetails.createRoute(topicId))
                    },
                    onResumePractice = { topicId, mode, sectionId ->
                        navController.navigate(
                            Screen.PracticeSession.createRoute(topicId, mode, sectionId)
                        )
                    },
                    onNavigateToManage = { navController.navigate(Screen.Manage.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Library Screen
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel()
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onTopicSelected = { topicId ->
                        navController.navigate(Screen.TopicDetails.createRoute(topicId))
                    },
                    onNavigateToManage = { navController.navigate(Screen.Manage.route) }
                )
            }

            // Topic Details Screen
            composable(
                route = Screen.TopicDetails.route,
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                val detailsViewModel: TopicDetailsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return TopicDetailsViewModel(app, topicId) as T
                        }
                    }
                )
                TopicDetailsScreen(
                    viewModel = detailsViewModel,
                    onBack = { navController.popBackStack() },
                    onStartPractice = { tId, mode, sId ->
                        navController.navigate(Screen.PracticeSession.createRoute(tId, mode, sId))
                    }
                )
            }

            // Guided Practice Session Screen
            composable(
                route = Screen.PracticeSession.route,
                arguments = listOf(
                    navArgument("topicId") { type = NavType.StringType },
                    navArgument("writingMode") { type = NavType.StringType },
                    navArgument("sectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                val writingMode = backStackEntry.arguments?.getString("writingMode") ?: "english_focus"
                val sectionId = backStackEntry.arguments?.getString("sectionId") ?: ""

                val practiceViewModel: PracticeViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return PracticeViewModel(app, topicId, writingMode, sectionId) as T
                        }
                    }
                )

                PracticeScreen(
                    viewModel = practiceViewModel,
                    onExitPractice = {
                        navController.popBackStack()
                    }
                )
            }

            // Content Manager Screen
            composable(Screen.Manage.route) {
                val contentViewModel: ContentManagerViewModel = viewModel()
                ContentManagerScreen(viewModel = contentViewModel)
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
