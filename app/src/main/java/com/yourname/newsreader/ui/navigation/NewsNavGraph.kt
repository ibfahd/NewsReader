package com.yourname.newsreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yourname.newsreader.data.repository.MockNewsRepository
import com.yourname.newsreader.ui.screens.articledetail.ArticleDetailScreen
import com.yourname.newsreader.ui.screens.articledetail.ArticleDetailViewModel
import com.yourname.newsreader.ui.screens.articledetail.ArticleDetailViewModelFactory
import com.yourname.newsreader.ui.screens.articlelist.ArticleListScreen
import com.yourname.newsreader.ui.screens.articlelist.ArticleListViewModel
import com.yourname.newsreader.ui.screens.articlelist.ArticleListViewModelFactory

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object ArticleList : Screen("article_list")
    data object ArticleDetail : Screen("article_detail/{articleId}") {
        fun createRoute(articleId: String) = "article_detail/$articleId"
    }
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun NewsNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Create repository instance
    // In later chapters, this will be injected via Hilt
    val repository = MockNewsRepository()

    NavHost(
        navController = navController,
        startDestination = Screen.ArticleList.route,
        modifier = modifier
    ) {
        // Article List Screen
        composable(Screen.ArticleList.route) {
            val viewModel: ArticleListViewModel = viewModel(
                factory = ArticleListViewModelFactory(
                    repository = repository,
                    savedStateHandle = SavedStateHandle()
                )
            )

            ArticleListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // Article Detail Screen
        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(
                navArgument("articleId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
            val savedStateHandle = SavedStateHandle().apply {
                set(ArticleDetailViewModel.ARG_ARTICLE_ID, articleId)
            }

            val viewModel: ArticleDetailViewModel = viewModel(
                factory = ArticleDetailViewModelFactory(
                    repository = repository,
                    savedStateHandle = savedStateHandle
                )
            )

            ArticleDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
