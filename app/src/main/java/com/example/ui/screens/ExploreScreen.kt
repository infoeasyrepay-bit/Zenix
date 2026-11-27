package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ZenixCatalog
import com.example.domain.model.MaturityLevel
import com.example.domain.model.RecommendedPick
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchlistItem
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary

@Composable
fun ExploreScreen(
  activeProfile: ViewerProfile,
  searchQuery: String,
  selectedGenre: String,
  recommendations: List<RecommendedPick>,
  watchlist: List<WatchlistItem>,
  onSearchQueryChange: (String) -> Unit,
  onSelectGenre: (String) -> Unit,
  onOpenDetails: (RecommendedPick) -> Unit,
) {
  var onlyMyList by remember { mutableStateOf(false) }
  val isKids = MaturityLevel.fromRaw(activeProfile.maturityLevel) == MaturityLevel.KIDS
  val genres = remember(isKids) {
    if (isKids) listOf("All", "Animation", "Documentary", "Fantasy", "Action")
    else listOf("All") + ZenixCatalog.allGenres
  }
  val watchlistIds = remember(watchlist) { watchlist.map { it.contentId }.toSet() }

  val filtered = remember(recommendations, searchQuery, selectedGenre, onlyMyList, watchlistIds) {
    recommendations.filter { pick ->
      val item = pick.content
      val matchesSearch = searchQuery.isBlank() ||
        item.title.contains(searchQuery, ignoreCase = true) ||
        item.primaryGenre.contains(searchQuery, ignoreCase = true) ||
        item.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
        item.synopsis.contains(searchQuery, ignoreCase = true)
      val matchesGenre = selectedGenre == "All" || item.primaryGenre.equals(selectedGenre, ignoreCase = true)
      val matchesList = !onlyMyList || item.id in watchlistIds
      matchesSearch && matchesGenre && matchesList
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian)
      .statusBarsPadding()
      .padding(top = 16.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Explore Catalog",
        style = MaterialTheme.typography.headlineLarge,
        color = ZenixTextPrimary,
      )

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search titles, genres, moods...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ZenixTextSecondary) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = ZenixTextSecondary)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ZenixCrimson,
          unfocusedBorderColor = ZenixOutline,
          focusedContainerColor = ZenixSurfaceElevated,
          unfocusedContainerColor = ZenixSurfaceElevated,
          focusedTextColor = ZenixTextPrimary,
          unfocusedTextColor = ZenixTextPrimary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("explore_search_input"),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item(key = "my_list_filter") {
        FilterChip(
          selected = onlyMyList,
          onClick = { onlyMyList = !onlyMyList },
          label = { Text("My List (${watchlist.size})") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Filled.Bookmark,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ZenixEmeraldMatch.copy(alpha = 0.25f),
            selectedLabelColor = ZenixEmeraldMatch,
            selectedLeadingIconColor = ZenixEmeraldMatch,
            containerColor = ZenixSurfaceElevated,
            labelColor = ZenixTextSecondary,
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = onlyMyList,
            borderColor = ZenixOutline,
            selectedBorderColor = ZenixEmeraldMatch,
          ),
        )
      }
      items(genres) { genre ->
        val active = genre == selectedGenre
        FilterChip(
          selected = active,
          onClick = { onSelectGenre(genre) },
          label = { Text(genre) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ZenixCrimson,
            selectedLabelColor = Color.White,
            containerColor = ZenixSurfaceElevated,
            labelColor = ZenixTextSecondary,
          ),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (filtered.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.MovieFilter,
            contentDescription = null,
            tint = ZenixTextSecondary,
            modifier = Modifier.size(48.dp),
          )
          Text(
            text = if (onlyMyList) "Your watchlist is empty" else "No matching titles",
            style = MaterialTheme.typography.titleLarge,
            color = ZenixTextPrimary,
          )
          Text(
            text = if (onlyMyList) "Tap + My List on any movie or series to save it here."
            else "Try clearing your genre or search filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenixTextSecondary,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 132.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(filtered, key = { it.content.id }) { pick ->
          MediaPosterCard(
            pick = pick,
            onClick = { onOpenDetails(pick) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}
