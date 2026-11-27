package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.ZenixCatalog
import com.example.domain.model.MaturityLevel
import com.example.domain.model.MediaContent
import com.example.domain.model.RecommendedPick
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.ZenixAvatars
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceDark
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixSurfaceHigh
import com.example.ui.theme.ZenixTextMuted
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary
import com.example.ui.theme.ZenixVioletAccent

@Composable
fun ZenixWordmark(
  modifier: Modifier = Modifier,
  fontSizeSp: Int = 26,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .size((fontSizeSp + 4).dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.linearGradient(
            listOf(ZenixCrimson, Color(0xFFFF4D4D))
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Z",
        style = MaterialTheme.typography.titleLarge.copy(
          fontSize = (fontSizeSp - 6).sp,
          fontWeight = FontWeight.ExtraBold,
        ),
        color = Color.White,
      )
    }
    Text(
      text = "ZENIX",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontSize = fontSizeSp.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
      ),
      color = ZenixTextPrimary,
    )
  }
}

@Composable
fun ZenixArtwork(
  content: MediaContent,
  modifier: Modifier = Modifier,
  useBackdrop: Boolean = false,
) {
  val localRes = if (useBackdrop) content.localBackdropRes ?: content.localPosterRes else content.localPosterRes
  val fallbackBrush = remember(content.id) {
    Brush.linearGradient(
      colors = listOf(
        content.accentColor.copy(alpha = 0.55f),
        ZenixSurfaceElevated,
        ZenixObsidian,
      )
    )
  }

  Box(modifier = modifier.background(fallbackBrush)) {
    if (localRes != null) {
      Image(
        painter = painterResource(id = localRes),
        contentDescription = "${content.title} artwork",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (!content.posterUrl.isNullOrBlank()) {
      SubcomposeAsyncImage(
        model = content.posterUrl,
        contentDescription = "${content.title} artwork",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = {
          ArtworkPlaceholder(content = content, brush = fallbackBrush)
        },
        error = {
          ArtworkPlaceholder(content = content, brush = fallbackBrush)
        },
      )
    } else {
      ArtworkPlaceholder(content = content, brush = fallbackBrush)
    }
  }
}

@Composable
private fun ArtworkPlaceholder(content: MediaContent, brush: Brush) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(brush)
      .padding(12.dp),
    contentAlignment = Alignment.BottomStart,
  ) {
    Column {
      Text(
        text = content.primaryGenre.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.75f),
      )
      Text(
        text = content.title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ProfileAvatarBadge(
  avatarId: String,
  size: Dp = 64.dp,
  isSelected: Boolean = false,
  isKids: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val spec = ZenixAvatars.find(avatarId)
  val icon = when (spec.id) {
    "cyber_visor" -> Icons.Filled.Bolt
    "aurora_star" -> Icons.Filled.AutoAwesome
    "amber_cine" -> Icons.Filled.Movie
    "kids_sprout" -> Icons.Filled.ChildCare
    else -> Icons.Filled.Face
  }
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(if (size >= 72.dp) 20.dp else 12.dp))
      .background(Brush.linearGradient(listOf(spec.startColor, spec.endColor)))
      .then(
        if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(if (size >= 72.dp) 20.dp else 12.dp))
        else Modifier
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = spec.label,
      tint = Color.White,
      modifier = Modifier.size(size * 0.48f),
    )
    if (isKids) {
      Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(4.dp),
      ) {
        Text(
          text = "KIDS",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
          color = ZenixEmeraldMatch,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
      }
    }
  }
}

