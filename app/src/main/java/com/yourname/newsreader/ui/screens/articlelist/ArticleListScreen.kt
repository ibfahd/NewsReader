package com.yourname.newsreader.ui.screens.articlelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Article List Screen - Route-level composable.
 * 
 * This is the screen-level composable that:
 * - Observes ViewModel state
 * - Handles navigation events
 * - Delegates to stateless UI components
 * 
 * Following state hoisting patterns:
 * - State flows down from ViewModel
 * - Events flow up to ViewModel
 * - UI is declarative and testable
 */
@Composable
fun ArticleListScreen(
    viewModel: ArticleListViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle one-time navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToDetail -> {
                    onNavigateToDetail(event.articleId)
                }
            }
        }
    }
    
    // Delegate to stateless content composable
    ArticleListContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * Stateless content composable.
 * 
 * Benefits of being stateless:
 * - Easy to preview
 * - Easy to test
 * - Can be reused
 * - Clear contract via parameters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListContent(
    uiState: ArticleListState,
    onEvent: (ArticleListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News Reader") },
                actions = {
                    IconButton(onClick = { onEvent(ArticleListEvent.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (uiState) {
            is ArticleListState.Loading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is ArticleListState.Success -> {
                SuccessState(
                    articles = uiState.articles,
                    selectedCategory = uiState.selectedCategory,
                    favoriteIds = uiState.favoriteIds,
                    isRefreshing = uiState.isRefreshing,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is ArticleListState.Error -> {
                ErrorState(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(ArticleListEvent.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Loading state composable.
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Success state composable with pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessState(
    articles: List<Article>,
    selectedCategory: Category?,
    favoriteIds: Set<String>,
    isRefreshing: Boolean,
    onEvent: (ArticleListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Category filters
        CategoryFilters(
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                onEvent(ArticleListEvent.SelectCategory(category))
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Article list with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onEvent(ArticleListEvent.Refresh) },
            modifier = Modifier.fillMaxSize()
        ) {
            if (articles.isEmpty()) {
                EmptyState(
                    selectedCategory = selectedCategory,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = articles,
                        key = { it.id }
                    ) { article ->
                        ArticleCard(
                            article = article,
                            isFavorite = article.id in favoriteIds,
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
                }
            }
        }
    }
}

/**
 * Category filter chips.
 */
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
        // "All" filter
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        
        // Category filters
        items(Category.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) }
            )
        }
    }
}

/**
 * Individual article card.
 * 
 * This is a reusable, stateless component.
 */
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and favorite button
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
                
                IconButton(
                    onClick = { onFavoriteClick(!isFavorite) }
                ) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Description
            Text(
                text = article.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatDate(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state when no articles match filter.
 */
@Composable
private fun EmptyState(
    selectedCategory: Category?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (selectedCategory != null) {
                    "No ${selectedCategory.displayName.lowercase()} articles found"
                } else {
                    "No articles available"
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Try selecting a different category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state composable.
 */
@Composable
private fun ErrorState(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (canRetry) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Format date for display.
 */
private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}
