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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.MediaContent
import com.example.domain.model.RatingSentiment
import com.example.domain.model.RecommendedPick
import com.example.domain.model.WatchHistoryEntry
import com.example.ui.components.MatchBadge
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.QualityChip
import com.example.ui.components.ZenixArtwork
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceDark
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TitleDetailBottomSheet(
  pick: RecommendedPick,
  inWatchlist: Boolean,
  rating: RatingSentiment?,
  historyEntry: WatchHistoryEntry?,
  similarPicks: List<RecommendedPick>,
  onDismiss: () -> Unit,
  onPlay: (MediaContent, String) -> Unit,
  onToggleWatchlist: (MediaContent) -> Unit,
  onRate: (String, RatingSentiment) -> Unit,
  onSelectSimilar: (RecommendedPick) -> Unit,
) {
  val content = pick.content
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val resumeEpisodeId = historyEntry?.episodeId?.takeIf { it.isNotBlank() }
    ?: content.episodes.firstOrNull()?.episodeId.orEmpty()
  val hasProgress = historyEntry != null && historyEntry.progressPercent in 1..94

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ZenixSurfaceDark,
    contentColor = ZenixTextPrimary,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 680.dp)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 28.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(230.dp),
      ) {
        ZenixArtwork(
          content = content,
          useBackdrop = true,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.2f),
                  Color.Transparent,
                  ZenixSurfaceDark,
                )
              )
            )
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .minimumInteractiveComponentSize()
            .testTag("detail_close_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close details",
            tint = Color.White,
          )
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
          if (content.isZenixOriginal) {
            Text(
              text = "ZENIX ORIGINAL",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = ZenixCrimson,
            )
          }
          Text(
            text = content.title,
            style = MaterialTheme.typography.headlineLarge,
            color = ZenixTextPrimary,
          )
        }
      }

      Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MatchBadge(matchPercent = pick.matchPercent)
          QualityChip(text = "${content.releaseYear}")
          QualityChip(text = content.maturityRating)
          QualityChip(text = content.qualityBadge, highlight = true)
          QualityChip(text = content.audioBadge)
          QualityChip(text = content.streamProtocol.label)
        }

        Text(
          text = pick.reason,
          style = MaterialTheme.typography.labelLarge,
          color = ZenixEmeraldMatch,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = { onPlay(content, resumeEpisodeId) },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = ZenixObsidian,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("detail_play_button"),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (hasProgress) "Resume (${historyEntry?.progressPercent}%)" else "Play Now",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }

          OutlinedButton(
            onClick = { onToggleWatchlist(content) },
            border = BorderStroke(1.dp, if (inWatchlist) ZenixEmeraldMatch else ZenixOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("detail_watchlist_button"),
          ) {
            Icon(
              imageVector = if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
              contentDescription = if (inWatchlist) "Remove from My List" else "Add to My List",
              tint = if (inWatchlist) ZenixEmeraldMatch else ZenixTextPrimary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (inWatchlist) "Listed" else "My List",
              color = if (inWatchlist) ZenixEmeraldMatch else ZenixTextPrimary,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Rate to tune recommendations:",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenixTextSecondary,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = { onRate(content.id, RatingSentiment.LIKE) },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(if (rating == RatingSentiment.LIKE) ZenixEmeraldMatch.copy(alpha = 0.2f) else ZenixSurfaceElevated)
              .testTag("detail_like_button"),
          ) {
            Icon(
              imageVector = if (rating == RatingSentiment.LIKE) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
              contentDescription = "Like ${content.title}",
              tint = if (rating == RatingSentiment.LIKE) ZenixEmeraldMatch else ZenixTextPrimary,
            )
          }
          IconButton(
            onClick = { onRate(content.id, RatingSentiment.DISLIKE) },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(if (rating == RatingSentiment.DISLIKE) ZenixCrimson.copy(alpha = 0.22f) else ZenixSurfaceElevated)
              .testTag("detail_dislike_button"),
          ) {
            Icon(
              imageVector = if (rating == RatingSentiment.DISLIKE) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
              contentDescription = "Not for me",
              tint = if (rating == RatingSentiment.DISLIKE) ZenixCrimson else ZenixTextPrimary,
            )
          }
        }

        Text(
          text = content.synopsis,
          style = MaterialTheme.typography.bodyLarge,
          color = ZenixTextPrimary,
        )

        Text(
          text = "Starring: ${content.cast.joinToString(", ")}  •  Director: ${content.director}",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenixTextSecondary,
        )

        if (content.isSeries && content.episodes.isNotEmpty()) {
          HorizontalDivider(color = ZenixOutline)
          Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleLarge,
            color = ZenixTextPrimary,
          )
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content.episodes.forEach { ep ->
              val isCurrentEp = historyEntry?.episodeId == ep.episodeId
              Surface(
                color = if (isCurrentEp) ZenixSurfaceElevated else ZenixObsidian.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isCurrentEp) ZenixCrimson.copy(alpha = 0.7f) else ZenixOutline),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onPlay(content, ep.episodeId) }
                  .testTag("episode_row_${ep.episodeId}"),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Surface(
                    color = ZenixCrimson.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play ${ep.title}",
                        tint = Color.White,
                      )
                    }
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = ep.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = ZenixTextPrimary,
                    )
                    Text(
                      text = ep.synopsis,
                      style = MaterialTheme.typography.bodyMedium,
                      color = ZenixTextSecondary,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Text(
                    text = "${ep.durationMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZenixTextSecondary,
                  )
                }
              }
            }
          }
        }

        if (similarPicks.isNotEmpty()) {
          HorizontalDivider(color = ZenixOutline)
          Text(
            text = "More Like This",
            style = MaterialTheme.typography.titleLarge,
            color = ZenixTextPrimary,
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
          ) {
            items(similarPicks, key = { it.content.id }) { sim ->
              MediaPosterCard(
                pick = sim,
                onClick = { onSelectSimilar(sim) },
              )
            }
          }
        }
      }
    }
  }
}
