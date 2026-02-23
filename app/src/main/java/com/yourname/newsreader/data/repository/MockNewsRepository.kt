package com.yourname.newsreader.data.repository

import com.yourname.newsreader.data.model.Article
import com.yourname.newsreader.data.model.Category
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Mock implementation of NewsRepository for Chapter 5.
 * 
 * This provides realistic behavior:
 * - Simulated network delay
 * - In-memory data storage
 * - Flow-based reactive updates
 * 
 * In Chapter 6, we'll replace this with a real implementation.
 */
class MockNewsRepository : NewsRepository {
    
    // In-memory storage using StateFlow for reactive updates
    private val _articles = MutableStateFlow(generateMockArticles())
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    
    override fun getArticles(category: Category?): Flow<List<Article>> {
        return _articles.asStateFlow().map { articles ->
            if (category != null) {
                articles.filter { it.category == category }
            } else {
                articles
            }
        }
    }
    
    override suspend fun getArticleById(articleId: String): Article? {
        // Simulate network delay
        delay(300)
        return _articles.value.find { it.id == articleId }
    }
    
    override suspend fun refreshArticles() {
        // Simulate network delay
        delay(1000)
        
        // In a real app, this would fetch from network
        // For now, just regenerate mock data
        _articles.value = generateMockArticles()
    }
    
    override suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        val currentFavorites = _favoriteIds.value.toMutableSet()
        if (isFavorite) {
            currentFavorites.add(articleId)
        } else {
            currentFavorites.remove(articleId)
        }
        _favoriteIds.value = currentFavorites
    }
    
    override fun getFavoriteIds(): Flow<Set<String>> {
        return _favoriteIds.asStateFlow()
    }
    
    /**
     * Generate mock articles for testing.
     */
    private fun generateMockArticles(): List<Article> {
        val baseTime = System.currentTimeMillis()
        
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
                    a dramatic improvement over previous records. This achievement brings us 
                    closer to the goal of fault-tolerant quantum computation.
                    
                    "This is a watershed moment for quantum computing," said Dr. Sarah Chen, 
                    lead researcher on the project. "We're now seeing a clear path to 
                    practical applications in cryptography, drug discovery, and optimization."
                """.trimIndent(),
                author = "Dr. Sarah Chen",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(2)),
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
                    
                    The S&P 500 rose 2.3%, while European markets saw similar gains. Asian 
                    markets had already closed higher in anticipation of the data release.
                    
                    Analysts attribute the rally to growing confidence in economic recovery 
                    and expectations of stable monetary policy. "The employment data suggests 
                    a robust recovery is underway," noted chief economist Michael Torres.
                """.trimIndent(),
                author = "Michael Torres",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(4)),
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
                    exoplanet orbiting a nearby star. The planet, designated Kepler-442b, lies 
                    within its star's habitable zone where liquid water could exist.
                    
                    Using advanced spectroscopic analysis, researchers detected atmospheric 
                    signatures consistent with water vapor and possible biosignatures.
                    
                    "This is one of the most promising candidates for hosting extraterrestrial 
                    life we've ever found," explained Professor James Wilson. "The next step 
                    is detailed atmospheric analysis with next-generation telescopes."
                """.trimIndent(),
                author = "Professor James Wilson",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(6)),
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
                    
                    The treatment, which harnesses the immune system to target cancer cells, 
                    achieved an 80% response rate across multiple cancer types. Researchers 
                    are calling it a paradigm shift in oncology.
                    
                    "We're witnessing a transformation in how we treat cancer," said 
                    Dr. Emily Rodriguez, principal investigator. "This personalized approach 
                    could revolutionize patient outcomes."
                """.trimIndent(),
                author = "Dr. Emily Rodriguez",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(8)),
                imageUrl = "https://picsum.photos/seed/medical/800/400",
                category = Category.HEALTH,
                source = "Medical News Today",
                url = "https://example.com/cancer-treatment"
            ),
            Article(
                id = "5",
                title = "Championship Finals Set Record Viewership",
                description = "Historic match draws 50 million viewers worldwide",
                content = """
                    Last night's championship finals shattered viewership records, with an 
                    estimated 50 million people tuning in worldwide to watch the dramatic 
                    conclusion to the season.
                    
                    The match went into overtime, with the home team securing victory in the 
                    final seconds. Social media exploded with reactions, making it the most-
                    discussed sporting event of the year.
                    
                    "The atmosphere was electric," said team captain Alex Martinez. "This is 
                    what we've worked for all season. The fans were incredible."
                """.trimIndent(),
                author = "Alex Martinez",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(10)),
                imageUrl = "https://picsum.photos/seed/sports/800/400",
                category = Category.SPORTS,
                source = "Sports Network",
                url = "https://example.com/championship-finals"
            ),
            Article(
                id = "6",
                title = "Blockbuster Film Breaks Opening Weekend Records",
                description = "Sci-fi epic earns $350 million in first three days",
                content = """
                    The highly anticipated sci-fi epic "Stellar Odyssey" has shattered box 
                    office records with a stunning $350 million opening weekend globally.
                    
                    The film, directed by acclaimed filmmaker Rachel Zhang, combines 
                    groundbreaking visual effects with a compelling story that has resonated 
                    with audiences worldwide.
                    
                    "We poured our hearts into this project," Zhang said. "Seeing audiences 
                    connect with the story has been incredibly rewarding. This is just the 
                    beginning of an epic franchise."
                """.trimIndent(),
                author = "Rachel Zhang",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(12)),
                imageUrl = "https://picsum.photos/seed/movie/800/400",
                category = Category.ENTERTAINMENT,
                source = "Entertainment Weekly",
                url = "https://example.com/blockbuster-film"
            ),
            Article(
                id = "7",
                title = "Climate Summit Reaches Historic Agreement",
                description = "195 nations commit to accelerated carbon neutrality",
                content = """
                    World leaders have reached a landmark agreement at the Global Climate 
                    Summit, committing to accelerated timelines for achieving carbon 
                    neutrality.
                    
                    The agreement includes binding commitments for emissions reductions, 
                    renewable energy investment, and support for developing nations in their 
                    transition to clean energy.
                    
                    "This represents a turning point in our collective response to climate 
                    change," announced UN Secretary-General Maria Santos. "Now comes the hard 
                    work of implementation."
                """.trimIndent(),
                author = "Maria Santos",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(14)),
                imageUrl = "https://picsum.photos/seed/climate/800/400",
                category = Category.GENERAL,
                source = "World News Network",
                url = "https://example.com/climate-summit"
            ),
            Article(
                id = "8",
                title = "AI Breakthrough in Natural Language Understanding",
                description = "New model achieves human-level comprehension in complex tasks",
                content = """
                    Researchers have unveiled a new artificial intelligence model that 
                    achieves human-level performance in complex language understanding tasks.
                    
                    The model, trained on diverse datasets, demonstrates unprecedented ability 
                    to understand context, nuance, and implicit meaning in text. It could 
                    revolutionize applications from customer service to content creation.
                    
                    "This represents a significant leap forward," explained lead AI researcher 
                    Dr. Kevin Park. "We're approaching truly intelligent systems that can 
                    understand and reason about language like humans do."
                """.trimIndent(),
                author = "Dr. Kevin Park",
                publishedAt = Date(baseTime - TimeUnit.HOURS.toMillis(16)),
                imageUrl = "https://picsum.photos/seed/ai/800/400",
                category = Category.TECHNOLOGY,
                source = "AI Research Today",
                url = "https://example.com/ai-breakthrough"
            )
        )
    }
}
