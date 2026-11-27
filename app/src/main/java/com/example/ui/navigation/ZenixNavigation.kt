package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface ZenixRoute {
  @Serializable data object ProfileSelector : ZenixRoute
  @Serializable data object Home : ZenixRoute
  @Serializable data object ForYouAi : ZenixRoute
  @Serializable data object Explore : ZenixRoute
  @Serializable data object AccountProfiles : ZenixRoute
  @Serializable data class Player(val contentId: String, val episodeId: String = "") : ZenixRoute
}
