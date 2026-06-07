package com.poem300

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.poem300.billing.BillingManager
import com.poem300.ui.screens.browse.BrowseScreen
import com.poem300.ui.screens.home.HomeScreen
import com.poem300.ui.screens.read.ReadScreen
import com.poem300.ui.screens.search.SearchScreen
import com.poem300.ui.screens.settings.SettingsScreen
import com.poem300.ui.screens.settings.PrivacyPolicyScreen
import com.poem300.ui.screens.quote.QuoteScreen
import com.poem300.ui.theme.Poem300Theme

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() requires API 30+; use WindowCompat for broader compatibility
        WindowCompat.setDecorFitsSystemWindows(window, false)

        billingManager = BillingManager(application)
        billingManager.startConnection()

        setContent {
            Poem300Theme {
                Poem300App(billingManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}

@Composable
fun Poem300App(billingManager: BillingManager) {
    val vm: MainViewModel = viewModel()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Home
        composable("home") {
            val todayPoem by vm.todayPoem.collectAsState()
            val favIds by vm.favoriteIds.collectAsState()
            val isPremium by vm.isPremium.collectAsState()
            val favoriteCount by vm.favoriteCount.collectAsState()
            val isFavorite = todayPoem?.let { favIds.contains(it.id) } ?: false

            HomeScreen(
                todayPoem = todayPoem,
                isFavorite = isFavorite,
                isPremium = isPremium,
                favoriteCount = favoriteCount,
                onFavoriteClick = { vm.toggleTodayFavorite() },
                onPoemClick = { todayPoem?.let { navController.navigate("read/${it.id}") } },
                onRefreshPoem = { vm.refreshDailyPoem() },
                onNavigateToBrowse = { navController.navigate("browse") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToQuote = { navController.navigate("quote") },
            )
        }

        // Read poem
        composable(
            "read/{poemId}",
            arguments = listOf(navArgument("poemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val poemId = backStackEntry.arguments?.getInt("poemId") ?: return@composable
            val poem by vm.currentPoem.collectAsState()
            val favIds by vm.favoriteIds.collectAsState()
            val isPremium by vm.isPremium.collectAsState()
            val note by vm.currentNote.collectAsState()

            LaunchedEffect(poemId) {
                vm.openPoem(poemId)
            }

            poem?.let { p ->
                ReadScreen(
                    poem = p,
                    isFavorite = favIds.contains(poemId),
                    isPremium = isPremium,
                    userNote = note,
                    onFavoriteClick = { vm.toggleFavorite(poemId) },
                    onNoteChange = { vm.updateNote(poemId, it) },
                    onBack = { navController.popBackStack() },
                    onShareQuote = { navController.navigate("quote/$poemId") },
                )
            }
        }

        // Search
        composable("search") {
            val results by vm.searchResults.collectAsState()
            val favIds by vm.favoriteIds.collectAsState()

            SearchScreen(
                searchResults = results,
                favoriteIds = favIds,
                onSearch = { vm.searchPoems(it) },
                onPoemClick = { navController.navigate("read/$it") },
                onFavoriteClick = { vm.toggleFavorite(it) },
                onBack = { navController.popBackStack() },
            )
        }

        // Browse
        composable("browse") {
            val poems by vm.allPoems.collectAsState()
            val authors by vm.authors.collectAsState()
            val dynasties by vm.dynasties.collectAsState()
            val favIds by vm.favoriteIds.collectAsState()
            val filtered by vm.filteredPoems.collectAsState()

            BrowseScreen(
                poems = if (filtered.isEmpty()) poems else filtered,
                authors = authors,
                dynasties = dynasties,
                favoriteIds = favIds,
                onPoemClick = { navController.navigate("read/$it") },
                onFavoriteClick = { vm.toggleFavorite(it) },
                onFilterByCategory = { vm.filterByCategory(it) },
                onFilterByAuthor = { vm.filterByAuthor(it) },
                onFilterByDynasty = { vm.filterByDynasty(it) },
                onFilterByDifficulty = { vm.filterByDifficulty(it) },
                onBack = { navController.popBackStack() },
            )
        }

        // Favorites
        composable("favorites") {
            val favPoems by vm.favoritePoems.collectAsState()
            val favIds by vm.favoriteIds.collectAsState()

            SearchScreen(
                searchResults = favPoems,
                favoriteIds = favIds,
                onSearch = { },
                onPoemClick = { navController.navigate("read/$it") },
                onFavoriteClick = { vm.toggleFavorite(it) },
                onBack = { navController.popBackStack() },
            )
        }

        // Settings
        composable("settings") {
            val isPremium by vm.isPremium.collectAsState()
            val favoriteCount by vm.favoriteCount.collectAsState()
            val activity = LocalContext.current as ComponentActivity

            SettingsScreen(
                isPremium = isPremium,
                favoriteCount = favoriteCount,
                onPurchaseClick = { billingManager.launchPurchaseFlow(activity) },
                onRestoreClick = { billingManager.startConnection() },
                onPrivacyClick = { navController.navigate("privacy") },
                onBack = { navController.popBackStack() },
            )
        }

        // Privacy Policy
        composable("privacy") {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Quote card
        composable(
            "quote/{poemId}",
            arguments = listOf(navArgument("poemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val poemId = backStackEntry.arguments?.getInt("poemId") ?: return@composable
            val poem by vm.currentPoem.collectAsState()

            LaunchedEffect(poemId) {
                vm.openPoem(poemId)
            }

            poem?.let { p ->
                QuoteScreen(
                    poem = p,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
