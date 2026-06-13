package com.poem300.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poem300.data.model.Poem
import com.poem300.ui.components.PoemCard
import com.poem300.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    todayPoem: Poem?,
    isFavorite: Boolean,
    isPremium: Boolean,
    favoriteCount: Int,
    onFavoriteClick: () -> Unit,
    onPoemClick: () -> Unit,
    onRefreshPoem: () -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQuote: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val calendar = Calendar.getInstance()
    val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "300 Tang Poems",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "$month $day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Daily greeting
            Text(
                text = getDailyGreeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Today's poem
            if (todayPoem != null) {
                Text(
                    text = "Today's Poem",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PoemCard(
                    poem = todayPoem,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
                    onClick = onPoemClick,
                    showTranslation = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = onRefreshPoem) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Another")
                    }
                    FilledTonalButton(onClick = onNavigateToQuote) {
                        Text("Share Card")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick navigation
            Text(
                text = "Explore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExploreCard(
                    title = "Browse",
                    subtitle = "By theme & poet",
                    onClick = onNavigateToBrowse,
                    modifier = Modifier.weight(1f)
                )
                ExploreCard(
                    title = "Search",
                    subtitle = "Find any poem",
                    onClick = onNavigateToSearch,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExploreCard(
                    title = "Favorites",
                    subtitle = "$favoriteCount poems saved",
                    onClick = onNavigateToFavorites,
                    modifier = Modifier.weight(1f)
                )
                ExploreCard(
                    title = "Settings",
                    subtitle = if (isPremium) "Premium" else "Free",
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExploreCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getDailyGreeting(): String {
    val greetings = listOf(
        "A poem a day keeps the noise away.",
        "Let ancient words speak to modern hearts.",
        "Today, let a Tang poet be your guide.",
        "In every poem, a world awaits.",
        "Words written a thousand years ago, still breathing today.",
        "Find your moment of stillness here.",
        "The poets of the Tang Dynasty are waiting for you.",
        "One poem. One breath. One moment of peace.",
    )
    val calendar = Calendar.getInstance()
    val index = (calendar.get(Calendar.DAY_OF_YEAR) + calendar.get(Calendar.YEAR)) % greetings.size
    return greetings[index]
}
