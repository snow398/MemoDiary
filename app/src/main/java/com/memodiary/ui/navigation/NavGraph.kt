package com.memodiary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.memodiary.ui.screen.detail.MemoDetailScreen
import com.memodiary.ui.screen.edit.MemoEditScreen
import com.memodiary.ui.screen.timeline.TimelineScreen

private const val ROUTE_TIMELINE = "timeline"
private const val ROUTE_DETAIL = "memoDetail/{memoId}"
private const val ROUTE_EDIT = "memoEdit/{memoId}"

/**
 * Root navigation graph.
 *
 * Routes:
 *  - timeline            → main memo list
 *  - memoDetail/{id}     → read-only detail view
 *  - memoEdit/{id|"new"} → create ("new") or edit an existing memo
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_TIMELINE) {

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
            // "new" signals a creation; anything else is parsed as an existing id
            val memoId = if (memoIdStr == "new") null else memoIdStr?.toLongOrNull()
            MemoEditScreen(
                memoId = memoId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}