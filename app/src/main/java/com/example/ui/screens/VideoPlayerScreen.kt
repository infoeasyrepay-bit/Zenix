package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.ZenixCatalog
import com.example.domain.model.Episode
import com.example.domain.model.WatchHistoryEntry
import com.example.ui.components.QualityChip
import com.example.ui.player.ZenixPlayerController
import com.example.ui.player.formatTimecode
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
  contentId: String,
  initialEpisodeId: String,
  autoplayNextEnabled: Boolean,
  existingHistory: WatchHistoryEntry?,
  onSaveProgress: (contentId: String, episodeId: String, positionMs: Long, durationMs: Long) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val context = LocalContext.current
  val content = remember(contentId) { ZenixCatalog.findById(contentId) }

  if (content == null) {
    Box(
      modifier = Modifier.fillMaxSize().background(Color.Black),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Title unavailable", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onNavigateBack) { Text("Go Back") }
      }
    }
    return
  }

  var activeEpisodeId by remember(contentId, initialEpisodeId) {
    val fallbackEp = content.episodes.firstOrNull()?.episodeId.orEmpty()
    mutableStateOf(initialEpisodeId.ifBlank { fallbackEp })
  }

  val activeEpisode: Episode? = remember(content, activeEpisodeId) {
    content.episodes.firstOrNull { it.episodeId == activeEpisodeId }
  }
  val nextEpisode: Episode? = remember(content, activeEpisode) {
    if (activeEpisode == null) null
    else {
      val idx = content.episodes.indexOfFirst { it.episodeId == activeEpisode.episodeId }
      if (idx in 0 until content.episodes.lastIndex) content.episodes[idx + 1] else null
    }
  }

  val streamUrl = activeEpisode?.videoUrl ?: content.videoUrl
  val streamProtocol = activeEpisode?.streamProtocol ?: content.streamProtocol
  val startPositionMs = remember(contentId, activeEpisodeId, existingHistory) {
    if (existingHistory != null &&
      existingHistory.episodeId == activeEpisodeId &&
      !existingHistory.completed &&
      existingHistory.positionMs > 3_000L
    ) {
      existingHistory.positionMs
    } else 0L
  }

  val controller = remember(context) { ZenixPlayerController(context) }

  var controlsVisible by remember { mutableStateOf(true) }
  var isLocked by remember { mutableStateOf(false) }
  var resizeZoom by remember { mutableStateOf(false) }
  var showSpeedMenu by remember { mutableStateOf(false) }
  var isDraggingSlider by remember { mutableStateOf(false) }
  var sliderDragFraction by remember { mutableFloatStateOf(0f) }
  var nextEpCountdown by remember { mutableIntStateOf(5) }

  LaunchedEffect(streamUrl) {
    controller.prepareMedia(streamUrl, startPositionMs)
  }

  // Real-time progress polling + periodic Firestore sync every 5 seconds
  LaunchedEffect(controller, activeEpisodeId) {
    var ticks = 0
    while (true) {
      controller.pollProgress()
      ticks++
      if (ticks % 10 == 0 && controller.durationMs > 0L) {
        onSaveProgress(content.id, activeEpisodeId, controller.currentPositionMs, controller.durationMs)
      }
      delay(500L)
    }
  }

  // Auto-hide HUD after 4 seconds while playing
  LaunchedEffect(controlsVisible, controller.isPlaying, isLocked, showSpeedMenu) {
    if (controlsVisible && controller.isPlaying && !isLocked && !showSpeedMenu) {
      delay(4000L)
      controlsVisible = false
    }
  }

  // Auto-play next episode countdown
  val showUpNext = nextEpisode != null &&
    ((controller.durationMs > 10_000L && controller.currentPositionMs >= controller.durationMs - 10_000L) || controller.hasEnded)

  LaunchedEffect(showUpNext, activeEpisodeId, autoplayNextEnabled) {
    if (showUpNext && autoplayNextEnabled && nextEpisode != null) {
      nextEpCountdown = 5
      while (nextEpCountdown > 0) {
        delay(1000L)
        nextEpCountdown--
      }
      onSaveProgress(content.id, activeEpisodeId, controller.durationMs, controller.durationMs)
      activeEpisodeId = nextEpisode.episodeId
    }
  }

  val saveAndExit = {
    if (controller.durationMs > 0L) {
      onSaveProgress(content.id, activeEpisodeId, controller.currentPositionMs, controller.durationMs)
    }
    controller.pause()
    onNavigateBack()
  }

  BackHandler {
    saveAndExit()
  }

  DisposableEffect(controller) {
    onDispose {
      if (controller.durationMs > 0L) {
        onSaveProgress(content.id, activeEpisodeId, controller.currentPositionMs, controller.durationMs)
      }
      controller.release()
    }
  }

  val showSkipIntro = activeEpisode != null &&
    controller.currentPositionMs in activeEpisode.introStartMs..activeEpisode.introEndMs

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) {
        controlsVisible = !controlsVisible
      },
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          useController = false
          layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
          player = controller.exoPlayer
        }
      },
      update = { view ->
        view.resizeMode = if (resizeZoom) {
          AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
          AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    if (controller.isBuffering && controller.playerErrorMessage == null) {
      CircularProgressIndicator(
        color = ZenixCrimson,
        strokeWidth = 3.dp,
        modifier = Modifier
          .size(48.dp)
          .align(Alignment.Center),
      )
    }

    if (controller.playerErrorMessage != null) {
      Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ZenixCrimson.copy(alpha = 0.7f)),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(24.dp),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Stream Interrupted",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
          Text(
            text = controller.playerErrorMessage ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenixTextSecondary,
          )
          Button(
            onClick = { controller.retryCurrent() },
            colors = ButtonDefaults.buttonColors(containerColor = ZenixCrimson),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry Stream")
          }
        }
      }
    }

    // Skip Intro Floating Pill
    AnimatedVisibility(
      visible = showSkipIntro && activeEpisode != null && !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .padding(end = 24.dp, bottom = 104.dp),
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            activeEpisode?.let { controller.seekTo(it.introEndMs) }
          }
          .testTag("skip_intro_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Text(
            text = "Skip Intro",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
        }
      }
    }

    // Up Next Episode Countdown Card
    AnimatedVisibility(
      visible = showUpNext && nextEpisode != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .padding(end = 20.dp, bottom = 108.dp),
    ) {
      nextEpisode?.let { nextEp ->
        Surface(
          color = ZenixSurfaceElevated.copy(alpha = 0.94f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, ZenixCrimson.copy(alpha = 0.7f)),
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
              onSaveProgress(content.id, activeEpisodeId, controller.durationMs, controller.durationMs)
              activeEpisodeId = nextEp.episodeId
            }
            .testTag("next_episode_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.SkipNext, contentDescription = null, tint = ZenixCrimson)
            Column {
              Text(
                text = if (autoplayNextEnabled) "Next Episode in ${nextEpCountdown}s" else "Up Next",
                style = MaterialTheme.typography.labelSmall,
                color = ZenixEmeraldMatch,
              )
              Text(
                text = nextEp.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
            }
          }
        }
      }
    }

    // Custom Cinema HUD Overlay
    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Black.copy(alpha = 0.72f),
                Color.Black.copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.85f),
              )
            )
          )
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            if (!isLocked) {
              IconButton(
                onClick = saveAndExit,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = Color.White,
                )
              }
            }
            Column {
              Text(
                text = content.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (activeEpisode != null) {
                Text(
                  text = activeEpisode.title,
                  style = MaterialTheme.typography.bodyMedium,
                  color = ZenixTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            QualityChip(text = "${streamProtocol.label} • ${controller.videoQualityLabel}", highlight = true)
            IconButton(
              onClick = { isLocked = !isLocked },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("player_lock_button"),
            ) {
              Icon(
                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (isLocked) "Unlock controls" else "Lock controls",
                tint = if (isLocked) ZenixCrimson else Color.White,
              )
            }
          }
        }

        // Center Transport Controls
        if (!isLocked) {
          Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { controller.seekBy(-10_000L) },
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag("player_rewind_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = "Rewind 10 seconds",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
              )
            }

            IconButton(
              onClick = { controller.togglePlayPause() },
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ZenixCrimson.copy(alpha = 0.9f))
                .testTag("player_play_pause_button"),
            ) {
              Icon(
                imageVector = if (controller.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (controller.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
              )
            }

            IconButton(
              onClick = { controller.seekBy(10_000L) },
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag("player_forward_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = "Forward 10 seconds",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }

        // Bottom Seekbar & Settings Row
        if (!isLocked) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            val dur = controller.durationMs.coerceAtLeast(1L)
            val progressFraction = if (isDraggingSlider) {
              sliderDragFraction
            } else {
              (controller.currentPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            val displayPosMs = if (isDraggingSlider) {
              (sliderDragFraction * dur).toLong()
            } else {
              controller.currentPositionMs
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = formatTimecode(displayPosMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
              Slider(
                value = progressFraction,
                onValueChange = { frac ->
                  isDraggingSlider = true
                  sliderDragFraction = frac
                },
                onValueChangeFinished = {
                  controller.seekTo((sliderDragFraction * dur).toLong())
                  isDraggingSlider = false
                },
                colors = SliderDefaults.colors(
                  thumbColor = ZenixCrimson,
                  activeTrackColor = ZenixCrimson,
                  inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.weight(1f).testTag("player_seek_slider"),
              )
              Text(
                text = formatTimecode(controller.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = ZenixTextSecondary,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box {
                Surface(
                  color = Color.White.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSpeedMenu = true }
                    .testTag("player_speed_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                      text = "${controller.playbackSpeed}x",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }
                }
                DropdownMenu(
                  expanded = showSpeedMenu,
                  onDismissRequest = { showSpeedMenu = false },
                ) {
                  listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    DropdownMenuItem(
                      text = { Text("${speed}x") },
                      onClick = {
                        controller.setSpeed(speed)
                        showSpeedMenu = false
                      },
                    )
                  }
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                  color = Color.White.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { resizeZoom = !resizeZoom }
                    .testTag("player_aspect_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                      text = if (resizeZoom) "Fill" else "Fit",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White,
                    )
                  }
                }

                if (nextEpisode != null) {
                  Surface(
                    color = ZenixCrimson.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        onSaveProgress(content.id, activeEpisodeId, controller.durationMs, controller.durationMs)
                        activeEpisodeId = nextEpisode.episodeId
                      },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                      Text("Next Ep", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
