package com.example.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StreamProtocol(val label: String) {
  HLS("HLS Adaptive"),
  MP4("HD Direct"),
}

enum class RatingSentiment {
  LIKE,
  DISLIKE;

  companion object {
    fun fromRaw(raw: String?): RatingSentiment? =
      entries.firstOrNull { it.name == raw }
  }
}

enum class MaturityLevel(val label: String) {
  ALL("All Maturity Ratings"),
  KIDS("Kids Safe (G / TV-Y)");

  companion object {
    fun fromRaw(raw: String?): MaturityLevel =
      entries.firstOrNull { it.name == raw } ?: ALL
  }
}

data class Episode(
  val episodeId: String,
  val seasonNumber: Int,
  val episodeNumber: Int,
  val title: String,
  val synopsis: String,
  val durationMinutes: Int,
  val videoUrl: String,
  val streamProtocol: StreamProtocol,
  val introStartMs: Long = 4_000L,
  val introEndMs: Long = 18_000L,
)

data class MediaContent(
  val id: String,
  val title: String,
  val tagline: String,
  val synopsis: String,
  val primaryGenre: String,
  val tags: List<String>,
  val releaseYear: Int,
  val maturityRating: String,
  val isKidsSafe: Boolean,
  val isSeries: Boolean,
  val isZenixOriginal: Boolean,
  val durationLabel: String,
  val durationMinutes: Int,
  val qualityBadge: String,
  val audioBadge: String,
  val videoUrl: String,
  val streamProtocol: StreamProtocol,
  val posterUrl: String? = null,
  @DrawableRes val localPosterRes: Int? = null,
  @DrawableRes val localBackdropRes: Int? = null,
  val accentColor: Color,
  val cast: List<String>,
  val director: String,
  val baseScore: Int,
  val episodes: List<Episode> = emptyList(),
)

data class RecommendedPick(
  val content: MediaContent,
  val matchPercent: Int,
  val reason: String,
  val aiCurated: Boolean = false,
  val moodTag: String = "",
)

data class ProfileAvatarSpec(
  val id: String,
  val label: String,
  val startColor: Color,
  val endColor: Color,
)

object ZenixAvatars {
  val presets = listOf(
    ProfileAvatarSpec("crimson_fox", "Crimson Noir", Color(0xFFE50914), Color(0xFF7A091A)),
    ProfileAvatarSpec("cyber_visor", "Neon Pulse", Color(0xFF22D3EE), Color(0xFF2563EB)),
    ProfileAvatarSpec("aurora_star", "Violet Horizon", Color(0xFFA855F7), Color(0xFF4C1D95)),
    ProfileAvatarSpec("amber_cine", "Solaris Gold", Color(0xFFF59E0B), Color(0xFFB45309)),
    ProfileAvatarSpec("kids_sprout", "Kids Explorer", Color(0xFF10B981), Color(0xFF047857)),
  )

  fun find(avatarId: String): ProfileAvatarSpec =
    presets.firstOrNull { it.id == avatarId } ?: presets.first()
}

@Keep
@IgnoreExtraProperties
data class ViewerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val avatarId: String = "crimson_fox",
  val maturityLevel: String = "ALL",
  val preferredGenres: List<String> = emptyList(),
  val autoplayNext: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WatchlistItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val profileId: String = "",
  val contentId: String = "",
  val title: String = "",
  val genre: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WatchHistoryEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val profileId: String = "",
  val contentId: String = "",
  val title: String = "",
  val genre: String = "",
  val episodeId: String = "",
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val progressPercent: Int = 0,
  val completed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TitleRating(
  @DocumentId val id: String = "",
  val userId: String = "",
  val profileId: String = "",
  val contentId: String = "",
  val sentiment: String = "LIKE",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
