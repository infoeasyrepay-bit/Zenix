package com.example.domain

import android.util.Log
import com.example.data.ZenixCatalog
import com.example.domain.model.MaturityLevel
import com.example.domain.model.MediaContent
import com.example.domain.model.RatingSentiment
import com.example.domain.model.RecommendedPick
import com.example.domain.model.TitleRating
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.WatchlistItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

object LocalRecommendationEngine {

  fun computeRecommendations(
    profile: ViewerProfile?,
    history: List<WatchHistoryEntry>,
    watchlist: List<WatchlistItem>,
    ratings: List<TitleRating>,
  ): List<RecommendedPick> {
    val isKids = MaturityLevel.fromRaw(profile?.maturityLevel) == MaturityLevel.KIDS
    val eligible = ZenixCatalog.filterForMaturity(isKids)

    val preferredGenres = profile?.preferredGenres?.map { it.lowercase() }?.toSet().orEmpty()
    val likedIds = ratings.filter { RatingSentiment.fromRaw(it.sentiment) == RatingSentiment.LIKE }
      .map { it.contentId }.toSet()
    val dislikedIds = ratings.filter { RatingSentiment.fromRaw(it.sentiment) == RatingSentiment.DISLIKE }
      .map { it.contentId }.toSet()
    val watchlistIds = watchlist.map { it.contentId }.toSet()

    val likedGenres = eligible.filter { it.id in likedIds }.map { it.primaryGenre.lowercase() }.toSet()
    val dislikedGenres = eligible.filter { it.id in dislikedIds }.map { it.primaryGenre.lowercase() }.toSet()
    val watchedGenres = history.filter { it.progressPercent >= 15 }.map { it.genre.lowercase() }.toSet()
    val watchlistGenres = watchlist.map { it.genre.lowercase() }.toSet()
    val watchedIds = history.map { it.contentId }.toSet()

    return eligible.map { item ->
      var score = (item.baseScore - 15).coerceIn(60, 82)
      val genreKey = item.primaryGenre.lowercase()
      val reasons = mutableListOf<String>()

      if (genreKey in preferredGenres) {
        score += 12
        reasons.add("Matches your ${item.primaryGenre} taste")
      }
      if (genreKey in likedGenres) {
        score += 10
        reasons.add("Similar to titles you liked")
      }
      if (genreKey in watchedGenres && item.id !in watchedIds) {
        score += 8
        reasons.add("Because you watch ${item.primaryGenre}")
      }
      if (genreKey in watchlistGenres || item.id in watchlistIds) {
        score += 6
      }
      if (item.id in likedIds) {
        score += 8
      }
      if (item.id in dislikedIds) {
        score -= 35
      } else if (genreKey in dislikedGenres) {
        score -= 14
      }
      if (item.isZenixOriginal) {
        score += 3
      }

      val finalMatch = score.coerceIn(62, 99)
      val reasonText = reasons.firstOrNull()
        ?: if (item.isZenixOriginal) "Zenix Original • Trending in ${item.primaryGenre}"
        else "Top rated in ${item.primaryGenre}"

      RecommendedPick(
        content = item,
        matchPercent = finalMatch,
        reason = reasonText,
        aiCurated = false,
        moodTag = item.tags.firstOrNull() ?: item.primaryGenre,
      )
    }.sortedByDescending { it.matchPercent }
  }
}

class AiRecommendationService {

