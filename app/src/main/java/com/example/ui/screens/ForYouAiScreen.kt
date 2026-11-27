package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.domain.model.MediaContent
import com.example.domain.model.RatingSentiment
import com.example.domain.model.RecommendedPick
import com.example.domain.model.TitleRating
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.WatchlistItem
import com.example.ui.components.MatchBadge
import com.example.ui.components.QualityChip
import com.example.ui.components.ZenixArtwork
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixSurfaceHigh
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary
import com.example.ui.theme.ZenixVioletAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForYouAiScreen(
  activeProfile: ViewerProfile,
  moodPrompt: String,
  aiState: UiState<List<RecommendedPick>>?,
  localRecommendations: List<RecommendedPick>,
  watchHistory: List<WatchHistoryEntry>,
  watchlist: List<WatchlistItem>,
  ratings: List<TitleRating>,
  onMoodPromptChange: (String) -> Unit,
  onGenerateAi: (String?) -> Unit,
  onPlayContent: (MediaContent, String) -> Unit,
  onOpenDetails: (RecommendedPick) -> Unit,
  onToggleWatchlist: (MediaContent) -> Unit,
) {
  val watchlistIds = remember(watchlist) { watchlist.map { it.contentId }.toSet() }
  val likedCount = remember(ratings) { ratings.count { it.sentiment == RatingSentiment.LIKE.name } }
  val moodPresets = remember {
    listOf(
      "Mind-bending sci-fi tonight",
      "Rainy neon noir mystery",
      "High-speed adrenaline under 20m",
      "Calming 4K ocean & nature",
      "Surprise me with hidden gems",
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian)
      .statusBarsPadding(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item(key = "ai_header") {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = ZenixVioletAccent,
          )
          Text(
            text = "Zenix Recommendation Engine",
            style = MaterialTheme.typography.headlineLarge,
            color = ZenixTextPrimary,
          )
        }
        Text(
          text = "Live behavioral scoring + Gemini AI curation personalized for ${activeProfile.name}",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenixTextSecondary,
        )
      }
    }

    item(key = "taste_dna") {
      Surface(
        color = ZenixSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ZenixOutline),
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${activeProfile.name}'s Taste DNA",
            style = MaterialTheme.typography.titleMedium,
            color = ZenixTextPrimary,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            QualityChip(text = "${watchHistory.size} Watched")
            QualityChip(text = "${watchlist.size} In My List")
            QualityChip(text = "$likedCount Liked")
            QualityChip(text = activeProfile.maturityLevel, highlight = activeProfile.maturityLevel == "KIDS")
            activeProfile.preferredGenres.forEach { genre ->
              QualityChip(text = genre)
            }
          }
        }
      }
    }

    item(key = "ai_prompt_box") {
      Surface(
        color = ZenixSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ZenixVioletAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Mood & Context Curator (Gemini AI)",
            style = MaterialTheme.typography.titleMedium,
            color = ZenixTextPrimary,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            moodPresets.forEach { preset ->
              AssistChip(
                onClick = { onGenerateAi(preset) },
                label = { Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = AssistChipDefaults.assistChipColors(
                  containerColor = ZenixSurfaceHigh,
                  labelColor = ZenixTextPrimary,
                ),
              )
            }
          }

          OutlinedTextField(
            value = moodPrompt,
            onValueChange = onMoodPromptChange,
            label = { Text("Describe what you want to watch") },
            placeholder = { Text("e.g., Tense cyberpunk thriller with great synth visuals") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ZenixVioletAccent,
              unfocusedBorderColor = ZenixOutline,
              focusedLabelColor = ZenixVioletAccent,
              unfocusedLabelColor = ZenixTextSecondary,
              focusedTextColor = ZenixTextPrimary,
              unfocusedTextColor = ZenixTextPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("ai_mood_input"),
          )

          Button(
            onClick = { onGenerateAi(null) },
            enabled = aiState !is UiState.Loading,
            colors = ButtonDefaults.buttonColors(
              containerColor = ZenixVioletAccent,
              contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("ai_recommend_button"),
          ) {
            if (aiState is UiState.Loading) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Analyzing Taste & Curating...")
            } else {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Curate Personalized Picks")
            }
          }
        }
      }
    }

    when (aiState) {
      is UiState.Loading -> {
        item(key = "ai_loading") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Matching catalog signals with ${activeProfile.name}'s watch history...",
              style = MaterialTheme.typography.bodyMedium,
              color = ZenixTextSecondary,
            )
          }
        }
      }

      is UiState.Error -> {
        item(key = "ai_error") {
          Surface(
            color = Color(0xFF3A1216),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ZenixCrimson.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "AI Recommendation Unavailable",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD1D1),
              )
              Text(
                text = aiState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFB4B4),
              )
              OutlinedButton(
                onClick = { onGenerateAi(null) },
                border = BorderStroke(1.dp, Color(0xFFFFB4B4)),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry AI Engine", color = Color.White)
              }
            }
          }
        }
      }

      is UiState.Success -> {
        item(key = "ai_results_title") {
          Text(
            text = "Gemini AI Curated For You",
            style = MaterialTheme.typography.titleLarge,
            color = ZenixTextPrimary,
          )
        }
        items(aiState.data, key = { "ai_${it.content.id}" }) { pick ->
          RecommendationHighlightCard(
            pick = pick,
            inWatchlist = pick.content.id in watchlistIds,
            onPlay = {
              val epId = pick.content.episodes.firstOrNull()?.episodeId.orEmpty()
              onPlayContent(pick.content, epId)
            },
            onDetails = { onOpenDetails(pick) },
            onToggleWatchlist = { onToggleWatchlist(pick.content) },
          )
        }
      }

      null -> {
        item(key = "realtime_engine_title") {
          Text(
            text = "Real-Time Match Ranking",
            style = MaterialTheme.typography.titleLarge,
            color = ZenixTextPrimary,
          )
        }
        items(localRecommendations.take(6), key = { "local_${it.content.id}" }) { pick ->
          RecommendationHighlightCard(
            pick = pick,
            inWatchlist = pick.content.id in watchlistIds,
            onPlay = {
              val epId = pick.content.episodes.firstOrNull()?.episodeId.orEmpty()
              onPlayContent(pick.content, epId)
            },
            onDetails = { onOpenDetails(pick) },
            onToggleWatchlist = { onToggleWatchlist(pick.content) },
          )
        }
      }
    }
  }
}

