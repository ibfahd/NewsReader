package com.yourname.newsreader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yourname.newsreader.ui.screens.articledetail.ArticleDetailScreen
import com.yourname.newsreader.ui.screens.articledetail.ArticleDetailViewModel
import com.yourname.newsreader.ui.screens.articlelist.ArticleListScreen
import com.yourname.newsreader.ui.screens.articlelist.ArticleListViewModel

sealed class Screen(val route: String) {
    data object ArticleList : Screen("article_list")
    data object ArticleDetail : Screen("article_detail/{articleId}") {
        fun createRoute(articleId: String) = "article_detail/${Uri.encode(articleId)}"
    }
}

/**
 * Main navigation graph — dramatically simplified by Hilt.
 *
 * ─── What changed from Chapter 5 ──────────────────────────────────────────────
 *
 * BEFORE (Chapter 5 — manual wiring):
 *   val repository = MockNewsRepository()               // create dependency
 *   val viewModel = viewModel(                          // create VM with factory
 *       factory = ArticleListViewModelFactory(
 *           repository = repository,
 *           savedStateHandle = SavedStateHandle()
 *       )
 *   )
 *
 * AFTER (Chapter 6 — Hilt):
 *   val viewModel: ArticleListViewModel = hiltViewModel()   // that's it
 *
 * ─── hiltViewModel() under the hood ──────────────────────────────────────────
 * hiltViewModel() (from hilt-navigation-compose) does three things:
 *
 *   1. Asks Hilt to construct the ViewModel, resolving the entire dependency
 *      tree: NewsRepository → NewsRepositoryImpl → ArticleDao + RemoteDataSource
 *      + UserPreferencesDataStore.
 *
 *   2. Scopes the ViewModel to the current NavBackStackEntry's lifecycle
 *      (not the Activity). The ViewModel is cleared when the back-stack entry
 *      is popped — correct memory management.
 *
 *   3. Passes the NavBackStackEntry's SavedStateHandle to the ViewModel.
 *      For ArticleDetailViewModel, the "articleId" route argument is already
 *      in this SavedStateHandle — so savedStateHandle.get<String>("articleId")
 *      still works with zero changes to the ViewModel logic.
 *
 * The ViewModel factory classes (ArticleListViewModelFactory,
 * ArticleDetailViewModelFactory) are now deleted — Hilt generates them.
 */
@Composable
fun NewsNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ArticleList.route,
        modifier = modifier
    ) {
        composable(Screen.ArticleList.route) {
            // Hilt resolves the full dependency graph automatically.
            val viewModel: ArticleListViewModel = hiltViewModel()
            ArticleListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) {
            // Navigation Compose puts "articleId" into this entry's SavedStateHandle
            // before creating the ViewModel — no manual SavedStateHandle setup needed.
            val viewModel: ArticleDetailViewModel = hiltViewModel()
            ArticleDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}