  private val recommendationSchema = Schema.obj(
    properties = mapOf(
      "picks" to Schema.array(
        items = Schema.obj(
          properties = mapOf(
            "contentId" to Schema.string(description = "Exact ID from the provided Zenix catalog"),
            "matchPercent" to Schema.integer(description = "Match confidence between 75 and 99"),
            "moodTag" to Schema.string(description = "2-3 word atmospheric mood badge"),
            "reason" to Schema.string(description = "Concise 1-sentence personalized explanation referencing user taste"),
          )
        ),
        description = "Ordered list of 4 to 6 recommended titles from the catalog"
      )
    )
  )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = recommendationSchema
        temperature = 0.4f
      }
    )
  }

  suspend fun generatePersonalizedPicks(
    profile: ViewerProfile,
    history: List<WatchHistoryEntry>,
    watchlist: List<WatchlistItem>,
    ratings: List<TitleRating>,
    moodPrompt: String,
  ): List<RecommendedPick> {
    val isKids = MaturityLevel.fromRaw(profile.maturityLevel) == MaturityLevel.KIDS
    val eligible = ZenixCatalog.filterForMaturity(isKids)

    val catalogSummary = eligible.joinToString("\n") { item ->
      "- id=${item.id} | title=${item.title} | genre=${item.primaryGenre} | tags=${item.tags.joinToString(",")} | duration=${item.durationLabel} | synopsis=${item.synopsis}"
    }
    val likedTitles = ratings
      .filter { RatingSentiment.fromRaw(it.sentiment) == RatingSentiment.LIKE }
      .mapNotNull { ZenixCatalog.findById(it.contentId)?.title }
    val dislikedTitles = ratings
      .filter { RatingSentiment.fromRaw(it.sentiment) == RatingSentiment.DISLIKE }
      .mapNotNull { ZenixCatalog.findById(it.contentId)?.title }
    val watchedSummary = history.take(6).joinToString(", ") {
      "${it.title} (${it.progressPercent}% watched)"
    }.ifBlank { "None yet" }

    val prompt = """
      You are the Zenix streaming recommendation engine.
      Select 5 personalized titles strictly from the eligible catalog below.
      
      Viewer Profile:
      - Name: ${profile.name}
      - Maturity Mode: ${profile.maturityLevel}
      - Preferred Genres: ${profile.preferredGenres.joinToString(", ").ifBlank { "Open to discovery" }}
      - Watch History: $watchedSummary
      - Liked Titles: ${likedTitles.joinToString(", ").ifBlank { "None yet" }}
      - Disliked Titles: ${dislikedTitles.joinToString(", ").ifBlank { "None" }}
      - Saved in My List: ${watchlist.joinToString(", ") { it.title }.ifBlank { "Empty" }}
      - Viewer Mood Request: ${moodPrompt.ifBlank { "Surprise me with the best match for tonight" }}
      
      Eligible Catalog:
      $catalogSummary
      
      Rules:
      1. Every contentId MUST exactly match an id in the Eligible Catalog above.
      2. Never recommend disliked titles unless there are fewer than 5 eligible titles.
      3. Write crisp, cinematic, non-generic 1-sentence reasons referencing the viewer's actual taste or mood request.
    """.trimIndent()

    try {
      val response = withTimeout(60_000L) {
        model.generateContent(prompt)
      }
      val rawJson = response.text ?: throw IllegalStateException("AI recommendation engine returned an empty response.")
      val root = JSONObject(rawJson)
      val array = root.getJSONArray("picks")
      val results = mutableListOf<RecommendedPick>()
      val seenIds = mutableSetOf<String>()

      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val contentId = obj.optString("contentId")
        val content = eligible.firstOrNull { it.id == contentId } ?: continue
        if (!seenIds.add(content.id)) continue
        val match = obj.optInt("matchPercent", 90).coerceIn(75, 99)
        val moodTag = obj.optString("moodTag", content.primaryGenre).take(28)
        val reason = obj.optString("reason", "Selected for your ${content.primaryGenre} taste.")
        results.add(
          RecommendedPick(
            content = content,
            matchPercent = match,
            reason = reason,
            aiCurated = true,
            moodTag = moodTag,
          )
        )
      }

      if (results.isEmpty()) {
        throw IllegalStateException("No matching catalog recommendations were returned.")
      }
      return results
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI recommendation request timed out", e)
      throw IllegalStateException("Zenix AI recommendation request timed out. Please try again.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI recommendation generation failed", e)
      throw e
    }
  }

  private companion object {
    const val TAG = "ZenixAiRecommender"
  }
}