@Composable
private fun RecommendationHighlightCard(
  pick: RecommendedPick,
  inWatchlist: Boolean,
  onPlay: () -> Unit,
  onDetails: () -> Unit,
  onToggleWatchlist: () -> Unit,
) {
  val content = pick.content
  Surface(
    color = ZenixSurfaceElevated,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
      1.dp,
      if (pick.aiCurated) ZenixVioletAccent.copy(alpha = 0.5f) else ZenixOutline,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 680.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onDetails)
      .testTag("recommendation_card_${content.id}"),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(155.dp),
      ) {
        ZenixArtwork(content = content, useBackdrop = true, modifier = Modifier.fillMaxSize())
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, ZenixSurfaceElevated.copy(alpha = 0.95f))
              )
            )
        )
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MatchBadge(matchPercent = pick.matchPercent)
          if (pick.moodTag.isNotBlank()) {
            QualityChip(text = pick.moodTag, highlight = pick.aiCurated)
          }
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
          Text(
            text = content.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
          Text(
            text = "${content.primaryGenre} • ${content.durationLabel} • ${content.qualityBadge}",
            style = MaterialTheme.typography.labelSmall,
            color = ZenixTextSecondary,
          )
        }
      }
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = pick.reason,
          style = MaterialTheme.typography.bodyLarge,
          color = ZenixTextPrimary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(
              containerColor = ZenixCrimson,
              contentColor = Color.White,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Watch Now")
          }
          OutlinedButton(
            onClick = onToggleWatchlist,
            border = BorderStroke(1.dp, if (inWatchlist) ZenixEmeraldMatch else ZenixOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp),
          ) {
            Icon(
              imageVector = if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
              contentDescription = if (inWatchlist) "Listed" else "Add to My List",
              tint = if (inWatchlist) ZenixEmeraldMatch else ZenixTextPrimary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (inWatchlist) "Listed" else "My List",
              color = if (inWatchlist) ZenixEmeraldMatch else ZenixTextPrimary,
            )
          }
        }
      }
    }
  }
}
