package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )

    data object PracticeRoot : Screen(
        route = "practice_root",
        title = "Practice",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote
    )

    data object Manage : Screen(
        route = "manage",
        title = "Manage",
        selectedIcon = Icons.Filled.FolderZip,
        unselectedIcon = Icons.Outlined.FolderZip
    )

    data object TopicDetails : Screen(
        route = "topic_details/{topicId}",
        title = "Topic Details"
    ) {
        fun createRoute(topicId: String) = "topic_details/$topicId"
    }

    data object PracticeSession : Screen(
        route = "practice_session/{topicId}/{writingMode}/{sectionId}",
        title = "Guided Practice"
    ) {
        fun createRoute(topicId: String, writingMode: String, sectionId: String) =
            "practice_session/$topicId/$writingMode/$sectionId"
    }

    data object Settings : Screen(
        route = "settings",
        title = "Settings"
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOfNotNull(Home, Library, PracticeRoot, Manage)
    }
}
