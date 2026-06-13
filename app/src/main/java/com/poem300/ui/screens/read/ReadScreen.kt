package com.poem300.ui.screens.read

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poem300.audio.AudioManager
import com.poem300.data.model.Poem
import com.poem300.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    poem: Poem,
    isFavorite: Boolean,
    isPremium: Boolean,
    isTestMode: Boolean,
    audioPlayCount: Int,
    canPlayAudio: Boolean,
    onAudioPlayed: () -> Unit,
    userNote: String,
    onFavoriteClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onBack: () -> Unit,
    onShareQuote: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showAnnotation by remember { mutableStateOf(false) }
    var showTranslation by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showAudioLimitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Audio manager - bundled in assets
    val audioManager = remember { AudioManager(context) }
    val poemId = poem.id ?: 0
    var isPlaying by remember { mutableStateOf(false) }

    // Reset isPlaying when playback completes
    LaunchedEffect(audioManager) {
        audioManager.onPlaybackComplete = {
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.release()
        }
    }

    // Audio limit dialog
    if (showAudioLimitDialog) {
        AlertDialog(
            onDismissRequest = { showAudioLimitDialog = false },
            title = { Text("Daily Audio Limit Reached") },
            text = {
                Column {
                    Text("You've used all $audioPlayCount free audio plays today.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Upgrade to Premium for unlimited AI recitations!")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAudioLimitDialog = false
                    onUpgradeClick()
                }) {
                    Text("Upgrade — $0.99")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioLimitDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareQuote) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = if (isFavorite) Terracotta else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Read aloud (AI audio)
                    AssistChip(
                        onClick = {
                            if (!canPlayAudio) {
                                showAudioLimitDialog = true
                            } else if (isPlaying) {
                                audioManager.stop()
                                isPlaying = false
                            } else {
                                val started = audioManager.play(poemId)
                                if (started) {
                                    isPlaying = true
                                    onAudioPlayed()
                                }
                            }
                        },
                        label = {
                            Text(
                                when {
                                    isPlaying -> "Stop"
                                    !canPlayAudio -> "Limit Reached"
                                    !isPremium && audioPlayCount >= 7 -> "Read (${10 - audioPlayCount} left)"
                                    else -> "Read"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    // Translation toggle
                    AssistChip(
                        onClick = { showTranslation = !showTranslation },
                        label = { Text("Translation") },
                        leadingIcon = {
                            Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    // Annotation toggle
                    AssistChip(
                        onClick = { showAnnotation = !showAnnotation },
                        label = { Text("Notes") },
                        leadingIcon = {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    // Personal note
                    AssistChip(
                        onClick = { showNoteEditor = !showNoteEditor },
                        label = { Text("My Note") },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = poem.title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = poem.titleEn,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            // Author
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = poem.author,
                    style = MaterialTheme.typography.titleMedium,
                    color = DustyBlue
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = poem.authorEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = poem.dynastyEn + " Dynasty",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Poem content
            poem.content.split("\n").forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            // Translation
            AnimatedVisibility(visible = showTranslation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Translation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    poem.translation.split("\n").forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp,
                                lineHeight = 26.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Annotation
            AnimatedVisibility(visible = showAnnotation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "About This Poem",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = poem.annotation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Personal note
            AnimatedVisibility(visible = showNoteEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "My Note",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = userNote,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your thoughts...") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Category tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                poem.category.split(",").forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = cat.trim(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
