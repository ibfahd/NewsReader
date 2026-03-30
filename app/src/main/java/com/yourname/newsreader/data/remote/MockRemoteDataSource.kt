package com.yourname.newsreader.data.remote

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Mock implementation of [RemoteDataSource] — simulates a real API for Chapter 6.
 *
 * This class replaces the data that previously lived in MockNewsRepository.
 * The architecture is now correct: mock *network* data lives in the remote layer.
 *
 * In Chapter 7, this class is replaced by RetrofitRemoteDataSource.
 * Because RepositoryModule uses @Binds, that swap is a single-line change.
 *
 * @Inject constructor — Hilt can construct this directly without a @Provides
 * function, because all constructor parameters are already known to Hilt.
 * (In this case, there are none — so it's trivially constructable.)
 */
class MockRemoteDataSource @Inject constructor() : RemoteDataSource {

    override suspend fun fetchArticles(): List<Article> {
        delay(500) // Simulate realistic network latency
        return generateMockArticles()
    }

    private fun generateMockArticles(): List<Article> {
        val now = System.currentTimeMillis()
        return listOf(
            Article(
                id = "1",
                title = "Breakthrough in Quantum Computing Achieved",
                description = "Scientists announce major advancement in quantum error correction",
                content = """
                    Researchers at a leading technology institute have announced a significant
                    breakthrough in quantum computing. The new error correction technique could
                    pave the way for practical quantum computers capable of solving complex
                    problems beyond the reach of classical computers.
                    The team demonstrated sustained quantum coherence for over 10 minutes,
                    a dramatic improvement over previous records.
                """.trimIndent(),
                author = "Dr. Sarah Chen",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(2)),
                imageUrl = "https://picsum.photos/seed/quantum/800/400",
                category = Category.TECHNOLOGY,
                source = "Tech Science Daily",
                url = "https://example.com/quantum-breakthrough"
            ),
            Article(
                id = "2",
                title = "Global Markets Rally on Economic Data",
                description = "Stock markets surge following positive employment reports",
                content = """
                    Global stock markets experienced significant gains today following the
                    release of stronger-than-expected employment data from major economies.
                    The S&P 500 rose 2.3%, while European markets saw similar gains.
                """.trimIndent(),
                author = "Michael Torres",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(4)),
                imageUrl = "https://picsum.photos/seed/markets/800/400",
                category = Category.BUSINESS,
                source = "Financial Times",
                url = "https://example.com/markets-rally"
            ),
            Article(
                id = "3",
                title = "New Exoplanet Discovery Could Harbor Life",
                description = "Astronomers find potentially habitable planet 40 light-years away",
                content = """
                    An international team of astronomers has discovered a potentially habitable
                    exoplanet within its star's habitable zone where liquid water could exist.
                """.trimIndent(),
                author = "Prof. James Wilson",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(6)),
                imageUrl = "https://picsum.photos/seed/exoplanet/800/400",
                category = Category.SCIENCE,
                source = "Space Journal",
                url = "https://example.com/exoplanet-discovery"
            ),
            Article(
                id = "4",
                title = "Revolutionary Cancer Treatment Shows Promise",
                description = "Clinical trials reveal 80% success rate for new immunotherapy",
                content = """
                    A groundbreaking cancer treatment has shown remarkable results in Phase 3
                    clinical trials, offering new hope to patients with previously untreatable
                    forms of the disease.
                """.trimIndent(),
                author = "Dr. Emily Rodriguez",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(8)),
                imageUrl = "https://picsum.photos/seed/medical/800/400",
                category = Category.HEALTH,
                source = "Medical News Today",
                url = "https://example.com/cancer-treatment"
            ),
            Article(
                id = "5",
                title = "Championship Finals Set Record Viewership",
                description = "Historic match draws 50 million viewers worldwide",
                content = "Last night's championship finals shattered viewership records.",
                author = "Alex Martinez",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(10)),
                imageUrl = "https://picsum.photos/seed/sports/800/400",
                category = Category.SPORTS,
                source = "Sports Network",
                url = "https://example.com/championship-finals"
            ),
            Article(
                id = "6",
                title = "Blockbuster Film Breaks Opening Weekend Records",
                description = "Sci-fi epic earns \$350 million in first three days",
                content = "\"Stellar Odyssey\" shattered box office records globally.",
                author = "Rachel Zhang",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(12)),
                imageUrl = "https://picsum.photos/seed/movie/800/400",
                category = Category.ENTERTAINMENT,
                source = "Entertainment Weekly",
                url = "https://example.com/blockbuster-film"
            ),
            Article(
                id = "7",
                title = "Climate Summit Reaches Historic Agreement",
                description = "195 nations commit to accelerated carbon neutrality",
                content = "World leaders reached a landmark agreement on accelerated clean energy.",
                author = "Maria Santos",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(14)),
                imageUrl = "https://picsum.photos/seed/climate/800/400",
                category = Category.GENERAL,
                source = "World News Network",
                url = "https://example.com/climate-summit"
            ),
            Article(
                id = "8",
                title = "AI Breakthrough in Natural Language Understanding",
                description = "New model achieves human-level comprehension in complex tasks",
                content = "A new AI model achieves human-level performance in language understanding.",
                author = "Dr. Kevin Park",
                publishedAt = Date(now - TimeUnit.HOURS.toMillis(16)),
                imageUrl = "https://picsum.photos/seed/ai/800/400",
                category = Category.TECHNOLOGY,
                source = "AI Research Today",
                url = "https://example.com/ai-breakthrough"
            )
        )
    }
}