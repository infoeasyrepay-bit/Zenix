package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.data.ZenixCatalog
import com.example.data.ZenixRepository
import com.example.domain.AiRecommendationService
import com.example.domain.LocalRecommendationEngine
import com.example.domain.model.MaturityLevel
import com.example.domain.model.MediaContent
import com.example.domain.model.RatingSentiment
import com.example.domain.model.RecommendedPick
import com.example.domain.model.TitleRating
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.WatchlistItem
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ZenixViewModel(
  private val repository: ZenixRepository,
  private val aiService: AiRecommendationService,
  private val currentUserId: String,
) : ViewModel() {

  private val _activeProfileId = MutableStateFlow<String?>(null)
  val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

  private val _actionBanner = MutableStateFlow<String?>(null)
  val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedGenre = MutableStateFlow("All")
  val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

  private val _aiMoodPrompt = MutableStateFlow("")
  val aiMoodPrompt: StateFlow<String> = _aiMoodPrompt.asStateFlow()

  private val _aiRecommendations = MutableStateFlow<UiState<List<RecommendedPick>>?>(null)
  val aiRecommendations: StateFlow<UiState<List<RecommendedPick>>?> = _aiRecommendations.asStateFlow()

  private var autoCreatedStarterProfile = false

  val profilesState: StateFlow<UiState<List<ViewerProfile>>> =
    repository.observeProfiles(currentUserId)
      .map<List<ViewerProfile>, UiState<List<ViewerProfile>>> { UiState.Success(it) }
      .catch { error -> emit(handleStreamError("profiles", error)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val activeProfile: StateFlow<ViewerProfile?> =
    combine(profilesState, _activeProfileId) { state, selectedId ->
      val profiles = (state as? UiState.Success)?.data.orEmpty()
      profiles.firstOrNull { it.id == selectedId }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val watchlistState: StateFlow<UiState<List<WatchlistItem>>> =
    _activeProfileId.flatMapLatest { profileId ->
      if (profileId.isNullOrBlank()) {
        flowOf(UiState.Success(emptyList()))
      } else {
        repository.observeWatchlist(currentUserId, profileId)
          .map<List<WatchlistItem>, UiState<List<WatchlistItem>>> { UiState.Success(it) }
          .catch { error -> emit(handleStreamError("watchlist", error)) }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val watchHistoryState: StateFlow<UiState<List<WatchHistoryEntry>>> =
    _activeProfileId.flatMapLatest { profileId ->
      if (profileId.isNullOrBlank()) {
        flowOf(UiState.Success(emptyList()))
      } else {
        repository.observeWatchHistory(currentUserId, profileId)
          .map<List<WatchHistoryEntry>, UiState<List<WatchHistoryEntry>>> { UiState.Success(it) }
          .catch { error -> emit(handleStreamError("watchHistory", error)) }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val ratingsState: StateFlow<UiState<List<TitleRating>>> =
    _activeProfileId.flatMapLatest { profileId ->
      if (profileId.isNullOrBlank()) {
        flowOf(UiState.Success(emptyList()))
      } else {
        repository.observeRatings(currentUserId, profileId)
          .map<List<TitleRating>, UiState<List<TitleRating>>> { UiState.Success(it) }
          .catch { error -> emit(handleStreamError("ratings", error)) }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val localRecommendations: StateFlow<List<RecommendedPick>> =
    combine(
      activeProfile,
      watchHistoryState,
      watchlistState,
      ratingsState,
    ) { profile, historyUi, watchlistUi, ratingsUi ->
      val history = (historyUi as? UiState.Success)?.data.orEmpty()
      val watchlist = (watchlistUi as? UiState.Success)?.data.orEmpty()
      val ratings = (ratingsUi as? UiState.Success)?.data.orEmpty()
      LocalRecommendationEngine.computeRecommendations(profile, history, watchlist, ratings)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = LocalRecommendationEngine.computeRecommendations(null, emptyList(), emptyList(), emptyList()),
    )

  fun syncUserAccount(user: FirebaseUser) {
    viewModelScope.launch {
      repository.ensureUserAccount(user)
    }
  }

  fun ensureStarterProfilesIfEmpty(user: FirebaseUser, currentProfiles: List<ViewerProfile>) {
    if (currentProfiles.isNotEmpty() || autoCreatedStarterProfile) return
    autoCreatedStarterProfile = true
    viewModelScope.launch {
      val firstName = user.displayName?.split(" ")?.firstOrNull()?.take(18)?.ifBlank { "Main" } ?: "Main"
      repository.createProfile(
        userId = currentUserId,
        name = firstName,
        avatarId = "crimson_fox",
        maturityLevel = MaturityLevel.ALL,
        preferredGenres = listOf("Sci-Fi", "Cyberpunk", "Thriller"),
        autoplayNext = true,
      )
      repository.createProfile(
        userId = currentUserId,
        name = "Kids",
        avatarId = "kids_sprout",
        maturityLevel = MaturityLevel.KIDS,
        preferredGenres = listOf("Animation", "Documentary"),
        autoplayNext = true,
      )
    }
  }

  fun selectProfile(profileId: String) {
    _activeProfileId.value = profileId
    _aiRecommendations.value = null
  }

  fun clearActiveProfile() {
    _activeProfileId.value = null
  }

  fun createProfile(
    name: String,
    avatarId: String,
    maturityLevel: MaturityLevel,
    preferredGenres: List<String>,
    autoplayNext: Boolean,
    selectOnSuccess: Boolean = false,
  ) {
    viewModelScope.launch {
      val res = repository.createProfile(
        userId = currentUserId,
        name = name,
        avatarId = avatarId,
        maturityLevel = maturityLevel,
        preferredGenres = preferredGenres,
        autoplayNext = autoplayNext,
      )
      res.onSuccess { newId ->
        if (selectOnSuccess) _activeProfileId.value = newId
        _actionBanner.value = "Profile \"$name\" created."
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Failed to create profile."
      }
    }
  }

  fun updateProfile(
    profileId: String,
    name: String,
    avatarId: String,
    maturityLevel: MaturityLevel,
    preferredGenres: List<String>,
    autoplayNext: Boolean,
  ) {
    viewModelScope.launch {
      val res = repository.updateProfile(
        userId = currentUserId,
        profileId = profileId,
        name = name,
        avatarId = avatarId,
        maturityLevel = maturityLevel,
        preferredGenres = preferredGenres,
        autoplayNext = autoplayNext,
      )
      res.onSuccess {
        _actionBanner.value = "Profile updated."
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Failed to update profile."
      }
    }
  }

  fun deleteProfile(profileId: String) {
    viewModelScope.launch {
      val res = repository.deleteProfile(currentUserId, profileId)
      res.onSuccess {
        if (_activeProfileId.value == profileId) {
          _activeProfileId.value = null
        }
        _actionBanner.value = "Profile removed."
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Failed to delete profile."
      }
    }
  }

  fun toggleWatchlist(content: MediaContent) {
    val profileId = _activeProfileId.value ?: return
    val currentItems = (watchlistState.value as? UiState.Success)?.data.orEmpty()
    val inList = currentItems.any { it.contentId == content.id }
    viewModelScope.launch {
      val res = repository.toggleWatchlist(currentUserId, profileId, content, inList)
      res.onSuccess { added ->
        _actionBanner.value = if (added) "Added ${content.title} to My List" else "Removed ${content.title} from My List"
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Could not update My List."
      }
    }
  }

  fun setRating(contentId: String, sentiment: RatingSentiment) {
    val profileId = _activeProfileId.value ?: return
    val currentRatings = (ratingsState.value as? UiState.Success)?.data.orEmpty()
    val existing = currentRatings.firstOrNull { it.contentId == contentId }
    val nextSentiment = if (existing?.sentiment == sentiment.name) null else sentiment
    viewModelScope.launch {
      val res = repository.setRating(currentUserId, profileId, contentId, nextSentiment)
      res.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Could not save rating."
      }
    }
  }

  fun savePlaybackProgress(
    contentId: String,
    episodeId: String,
    positionMs: Long,
    durationMs: Long,
  ) {
    val profileId = _activeProfileId.value ?: return
    val content = ZenixCatalog.findById(contentId) ?: return
    if (durationMs <= 0L) return
    viewModelScope.launch {
      repository.savePlaybackProgress(
        userId = currentUserId,
        profileId = profileId,
        content = content,
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
      )
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectGenre(genre: String) {
    _selectedGenre.value = genre
  }

  fun updateAiMoodPrompt(prompt: String) {
    _aiMoodPrompt.value = prompt
  }

  fun generateAiRecommendations(customMood: String? = null) {
    val profile = activeProfile.value ?: return
    val mood = customMood ?: _aiMoodPrompt.value
    if (customMood != null) {
      _aiMoodPrompt.value = customMood
    }
    val history = (watchHistoryState.value as? UiState.Success)?.data.orEmpty()
    val watchlist = (watchlistState.value as? UiState.Success)?.data.orEmpty()
    val ratings = (ratingsState.value as? UiState.Success)?.data.orEmpty()

    viewModelScope.launch {
      _aiRecommendations.value = UiState.Loading
      try {
        val picks = aiService.generatePersonalizedPicks(
          profile = profile,
          history = history,
          watchlist = watchlist,
          ratings = ratings,
          moodPrompt = mood,
        )
        _aiRecommendations.value = UiState.Success(picks)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiRecommendations.value = UiState.Error(e.localizedMessage ?: "Zenix AI recommendation engine failed.")
      }
    }
  }

  fun clearBanner() {
    _actionBanner.value = null
  }

  private fun handleStreamError(streamName: String, error: Throwable): UiState.Error {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error observing $streamName", rootError)
    return UiState.Error(rootError.message ?: "Failed to load $streamName")
  }

  private companion object {
    const val TAG = "ZenixViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
