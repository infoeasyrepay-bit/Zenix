package com.example.ui.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Stable
class ZenixPlayerController(context: Context) {

  val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
    .setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build(),
      true,
    )
    .setHandleAudioBecomingNoisy(true)
    .build()

  var isPlaying by mutableStateOf(false)
    private set
  var isBuffering by mutableStateOf(true)
    private set
  var hasEnded by mutableStateOf(false)
    private set
  var currentPositionMs by mutableLongStateOf(0L)
    private set
  var durationMs by mutableLongStateOf(0L)
    private set
  var bufferedPositionMs by mutableLongStateOf(0L)
    private set
  var playbackSpeed by mutableFloatStateOf(1.0f)
    private set
  var videoQualityLabel by mutableStateOf("Adaptive")
    private set
  var playerErrorMessage by mutableStateOf<String?>(null)
    private set

  private var currentUrl: String? = null

  private val listener = object : Player.Listener {
    override fun onIsPlayingChanged(playing: Boolean) {
      isPlaying = playing
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
      isBuffering = playbackState == Player.STATE_BUFFERING
      hasEnded = playbackState == Player.STATE_ENDED
      if (exoPlayer.duration != C.TIME_UNSET && exoPlayer.duration > 0) {
        durationMs = exoPlayer.duration
      }
    }

    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
      if (videoSize.height > 0) {
        val tier = when {
          videoSize.height >= 2160 -> "4K UHD"
          videoSize.height >= 1440 -> "1440p QHD"
          videoSize.height >= 1080 -> "1080p FHD"
          videoSize.height >= 720 -> "720p HD"
          else -> "${videoSize.height}p"
        }
        videoQualityLabel = tier
      }
    }

    override fun onPlayerError(error: PlaybackException) {
      isBuffering = false
      playerErrorMessage = error.localizedMessage ?: "Stream playback interrupted (${error.errorCodeName})."
    }
  }

  init {
    exoPlayer.addListener(listener)
  }

  fun prepareMedia(url: String, startPositionMs: Long = 0L) {
    if (currentUrl == url && playerErrorMessage == null) return
    currentUrl = url
    playerErrorMessage = null
    hasEnded = false
    isBuffering = true
    val mediaItem = MediaItem.fromUri(Uri.parse(url))
    exoPlayer.setMediaItem(mediaItem)
    if (startPositionMs > 2_000L) {
      exoPlayer.seekTo(startPositionMs)
    }
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
  }

  fun retryCurrent() {
    val url = currentUrl ?: return
    val pos = currentPositionMs
    currentUrl = null
    prepareMedia(url, pos)
  }

  fun pollProgress() {
    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
    val dur = if (exoPlayer.duration != C.TIME_UNSET && exoPlayer.duration > 0) exoPlayer.duration else 0L
    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
    currentPositionMs = pos
    if (dur > 0L) durationMs = dur
    bufferedPositionMs = buf
  }

  fun togglePlayPause() {
    if (hasEnded) {
      exoPlayer.seekTo(0L)
      exoPlayer.play()
    } else if (exoPlayer.isPlaying) {
      exoPlayer.pause()
    } else {
      exoPlayer.play()
    }
  }

  fun seekTo(positionMs: Long) {
    val clamped = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    exoPlayer.seekTo(clamped)
    currentPositionMs = clamped
  }

  fun seekBy(deltaMs: Long) {
    seekTo(exoPlayer.currentPosition + deltaMs)
  }

  fun setSpeed(speed: Float) {
    playbackSpeed = speed
    exoPlayer.playbackParameters = PlaybackParameters(speed)
  }

  fun pause() {
    exoPlayer.pause()
  }

  fun release() {
    exoPlayer.removeListener(listener)
    exoPlayer.release()
  }
}

fun formatTimecode(ms: Long): String {
  val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}
