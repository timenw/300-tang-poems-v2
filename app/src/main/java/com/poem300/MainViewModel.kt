package com.poem300

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poem300.billing.BillingManager
import com.poem300.data.db.PoemDatabase
import com.poem300.data.model.Poem
import com.poem300.data.repository.PoemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PoemDatabase.getInstance(application)
    private val repository = PoemRepository(database.poemDao(), database.favoriteDao())
    val billingManager by lazy { BillingManager(application) }

    // Audio playback limit for free users
    companion object {
        private const val PREFS_NAME = "audio_limit_prefs"
        private const val KEY_AUDIO_COUNT = "audio_play_count"
        private const val KEY_AUDIO_DATE = "audio_play_date"
        private const val AUDIO_LIMIT = 10 // free users: 10 per day
    }

    private val audioPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _audioPlayCount = MutableStateFlow(0)
    val audioPlayCount: StateFlow<Int> = _audioPlayCount.asStateFlow()

    private val _audioLimitReached = MutableStateFlow(false)
    val audioLimitReached: StateFlow<Boolean> = _audioLimitReached.asStateFlow()

    // All poems
    val allPoems: StateFlow<List<Poem>> = repository.getAllPoems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Favorite IDs
    val favoriteIds: StateFlow<Set<Int>> = repository.getFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    // Favorite poems
    val favoritePoems: StateFlow<List<Poem>> = repository.getFavoritePoems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Favorite count
    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    // Authors
    val authors: StateFlow<List<String>> = repository.getAllAuthors()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dynasties
    val dynasties: StateFlow<List<String>> = repository.getAllDynasties()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Today's poem
    private val _todayPoem = MutableStateFlow<Poem?>(null)
    val todayPoem: StateFlow<Poem?> = _todayPoem.asStateFlow()

    // Search results
    private val _searchResults = MutableStateFlow<List<Poem>>(emptyList())
    val searchResults: StateFlow<List<Poem>> = _searchResults.asStateFlow()

    // Filtered poems (for browse)
    private val _filteredPoems = MutableStateFlow<List<Poem>>(emptyList())
    val filteredPoems: StateFlow<List<Poem>> = _filteredPoems.asStateFlow()

    // Current poem detail
    private val _currentPoem = MutableStateFlow<Poem?>(null)
    val currentPoem: StateFlow<Poem?> = _currentPoem.asStateFlow()

    // User note for current poem
    private val _currentNote = MutableStateFlow("")
    val currentNote: StateFlow<String> = _currentNote.asStateFlow()

    // Premium status
    val isPremium: StateFlow<Boolean> by lazy { billingManager.isPremium }

    // Show premium prompt when free user hits favorite limit
    private val _showPremiumPrompt = MutableStateFlow(false)
    val showPremiumPrompt: StateFlow<Boolean> = _showPremiumPrompt.asStateFlow()

    init {
        loadTodayPoem()
        loadFavoriteCount()
        loadAudioCount()
        // Delay billing init to avoid crash on devices without Google Play
        viewModelScope.launch {
            try {
                billingManager.startConnection()
            } catch (e: Exception) {
                // Ignore billing errors on devices without Google Play
            }
        }
    }

    private fun loadAudioCount() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = audioPrefs.getString(KEY_AUDIO_DATE, "")
        if (savedDate != today) {
            // New day, reset count
            audioPrefs.edit()
                .putString(KEY_AUDIO_DATE, today)
                .putInt(KEY_AUDIO_COUNT, 0)
                .apply()
            _audioPlayCount.value = 0
        } else {
            _audioPlayCount.value = audioPrefs.getInt(KEY_AUDIO_COUNT, 0)
        }
        _audioLimitReached.value = _audioPlayCount.value >= AUDIO_LIMIT
    }

    fun onAudioPlayed() {
        val newCount = _audioPlayCount.value + 1
        _audioPlayCount.value = newCount
        audioPrefs.edit().putInt(KEY_AUDIO_COUNT, newCount).apply()
        _audioLimitReached.value = newCount >= AUDIO_LIMIT
    }

    fun canPlayAudio(): Boolean {
        return isPremium.value || _audioPlayCount.value < AUDIO_LIMIT
    }

    private fun loadFavoriteCount() {
        viewModelScope.launch {
            _favoriteCount.value = repository.getFavoriteCount()
        }
    }

    fun loadTodayPoem() {
        viewModelScope.launch {
            _todayPoem.value = repository.getDailyPoem()
        }
    }

    fun refreshDailyPoem() {
        viewModelScope.launch {
            val poem = database.poemDao().getRandomPoem()
            _todayPoem.value = poem
        }
    }

    fun searchPoems(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.searchPoems(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            repository.getPoemsByCategory(category).collect { results ->
                _filteredPoems.value = results
            }
        }
    }

    fun filterByAuthor(author: String) {
        viewModelScope.launch {
            repository.getPoemsByAuthor(author).collect { results ->
                _filteredPoems.value = results
            }
        }
    }

    fun filterByDynasty(dynasty: String) {
        viewModelScope.launch {
            repository.getAllPoems().collect { poems ->
                _filteredPoems.value = poems.filter { it.dynastyEn == dynasty }
            }
        }
    }

    fun filterByDifficulty(level: Int) {
        viewModelScope.launch {
            repository.getPoemsByDifficulty(level).collect { results ->
                _filteredPoems.value = results
            }
        }
    }

    fun openPoem(poemId: Int) {
        viewModelScope.launch {
            _currentPoem.value = repository.getPoemById(poemId)
            // Load note if it's a favorite
            favoriteIds.value.let { ids ->
                if (ids.contains(poemId)) {
                    // Note will be loaded from favorite
                }
            }
        }
    }

    fun toggleFavorite(poemId: Int) {
        viewModelScope.launch {
            val ids = favoriteIds.value
            if (ids.contains(poemId)) {
                repository.removeFavorite(poemId)
            } else {
                // Check limit for free users
                if (!isPremium.value && _favoriteCount.value >= 20) {
                    _showPremiumPrompt.value = true
                    return@launch
                }
                repository.addFavorite(poemId)
            }
            loadFavoriteCount()
        }
    }

    fun toggleTodayFavorite() {
        _todayPoem.value?.let { toggleFavorite(it.id!!) }
    }

    fun updateNote(poemId: Int, note: String) {
        _currentNote.value = note
        viewModelScope.launch {
            repository.updateNote(poemId, note)
        }
    }

    fun clearPremiumPrompt() {
        _showPremiumPrompt.value = false
    }

    fun isFavorite(poemId: Int): Boolean {
        return favoriteIds.value.contains(poemId)
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }
}
