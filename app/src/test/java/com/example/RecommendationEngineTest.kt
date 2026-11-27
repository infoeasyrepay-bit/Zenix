package com.example

import com.example.data.ZenixCatalog
import com.example.domain.LocalRecommendationEngine
import com.example.domain.model.MaturityLevel
import com.example.domain.model.RatingSentiment
import com.example.domain.model.TitleRating
import com.example.domain.model.ViewerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

  @Test
  fun catalogIdsAreUniqueAndStreamsAreHttps() {
    val ids = ZenixCatalog.titles.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
    assertTrue(ZenixCatalog.titles.all { it.videoUrl.startsWith("https://") })
  }

  @Test
  fun kidsProfileFiltersOutMatureTitles() {
    val kidsProfile = ViewerProfile(
      id = "kids_1",
      userId = "u1",
      name = "Kids",
      maturityLevel = MaturityLevel.KIDS.name,
      preferredGenres = listOf("Animation"),
    )
    val picks = LocalRecommendationEngine.computeRecommendations(
      profile = kidsProfile,
      history = emptyList(),
      watchlist = emptyList(),
      ratings = emptyList(),
    )
    assertTrue(picks.isNotEmpty())
    assertTrue(picks.all { it.content.isKidsSafe })
  }

  @Test
  fun likedAndPreferredGenresOutrankDislikedTitles() {
    val profile = ViewerProfile(
      id = "main_1",
      userId = "u1",
      name = "Alex",
      maturityLevel = MaturityLevel.ALL.name,
      preferredGenres = listOf("Sci-Fi"),
    )
    val ratings = listOf(
      TitleRating(
        id = "solaris_drift",
        userId = "u1",
        profileId = "main_1",
        contentId = "solaris_drift",
        sentiment = RatingSentiment.LIKE.name,
      ),
      TitleRating(
        id = "velvet_horizon",
        userId = "u1",
        profileId = "main_1",
        contentId = "velvet_horizon",
        sentiment = RatingSentiment.DISLIKE.name,
      ),
    )
    val picks = LocalRecommendationEngine.computeRecommendations(
      profile = profile,
      history = emptyList(),
      watchlist = emptyList(),
      ratings = ratings,
    )
    val solarisScore = picks.first { it.content.id == "solaris_drift" }.matchPercent
    val velvetScore = picks.first { it.content.id == "velvet_horizon" }.matchPercent
    assertTrue(solarisScore > velvetScore)
  }
}
