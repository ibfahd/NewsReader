package com.yourname.newsreader.ui.screens.articlelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ch.7: The screen now works with [LazyPagingItems] for the article list.
 *
 * [collectAsLazyPagingItems] subscribes to the ViewModel's paging flow and
 * returns a [LazyPagingItems] object that provides:
 *   - [itemCount]: how many items are currently available.
 *   - [get(index)]: the item at a position (null if not yet loaded).
 *   - [loadState]: the current loading state for refresh, prepend, and append.
 *   - [refresh()]: trigger a full refresh (calls RemoteMediator with REFRESH).
 *   - [retry()]: retry a failed load.
 */
@Composable
fun ArticleListScreen(
    viewModel: ArticleListViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // collectAsLazyPagingItems MUST be called in the composable — not in the ViewModel.
    // This ensures the Paging library's lifecycle is tied to the composition.
    val pagingItems = viewModel.articlePagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToDetail -> onNavigateToDetail(event.articleId)
            }
        }
    }

    ArticleListContent(
        uiState = uiState,
        pagingItems = pagingItems,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListContent(
    uiState: ArticleListUiState,
    pagingItems: LazyPagingItems<Article>,
    onEvent: (ArticleListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News Reader") },
                actions = {
                    // The refresh button now calls pagingItems.refresh() directly.
                    // This triggers LoadType.REFRESH in ArticleRemoteMediator, which
                    // clears old data and fetches a fresh first page.
                    IconButton(onClick = { pagingItems.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategoryFilters(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { onEvent(ArticleListEvent.SelectCategory(it)) }
            )

            // ─── State routing via loadState ───────────────────────────────
            // loadState.refresh reflects the state of the most recent REFRESH
            // load — the initial load or a user-triggered refresh.
            //
            // loadState.append reflects appending more pages to the end of
            // the list — triggered automatically as the user scrolls.
            when {
                // Full-screen loading: shown only on the very first load when
                // the list is completely empty. After that, the existing items
                // stay visible while new items are appended below.
                pagingItems.loadState.refresh is LoadState.Loading
                        && pagingItems.itemCount == 0 -> {
                    FullScreenLoading()
                }

                // Full-screen error: shown if the initial load fails entirely.
                pagingItems.loadState.refresh is LoadState.Error
                        && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    FullScreenError(
                        message = friendlyErrorMessage(error),
                        onRetry = { pagingItems.refresh() }
                    )
                }

                // Success or partial states: the list is shown, with load-more
                // indicators or errors at the bottom.
                else -> {
                    // PullToRefreshBox integrates with loadState.refresh automatically.
                    PullToRefreshBox(
                        isRefreshing = pagingItems.loadState.refresh is LoadState.Loading,
                        onRefresh = { pagingItems.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Article items. pagingItems[index] returns null for
                            // placeholder positions — skip those gracefully.
                            items(
                                count = pagingItems.itemCount,
                                key = { index -> pagingItems[index]?.id ?: index }
                            ) { index ->
                                val article = pagingItems[index] ?: return@items
                                ArticleCard(
                                    article = article,
                                    isFavorite = article.id in uiState.favoriteIds,
                                    onArticleClick = {
                                        onEvent(ArticleListEvent.ArticleClicked(article.id))
                                    },
                                    onFavoriteClick = { isFavorite ->
                                        onEvent(
                                            ArticleListEvent.ToggleFavorite(
                                                articleId = article.id,
                                                isFavorite = isFavorite
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }

                            // Append loading indicator — shown at the bottom while
                            // the next page is being fetched as the user scrolls down.
                            item {
                                when (pagingItems.loadState.append) {
                                    is LoadState.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                    is LoadState.Error -> {
                                        val error = (pagingItems.loadState.append as LoadState.Error).error
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = friendlyErrorMessage(error),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Button(onClick = { pagingItems.retry() }) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilters(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(Category.entries.size) { index ->
            val category = Category.entries[index]
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) }
            )
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    isFavorite: Boolean,
    onArticleClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onArticleClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onFavoriteClick(!isFavorite) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites"
                        else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = article.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Converts a raw [Throwable] from Paging's [LoadState.Error] into a message
 * suitable for display to the user.
 *
 * Users don't need to see stack traces or HTTP jargon — they need to know
 * whether the problem is on their end (no internet) or the server's end.
 */
private fun friendlyErrorMessage(throwable: Throwable): String = when {
    throwable is java.io.IOException -> "No internet connection. Pull down to retry."
    throwable is retrofit2.HttpException && throwable.code() == 401 ->
        "Invalid API key. Check your NewsAPI configuration."
    throwable is retrofit2.HttpException && throwable.code() == 429 ->
        "Too many requests. Please wait a moment and retry."
    throwable is retrofit2.HttpException ->
        "Server error (${throwable.code()}). Please try again."
    else -> "An unexpected error occurred. Please try again."
}