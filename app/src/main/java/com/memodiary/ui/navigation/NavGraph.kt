package com.memodiary.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.memodiary.ui.screen.detail.MemoDetailScreen
import com.memodiary.ui.screen.edit.MemoEditScreen
import com.memodiary.ui.screen.footprint.FootprintDetailScreen
import com.memodiary.ui.screen.footprint.FootprintListScreen
import com.memodiary.ui.screen.timeline.TimelineScreen

private const val ROUTE_TIMELINE = "timeline"
private const val ROUTE_FOOTPRINT = "footprintList"
private const val ROUTE_FOOTPRINT_DETAIL = "footprintDetail/{city}"
private const val ROUTE_DETAIL = "memoDetail/{memoId}"
private const val ROUTE_EDIT = "memoEdit/{memoId}"

private data class TabItem(val label: String, val icon: ImageVector, val route: String)

private val tabs = listOf(
    TabItem("笔记", Icons.Default.Edit, ROUTE_TIMELINE),
    TabItem("足迹", Icons.Default.LocationOn, ROUTE_FOOTPRINT)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on top-level tab screens
    val showBottomBar = currentRoute in listOf(ROUTE_TIMELINE, ROUTE_FOOTPRINT)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_TIMELINE) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_TIMELINE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_TIMELINE) {
                TimelineScreen(
                    onMemoClick = { memoId ->
                        navController.navigate("memoDetail/$memoId")
                    },
                    onAddMemo = {
                        navController.navigate("memoEdit/new")
                    }
                )
            }

            composable(ROUTE_FOOTPRINT) {
                FootprintListScreen(
                    onCityClick = { city ->
                        navController.navigate("footprintDetail/$city")
                    }
                )
            }

            composable(ROUTE_FOOTPRINT_DETAIL) { backStackEntry ->
                val city = backStackEntry.arguments?.getString("city") ?: return@composable
                FootprintDetailScreen(
                    city = city,
                    onBack = { navController.popBackStack() },
                    onMemoClick = { memoId ->
                        navController.navigate("memoDetail/$memoId")
                    }
                )
            }

            composable(ROUTE_DETAIL) { backStackEntry ->
                val memoId = backStackEntry.arguments
                    ?.getString("memoId")?.toLongOrNull()
                    ?: return@composable
                MemoDetailScreen(
                    memoId = memoId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("memoEdit/$memoId") }
                )
            }

            composable(ROUTE_EDIT) { backStackEntry ->
                val memoIdStr = backStackEntry.arguments?.getString("memoId")
                val memoId = if (memoIdStr == "new") null else memoIdStr?.toLongOrNull()
                MemoEditScreen(
                    memoId = memoId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}