package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ZenixCatalog
import com.example.domain.model.MaturityLevel
import com.example.domain.model.MediaContent
import com.example.domain.model.RecommendedPick
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.WatchlistItem
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.MatchBadge
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.QualityChip
import com.example.ui.components.ZenixArtwork
import com.example.ui.components.ZenixWordmark
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary
import com.example.ui.theme.ZenixVioletAccent

@Composable
fun HomeScreen(
  activeProfile: ViewerProfile,
  recommendations: List<RecommendedPick>,
  watchHistory: List<WatchHistoryEntry>,
  watchlist: List<WatchlistItem>,
  onPlayContent: (MediaContent, String) -> Unit,
  onOpenDetails: (RecommendedPick) -> Unit,
  onToggleWatchlist: (MediaContent) -> Unit,
  onSwitchProfileClick: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenForYouAi: () -> Unit,
) {
  val isKids = MaturityLevel.fromRaw(activeProfile.maturityLevel) == MaturityLevel.KIDS
  var selectedCategory by remember { mutableStateOf("All") }

  val categories = remember(isKids) {
    if (isKids) listOf("All", "Animation", "Documentary", "Fantasy", "Action")
    else listOf("All") + ZenixCatalog.allGenres
  }

  val filteredPicks = remember(recommendations, selectedCategory) {
    if (selectedCategory == "All") recommendations
    else recommendations.filter { it.content.primaryGenre.equals(selectedCategory, ignoreCase = true) }
  }

  val heroPick = filteredPicks.firstOrNull() ?: recommendations.firstOrNull()
  val watchlistIds = remember(watchlist) { watchlist.map { it.contentId }.toSet() }

  val continueWatchingPairs = remember(watchHistory, isKids) {
    watchHistory
      .filter { !it.completed && it.progressPercent > 0 }
      .mapNotNull { entry ->
        val content = ZenixCatalog.findById(entry.contentId)
        if (content != null && (!isKids || content.isKidsSafe)) entry to content else null
      }
  }

  val zenixOriginals = remember(recommendations) {
    recommendations.filter { it.content.isZenixOriginal }
  }

  val myListPicks = remember(recommendations, watchlistIds) {
    recommendations.filter { it.content.id in watchlistIds }
  }

  val lastWatched = continueWatchingPairs.firstOrNull()?.second
    ?: watchHistory.firstOrNull()?.let { ZenixCatalog.findById(it.contentId) }
  val becauseYouWatchedPicks = remember(recommendations, lastWatched) {
    if (lastWatched == null) emptyList()
    else recommendations.filter {
      it.content.id != lastWatched.id &&
        (it.content.primaryGenre == lastWatched.primaryGenre ||
          it.content.tags.any { tag -> tag in lastWatched.tags })
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian),
    contentPadding = PaddingValues(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp),
  ) {
    item(key = "hero_banner") {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(470.dp),
      ) {
        if (heroPick != null) {
          val hero = heroPick.content
          val inMyList = hero.id in watchlistIds
          ZenixArtwork(
            content = hero,
            useBackdrop = true,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color.Black.copy(alpha = 0.65f),
                    Color.Transparent,
                    ZenixObsidian.copy(alpha = 0.6f),
                    ZenixObsidian,
                  )
                )
              )
          )

          // Top Floating Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ZenixWordmark(fontSizeSp = 21)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.45f))
                  .testTag("home_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Search,
                  contentDescription = "Search catalog",
                  tint = Color.White,
                )
              }
              Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(24.dp))
                  .clickable(onClick = onSwitchProfileClick)
                  .testTag("home_profile_switcher"),
              ) {
                Row(
                  modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  ProfileAvatarBadge(
                    avatarId = activeProfile.avatarId,
                    size = 30.dp,
                    isKids = isKids,
                  )
                  Text(
                    text = activeProfile.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          // Hero Content Details & Actions
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              MatchBadge(matchPercent = heroPick.matchPercent)
              QualityChip(text = hero.qualityBadge, highlight = true)
              QualityChip(text = hero.primaryGenre)
            }

            Text(
              text = hero.title,
              style = MaterialTheme.typography.displayMedium,
              color = Color.White,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = hero.tagline,
              style = MaterialTheme.typography.bodyLarge,
              color = ZenixTextSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = {
                  val epId = hero.episodes.firstOrNull()?.episodeId.orEmpty()
                  onPlayContent(hero, epId)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color.White,
                  contentColor = ZenixObsidian,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("hero_play_button"),
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.play_now),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }

              OutlinedButton(
                onClick = { onToggleWatchlist(hero) },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("hero_my_list_button"),
              ) {
                Icon(
                  imageVector = if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
                  contentDescription = if (inMyList) "In My List" else "Add to My List",
                  tint = if (inMyList) ZenixEmeraldMatch else Color.White,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (inMyList) "Listed" else "My List",
                  color = if (inMyList) ZenixEmeraldMatch else Color.White,
                )
              }

              IconButton(
                onClick = { onOpenDetails(heroPick) },
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(ZenixSurfaceElevated.copy(alpha = 0.8f))
                  .testTag("hero_info_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Info,
                  contentDescription = "More info about ${hero.title}",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    }

    // Genre Filter Pills + AI Concierge Banner
    item(key = "genre_pills") {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(categories) { category ->
            val selected = category == selectedCategory
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = category },
              label = { Text(category) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ZenixCrimson,
                selectedLabelColor = Color.White,
                containerColor = ZenixSurfaceElevated,
                labelColor = ZenixTextSecondary,
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = ZenixOutline,
                selectedBorderColor = ZenixCrimson,
              ),
            )
          }
        }

        Surface(
          color = ZenixSurfaceElevated,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, ZenixVioletAccent.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenForYouAi)
            .testTag("home_ai_banner"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              color = ZenixVioletAccent.copy(alpha = 0.22f),
              shape = CircleShape,
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = ZenixVioletAccent,
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Zenix AI Concierge",
                style = MaterialTheme.typography.titleMedium,
                color = ZenixTextPrimary,
              )
              Text(
                text = "Ask Gemini for mood-matched recommendations tuned for ${activeProfile.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = ZenixTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "Open →",
              style = MaterialTheme.typography.labelLarge,
              color = ZenixVioletAccent,
            )
          }
        }
      }
    }

    // Continue Watching Row
    if (continueWatchingPairs.isNotEmpty()) {
      item(key = "continue_watching") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SectionHeader(title = "Continue Watching for ${activeProfile.name}")
          LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(continueWatchingPairs, key = { it.first.contentId }) { (entry, content) ->
              ContinueWatchingCard(
                content = content,
                history = entry,
                onPlayClick = { onPlayContent(content, entry.episodeId) },
                onDetailsClick = {
                  val pick = recommendations.firstOrNull { it.content.id == content.id }
                    ?: RecommendedPick(content, 92, "Continue from ${entry.progressPercent}%")
                  onOpenDetails(pick)
                },
              )
            }
          }
        }
      }
    }

    // Top Picks For You
    item(key = "top_picks") {
      CarouselSection(
        title = if (selectedCategory == "All") "Top Picks for ${activeProfile.name}" else "Top in $selectedCategory",
        picks = filteredPicks,
        onSelectPick = onOpenDetails,
      )
    }

    // Because You Watched
    if (lastWatched != null && becauseYouWatchedPicks.isNotEmpty() && selectedCategory == "All") {
      item(key = "because_you_watched") {
        CarouselSection(
          title = "Because You Watched ${lastWatched.title}",
          picks = becauseYouWatchedPicks,
          onSelectPick = onOpenDetails,
        )
      }
    }

    // Zenix Originals
    if (zenixOriginals.isNotEmpty() && selectedCategory == "All") {
      item(key = "zenix_originals") {
        CarouselSection(
          title = "Zenix Originals • 4K Showcase",
          picks = zenixOriginals,
          onSelectPick = onOpenDetails,
        )
      }
    }

    // My List Row
    if (myListPicks.isNotEmpty() && selectedCategory == "All") {
      item(key = "my_list_row") {
        CarouselSection(
          title = "${activeProfile.name}'s Watchlist",
          picks = myListPicks,
          onSelectPick = onOpenDetails,
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleLarge,
    color = ZenixTextPrimary,
    modifier = Modifier.padding(horizontal = 20.dp),
  )
}

@Composable
private fun CarouselSection(
  title: String,
  picks: List<RecommendedPick>,
  onSelectPick: (RecommendedPick) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionHeader(title = title)
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(picks, key = { it.content.id }) { pick ->
        MediaPosterCard(
          pick = pick,
          onClick = { onSelectPick(pick) },
        )
      }
    }
  }
}