@Composable
fun MatchBadge(
  matchPercent: Int,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ZenixEmeraldMatch.copy(alpha = 0.16f),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, ZenixEmeraldMatch.copy(alpha = 0.4f)),
    modifier = modifier,
  ) {
    Text(
      text = "$matchPercent% Match",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = ZenixEmeraldMatch,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun QualityChip(
  text: String,
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
) {
  Surface(
    color = if (highlight) ZenixCrimson.copy(alpha = 0.22f) else ZenixSurfaceHigh,
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(
      1.dp,
      if (highlight) ZenixCrimson.copy(alpha = 0.6f) else ZenixOutline,
    ),
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = if (highlight) Color(0xFFFFB4B4) else ZenixTextSecondary,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun MediaPosterCard(
  pick: RecommendedPick,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showMatch: Boolean = true,
) {
  val content = pick.content
  Column(
    modifier = modifier
      .width(138.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag("poster_card_${content.id}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(3f / 4f)
        .clip(RoundedCornerShape(14.dp))
        .border(1.dp, ZenixOutline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
    ) {
      ZenixArtwork(content = content, modifier = Modifier.fillMaxSize())

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Black.copy(alpha = 0.25f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.85f),
              )
            )
          )
      )

      if (content.isZenixOriginal) {
        Surface(
          color = ZenixCrimson,
          shape = RoundedCornerShape(5.dp),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
        ) {
          Text(
            text = "Z ORIGINAL",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      if (showMatch) {
        Surface(
          color = Color.Black.copy(alpha = 0.72f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        ) {
          Text(
            text = "${pick.matchPercent}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ZenixEmeraldMatch,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(10.dp),
      ) {
        Text(
          text = content.title,
          style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${content.primaryGenre} • ${content.qualityBadge}",
          style = MaterialTheme.typography.labelSmall,
          color = ZenixTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ContinueWatchingCard(
  content: MediaContent,
  history: WatchHistoryEntry,
  onPlayClick: () -> Unit,
  onDetailsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ZenixSurfaceElevated,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, ZenixOutline),
    modifier = modifier
      .width(220.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onPlayClick)
      .testTag("continue_card_${content.id}"),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(122.dp),
      ) {
        ZenixArtwork(content = content, useBackdrop = true, modifier = Modifier.fillMaxSize())
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            modifier = Modifier.size(48.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Resume ${content.title}",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
        LinearProgressIndicator(
          progress = { (history.progressPercent / 100f).coerceIn(0.03f, 1f) },
          color = ZenixCrimson,
          trackColor = Color.White.copy(alpha = 0.25f),
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .align(Alignment.BottomCenter),
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = content.title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            color = ZenixTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          val epLabel = if (history.episodeId.isNotBlank()) {
            "${history.episodeId.uppercase()} • ${history.progressPercent}%"
          } else {
            "${history.progressPercent}% watched"
          }
          Text(
            text = epLabel,
            style = MaterialTheme.typography.labelSmall,
            color = ZenixTextSecondary,
          )
        }
        IconButton(
          onClick = onDetailsClick,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Details for ${content.title}",
            tint = ZenixTextSecondary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorSheet(
  existingProfile: ViewerProfile?,
  canDelete: Boolean,
  onDismiss: () -> Unit,
  onSave: (name: String, avatarId: String, maturity: MaturityLevel, genres: List<String>, autoplayNext: Boolean) -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(existingProfile) { mutableStateOf(existingProfile?.name ?: "") }
  var selectedAvatar by remember(existingProfile) { mutableStateOf(existingProfile?.avatarId ?: "crimson_fox") }
  var maturity by remember(existingProfile) {
    mutableStateOf(MaturityLevel.fromRaw(existingProfile?.maturityLevel))
  }
  var autoplayNext by remember(existingProfile) { mutableStateOf(existingProfile?.autoplayNext ?: true) }
  val selectedGenres = remember(existingProfile) {
    mutableStateListOf<String>().apply {
      addAll(existingProfile?.preferredGenres ?: listOf("Sci-Fi", "Cyberpunk"))
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ZenixSurfaceDark,
    contentColor = ZenixTextPrimary,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existingProfile == null) "Create Viewer Profile" else "Edit Profile",
          style = MaterialTheme.typography.headlineMedium,
          color = ZenixTextPrimary,
        )
        if (existingProfile != null && canDelete && onDelete != null) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_profile_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Delete profile",
              tint = Color(0xFFFF6B6B),
            )
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 24) name = it },
        label = { Text("Profile Name") },
        placeholder = { Text("e.g., Alex, Cinema Club, Kids") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ZenixCrimson,
          unfocusedBorderColor = ZenixOutline,
          focusedLabelColor = ZenixCrimson,
          unfocusedLabelColor = ZenixTextSecondary,
          focusedTextColor = ZenixTextPrimary,
          unfocusedTextColor = ZenixTextPrimary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_name_input"),
      )

      Text(
        text = "Choose Avatar",
        style = MaterialTheme.typography.titleMedium,
        color = ZenixTextPrimary,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ZenixAvatars.presets.forEach { preset ->
          val chosen = preset.id == selectedAvatar
          ProfileAvatarBadge(
            avatarId = preset.id,
            size = 54.dp,
            isSelected = chosen,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedAvatar = preset.id },
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ZenixSurfaceElevated)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = if (maturity == MaturityLevel.KIDS) ZenixEmeraldMatch else ZenixTextSecondary,
          )
          Column {
            Text("Kids Safe Mode", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "Restrict catalog to G / PG / TV-Y titles",
              style = MaterialTheme.typography.bodyMedium,
              color = ZenixTextSecondary,
            )
          }
        }
        Switch(
          checked = maturity == MaturityLevel.KIDS,
          onCheckedChange = { isKids ->
            maturity = if (isKids) MaturityLevel.KIDS else MaturityLevel.ALL
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ZenixEmeraldMatch,
          ),
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ZenixSurfaceElevated)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Autoplay Next Episode", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Seamlessly queue the next episode",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenixTextSecondary,
          )
        }
        Switch(
          checked = autoplayNext,
          onCheckedChange = { autoplayNext = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ZenixCrimson,
          ),
        )
      }

      Text(
        text = "Taste Seeds (Boosts Recommendations)",
        style = MaterialTheme.typography.titleMedium,
        color = ZenixTextPrimary,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZenixCatalog.allGenres.forEach { genre ->
          val active = genre in selectedGenres
          FilterChip(
            selected = active,
            onClick = {
              if (active) selectedGenres.remove(genre)
              else if (selectedGenres.size < 8) selectedGenres.add(genre)
            },
            label = { Text(genre) },
            leadingIcon = if (active) {
              { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ZenixCrimson.copy(alpha = 0.25f),
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = ZenixCoralColor,
              containerColor = ZenixSurfaceElevated,
              labelColor = ZenixTextSecondary,
            ),
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(48.dp),
          border = BorderStroke(1.dp, ZenixOutline),
        ) {
          Text("Cancel", color = ZenixTextPrimary)
        }
        Button(
          onClick = {
            if (name.trim().isNotEmpty()) {
              onSave(name.trim(), selectedAvatar, maturity, selectedGenres.toList(), autoplayNext)
            }
          },
          enabled = name.trim().isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = ZenixCrimson),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("save_profile_button"),
        ) {
          Text("Save Profile", color = Color.White)
        }
      }
    }
  }
}

private val ZenixCoralColor = Color(0xFFFF4D4D)
