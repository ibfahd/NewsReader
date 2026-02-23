package com.yourname.newsreader.ui.screens.articlelist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import com.yourname.newsreader.data.repository.NewsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for ArticleListViewModel.
 * 
 * These tests demonstrate:
 * - Testing StateFlow emissions
 * - Mocking repositories with MockK
 * - Testing coroutines with test dispatchers
 * - Verifying state transitions
 * - Testing SavedStateHandle integration
 * 
 * Test structure follows:
 * - Given: Setup initial conditions
 * - When: Perform action
 * - Then: Verify expected outcome
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {
    
    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()
    
    // Mocks
    private lateinit var repository: NewsRepository
    private lateinit var savedStateHandle: SavedStateHandle
    
    // System under test
    private lateinit var viewModel: ArticleListViewModel
    
    // Test data
    private val testArticles = listOf(
        Article(
            id = "1",
            title = "Test Article 1",
            description = "Description 1",
            content = "Content 1",
            author = "Author 1",
            publishedAt = Date(),
            imageUrl = null,
            category = Category.TECHNOLOGY,
            source = "Test Source",
            url = "https://test.com/1"
        ),
        Article(
            id = "2",
            title = "Test Article 2",
            description = "Description 2",
            content = "Content 2",
            author = "Author 2",
            publishedAt = Date(),
            imageUrl = null,
            category = Category.BUSINESS,
            source = "Test Source",
            url = "https://test.com/2"
        )
    )
    
    @Before
    fun setup() {
        // Set main dispatcher for testing
        Dispatchers.setMain(testDispatcher)
        
        // Create mocks
        repository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        
        // Setup default repository behavior
        every { repository.getArticles(any()) } returns flowOf(testArticles)
        every { repository.getFavoriteIds() } returns flowOf(emptySet())
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    /**
     * Test: Initial state should be Loading, then Success.
     */
    @Test
    fun `initial state is Loading then Success`() = runTest {
        // When
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // Then
        viewModel.uiState.test {
            // First emission is Loading
            val loadingState = awaitItem()
            assertTrue(loadingState is ArticleListState.Loading)
            
            // Second emission is Success
            val successState = awaitItem()
            assertTrue(successState is ArticleListState.Success)
            assertEquals(testArticles, (successState as ArticleListState.Success).articles)
        }
    }
    
    /**
     * Test: Refresh should update articles.
     */
    @Test
    fun `refresh updates articles`() = runTest {
        // Given
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // Skip initial emissions
        viewModel.uiState.test {
            skipItems(2) // Skip Loading and initial Success
            
            // When
            viewModel.onEvent(ArticleListEvent.Refresh)
            
            // Then
            coVerify { repository.refreshArticles() }
        }
    }
    
    /**
     * Test: Selecting category filters articles.
     */
    @Test
    fun `selecting category filters articles`() = runTest {
        // Given
        val technologyArticles = testArticles.filter { 
            it.category == Category.TECHNOLOGY 
        }
        every { repository.getArticles(Category.TECHNOLOGY) } returns 
            flowOf(technologyArticles)
        
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        viewModel.uiState.test {
            skipItems(2) // Skip Loading and initial Success
            
            // When
            viewModel.onEvent(ArticleListEvent.SelectCategory(Category.TECHNOLOGY))
            
            // Then
            val state = awaitItem()
            assertTrue(state is ArticleListState.Success)
            assertEquals(
                Category.TECHNOLOGY,
                (state as ArticleListState.Success).selectedCategory
            )
        }
    }
    
    /**
     * Test: Category is persisted in SavedStateHandle.
     */
    @Test
    fun `category is persisted in SavedStateHandle`() = runTest {
        // Given
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // When
        viewModel.onEvent(ArticleListEvent.SelectCategory(Category.TECHNOLOGY))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(
            Category.TECHNOLOGY.displayName,
            savedStateHandle.get<String>("selected_category")
        )
    }
    
    /**
     * Test: Category is restored from SavedStateHandle.
     */
    @Test
    fun `category is restored from SavedStateHandle`() = runTest {
        // Given
        savedStateHandle["selected_category"] = Category.BUSINESS.displayName
        every { repository.getArticles(Category.BUSINESS) } returns 
            flowOf(testArticles.filter { it.category == Category.BUSINESS })
        
        // When
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip Loading
            
            val state = awaitItem()
            assertTrue(state is ArticleListState.Success)
            assertEquals(
                Category.BUSINESS,
                (state as ArticleListState.Success).selectedCategory
            )
        }
    }
    
    /**
     * Test: Toggle favorite calls repository.
     */
    @Test
    fun `toggle favorite calls repository`() = runTest {
        // Given
        coEvery { repository.toggleFavorite("1", true) } returns Unit
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // When
        viewModel.onEvent(ArticleListEvent.ToggleFavorite("1", true))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { repository.toggleFavorite("1", true) }
    }
    
    /**
     * Test: Clicking article emits navigation event.
     */
    @Test
    fun `clicking article emits navigation event`() = runTest {
        // Given
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        viewModel.navigationEvents.test {
            // When
            viewModel.onEvent(ArticleListEvent.ArticleClicked("1"))
            
            // Then
            val event = awaitItem()
            assertTrue(event is NavigationEvent.NavigateToDetail)
            assertEquals("1", (event as NavigationEvent.NavigateToDetail).articleId)
        }
    }
    
    /**
     * Test: Error state when repository throws exception.
     */
    @Test
    fun `error state when repository throws exception`() = runTest {
        // Given
        every { repository.getArticles(any()) } throws RuntimeException("Test error")
        
        // When
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip Loading
            
            val state = awaitItem()
            assertTrue(state is ArticleListState.Error)
            assertTrue(
                (state as ArticleListState.Error).message.contains("Test error")
            )
        }
    }
    
    /**
     * Test: Retry after error reloads data.
     */
    @Test
    fun `retry after error reloads data`() = runTest {
        // Given
        every { repository.getArticles(any()) } throws RuntimeException("Test error")
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        viewModel.uiState.test {
            skipItems(2) // Skip Loading and Error
            
            // Fix the repository
            every { repository.getArticles(any()) } returns flowOf(testArticles)
            
            // When
            viewModel.onEvent(ArticleListEvent.Retry)
            
            // Then
            val state = awaitItem()
            assertTrue(state is ArticleListState.Success)
        }
    }
    
    /**
     * Test: Favorite IDs are included in Success state.
     */
    @Test
    fun `favorite IDs are included in success state`() = runTest {
        // Given
        val favoriteIds = setOf("1", "2")
        every { repository.getFavoriteIds() } returns flowOf(favoriteIds)
        
        // When
        viewModel = ArticleListViewModel(repository, savedStateHandle)
        
        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip Loading
            
            val state = awaitItem()
            assertTrue(state is ArticleListState.Success)
            assertEquals(favoriteIds, (state as ArticleListState.Success).favoriteIds)
        }
    }
}
