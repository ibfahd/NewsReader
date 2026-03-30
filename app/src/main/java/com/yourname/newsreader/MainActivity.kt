package com.yourname.newsreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yourname.newsreader.ui.navigation.NewsNavGraph
import com.yourname.newsreader.ui.theme.NewsReaderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's single Activity.
 *
 * ─── @AndroidEntryPoint ───────────────────────────────────────────────────────
 * This annotation marks the Activity as a Hilt injection site. Hilt generates
 * a superclass (Hilt_MainActivity) that performs field injection before onCreate.
 *
 * Every Android class that wants Hilt injection needs a specific annotation:
 *   Application  → @HiltAndroidApp (NewsReaderApplication)
 *   Activity     → @AndroidEntryPoint (this file)
 *   Fragment     → @AndroidEntryPoint
 *   ViewModel    → @HiltViewModel
 *   Service      → @AndroidEntryPoint
 *
 * This Activity itself doesn't @Inject anything directly — ViewModels are
 * injected further down the tree by hiltViewModel() inside composables.
 * But the annotation is still required so that Hilt's component hierarchy
 * is correctly established for child composables.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NewsNavGraph(navController = navController)
                }
            }
        }
    }
}