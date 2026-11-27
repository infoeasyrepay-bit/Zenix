package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.UiState
import com.example.core.configureAppCheck
import com.example.data.ZenixRepository
import com.example.data.signOutZenix
import com.example.domain.AiRecommendationService
import com.example.domain.model.RatingSentiment
import com.example.domain.model.RecommendedPick
import com.example.ui.navigation.ZenixRoute
import com.example.ui.screens.AccountProfilesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.ForYouAiScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileSelectorScreen
import com.example.ui.screens.TitleDetailBottomSheet
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixSurfaceDark
import com.example.ui.theme.ZenixTextMuted
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTheme
import com.example.ui.viewmodel.ZenixViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandatory App Check initialization with launch Intent BEFORE setContent or ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      ZenixTheme {
        ZenixRootApp()
      }
    }
  }
}

@Composable
fun ZenixRootApp() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      },
    )
  } else {
    key(user.uid) {
      ZenixAuthenticatedShell(user = user)
    }
  }
}

@Composable
private fun ZenixAuthenticatedShell(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: ZenixViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        ZenixViewModel(
          repository = ZenixRepository(db),
          aiService = AiRecommendationService(),
          currentUserId = user.uid,
        )
      }
    },
  )

  LaunchedEffect(user.uid) {
    viewModel.syncUserAccount(user)
  }

  val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
  val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
  val watchlistState by viewModel.watchlistState.collectAsStateWithLifecycle()
  val watchHistoryState by viewModel.watchHistoryState.collectAsStateWithLifecycle()
  val ratingsState by viewModel.ratingsState.collectAsStateWithLifecycle()
  val localRecommendations by viewModel.localRecommendations.collectAsStateWithLifecycle()
  val aiRecommendations by viewModel.aiRecommendations.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
  val aiMoodPrompt by viewModel.aiMoodPrompt.collectAsStateWithLifecycle()
  val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()

  LaunchedEffect(profilesState) {
    if (profilesState is UiState.Success) {
      val list = (profilesState as UiState.Success).data
      viewModel.ensureStarterProfilesIfEmpty(user, list)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(actionBanner) {
    val msg = actionBanner
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  val onSignOut = {
    signOutZenix(
      credentialManager = credentialManager,
      scope = scope,
    )
  }

  val profile = activeProfile
  if (profile == null) {
    ProfileSelectorScreen(
      profilesState = profilesState,
      onSelectProfile = { viewModel.selectProfile(it.id) },
      onCreateProfile = { name, avatarId, maturity, genres, autoplay ->
        viewModel.createProfile(name, avatarId, maturity, genres, autoplay, selectOnSuccess = true)
      },
      onUpdateProfile = { id, name, avatarId, maturity, genres, autoplay ->
        viewModel.updateProfile(id, name, avatarId, maturity, genres, autoplay)
      },
      onDeleteProfile = { viewModel.deleteProfile(it) },
      onSignOut = onSignOut,
    )
    return
  }

  val watchlist = (watchlistState as? UiState.Success)?.data.orEmpty()
  val watchHistory = (watchHistoryState as? UiState.Success)?.data.orEmpty()
  val ratings = (ratingsState as? UiState.Success)?.data.orEmpty()
  val profiles = (profilesState as? UiState.Success)?.data.orEmpty()

  var detailPick by remember { mutableStateOf<RecommendedPick?>(null) }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = navBackStackEntry?.destination
  val isPlayerRoute = currentDest?.hasRoute<ZenixRoute.Player>() == true

  val navigateToPlayer = { contentId: String, episodeId: String ->
    detailPick = null
    navController.navigate(ZenixRoute.Player(contentId = contentId, episodeId = episodeId))
  }

  val navItems = listOf(
    Triple(ZenixRoute.Home, stringResource(R.string.nav_home), Pair(Icons.Filled.Home, Icons.Outlined.Home)),
    Triple(ZenixRoute.ForYouAi, stringResource(R.string.nav_for_you), Pair(Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)),
    Triple(ZenixRoute.Explore, stringResource(R.string.nav_explore), Pair(Icons.Filled.Explore, Icons.Outlined.Explore)),
    Triple(ZenixRoute.AccountProfiles, stringResource(R.string.nav_profiles), Pair(Icons.Filled.Person, Icons.Outlined.Person)),
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp && !isPlayerRoute

    Scaffold(
      containerColor = ZenixObsidian,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (!isPlayerRoute && !useRail) {
          NavigationBar(
            containerColor = ZenixSurfaceDark.copy(alpha = 0.96f),
            windowInsets = WindowInsets.navigationBars,
          ) {
            navItems.forEach { (route, label, icons) ->
              val selected = when (route) {
                is ZenixRoute.Home -> currentDest?.hasRoute<ZenixRoute.Home>() == true
                is ZenixRoute.ForYouAi -> currentDest?.hasRoute<ZenixRoute.ForYouAi>() == true
                is ZenixRoute.Explore -> currentDest?.hasRoute<ZenixRoute.Explore>() == true
                is ZenixRoute.AccountProfiles -> currentDest?.hasRoute<ZenixRoute.AccountProfiles>() == true
                else -> false
              }
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(route) {
                    popUpTo(ZenixRoute.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = Color.White,
                  selectedTextColor = ZenixTextPrimary,
                  indicatorColor = ZenixCrimson.copy(alpha = 0.85f),
                  unselectedIconColor = ZenixTextMuted,
                  unselectedTextColor = ZenixTextMuted,
                ),
                modifier = Modifier.testTag("nav_${label.lowercase().replace(" ", "_")}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(
            containerColor = ZenixSurfaceDark,
          ) {
            navItems.forEach { (route, label, icons) ->
              val selected = when (route) {
                is ZenixRoute.Home -> currentDest?.hasRoute<ZenixRoute.Home>() == true
                is ZenixRoute.ForYouAi -> currentDest?.hasRoute<ZenixRoute.ForYouAi>() == true
                is ZenixRoute.Explore -> currentDest?.hasRoute<ZenixRoute.Explore>() == true
                is ZenixRoute.AccountProfiles -> currentDest?.hasRoute<ZenixRoute.AccountProfiles>() == true
                else -> false
              }
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(route) {
                    popUpTo(ZenixRoute.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = Color.White,
                  selectedTextColor = ZenixTextPrimary,
                  indicatorColor = ZenixCrimson.copy(alpha = 0.85f),
                  unselectedIconColor = ZenixTextMuted,
                  unselectedTextColor = ZenixTextMuted,
                ),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = ZenixRoute.Home,
          ) {
            composable<ZenixRoute.Home> {
              HomeScreen(
                activeProfile = profile,
                recommendations = localRecommendations,
                watchHistory = watchHistory,
                watchlist = watchlist,
                onPlayContent = { content, epId ->
                  navigateToPlayer(content.id, epId)
                },
                onOpenDetails = { detailPick = it },
                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                onSwitchProfileClick = { viewModel.clearActiveProfile() },
                onOpenSearch = {
                  navController.navigate(ZenixRoute.Explore) {
                    popUpTo(ZenixRoute.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                onOpenForYouAi = {
                  navController.navigate(ZenixRoute.ForYouAi) {
                    popUpTo(ZenixRoute.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
              )
            }

            composable<ZenixRoute.ForYouAi> {
              ForYouAiScreen(
                activeProfile = profile,
                moodPrompt = aiMoodPrompt,
                aiState = aiRecommendations,
                localRecommendations = localRecommendations,
                watchHistory = watchHistory,
                watchlist = watchlist,
                ratings = ratings,
                onMoodPromptChange = { viewModel.updateAiMoodPrompt(it) },
                onGenerateAi = { viewModel.generateAiRecommendations(it) },
                onPlayContent = { content, epId ->
                  navigateToPlayer(content.id, epId)
                },
                onOpenDetails = { detailPick = it },
                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
              )
            }

            composable<ZenixRoute.Explore> {
              ExploreScreen(
                activeProfile = profile,
                searchQuery = searchQuery,
                selectedGenre = selectedGenre,
                recommendations = localRecommendations,
                watchlist = watchlist,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSelectGenre = { viewModel.selectGenre(it) },
                onOpenDetails = { detailPick = it },
              )
            }

            composable<ZenixRoute.AccountProfiles> {
              AccountProfilesScreen(
                user = user,
                profiles = profiles,
                activeProfile = profile,
                onSwitchProfile = { viewModel.selectProfile(it.id) },
                onCreateProfile = { name, avatarId, maturity, genres, autoplay ->
                  viewModel.createProfile(name, avatarId, maturity, genres, autoplay, selectOnSuccess = false)
                },
                onUpdateProfile = { id, name, avatarId, maturity, genres, autoplay ->
                  viewModel.updateProfile(id, name, avatarId, maturity, genres, autoplay)
                },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onSignOut = onSignOut,
              )
            }

            composable<ZenixRoute.Player> { backStackEntry ->
              val route = backStackEntry.toRoute<ZenixRoute.Player>()
              val historyEntry = watchHistory.firstOrNull { it.contentId == route.contentId }
              VideoPlayerScreen(
                contentId = route.contentId,
                initialEpisodeId = route.episodeId,
                autoplayNextEnabled = profile.autoplayNext,
                existingHistory = historyEntry,
                onSaveProgress = { cid, epId, pos, dur ->
                  viewModel.savePlaybackProgress(cid, epId, pos, dur)
                },
                onNavigateBack = {
                  navController.popBackStack()
                },
              )
            }
          }
        }
      }
    }
  }

  val activeDetail = detailPick
  if (activeDetail != null) {
    val inList = watchlist.any { it.contentId == activeDetail.content.id }
    val rating = ratings.firstOrNull { it.contentId == activeDetail.content.id }
      ?.let { RatingSentiment.fromRaw(it.sentiment) }
    val historyEntry = watchHistory.firstOrNull { it.contentId == activeDetail.content.id }
    val similar = localRecommendations.filter {
      it.content.id != activeDetail.content.id &&
        (it.content.primaryGenre == activeDetail.content.primaryGenre ||
          it.content.tags.any { tag -> tag in activeDetail.content.tags })
    }.take(6)

    TitleDetailBottomSheet(
      pick = activeDetail,
      inWatchlist = inList,
      rating = rating,
      historyEntry = historyEntry,
      similarPicks = similar,
      onDismiss = { detailPick = null },
      onPlay = { content, epId -> navigateToPlayer(content.id, epId) },
      onToggleWatchlist = { viewModel.toggleWatchlist(it) },
      onRate = { cid, sentiment -> viewModel.setRating(cid, sentiment) },
      onSelectSimilar = { detailPick = it },
    )
  }
}
