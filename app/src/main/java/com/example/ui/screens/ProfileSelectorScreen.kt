package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.UiState
import com.example.domain.model.MaturityLevel
import com.example.domain.model.ViewerProfile
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.ProfileEditorSheet
import com.example.ui.components.ZenixWordmark
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary

@Composable
fun ProfileSelectorScreen(
  profilesState: UiState<List<ViewerProfile>>,
  onSelectProfile: (ViewerProfile) -> Unit,
  onCreateProfile: (String, String, MaturityLevel, List<String>, Boolean) -> Unit,
  onUpdateProfile: (String, String, String, MaturityLevel, List<String>, Boolean) -> Unit,
  onDeleteProfile: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var isManageMode by remember { mutableStateOf(false) }
  var showCreateSheet by remember { mutableStateOf(false) }
  var editingProfile by remember { mutableStateOf<ViewerProfile?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ZenixWordmark(fontSizeSp = 22)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
            onClick = { isManageMode = !isManageMode },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("manage_profiles_toggle"),
          ) {
            Text(
              text = if (isManageMode) "Done" else "Edit",
              color = ZenixTextPrimary,
            )
          }
          TextButton(
            onClick = onSignOut,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_gate_sign_out"),
          ) {
            Text(text = stringResource(R.string.sign_out), color = ZenixTextSecondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = if (isManageMode) stringResource(R.string.manage_profiles) else stringResource(R.string.whos_watching),
        style = MaterialTheme.typography.headlineLarge,
        color = ZenixTextPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = if (isManageMode) "Tap a profile to customize genres, avatar, or Kids mode"
        else "Each profile keeps its own watchlist, resume state & AI picks",
        style = MaterialTheme.typography.bodyMedium,
        color = ZenixTextSecondary,
      )

      Spacer(modifier = Modifier.height(28.dp))

      when (profilesState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZenixCrimson)
          }
        }

        is UiState.Error -> {
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(text = profilesState.message, color = Color(0xFFFF8888))
          }
        }

        is UiState.Success -> {
          val profiles = profilesState.data
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
              .weight(1f)
              .widthIn(max = 560.dp),
          ) {
            items(profiles, key = { it.id }) { profile ->
              val isKids = MaturityLevel.fromRaw(profile.maturityLevel) == MaturityLevel.KIDS
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .clickable {
                    if (isManageMode) {
                      editingProfile = profile
                    } else {
                      onSelectProfile(profile)
                    }
                  }
                  .padding(8.dp)
                  .testTag("profile_card_${profile.id}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  ProfileAvatarBadge(
                    avatarId = profile.avatarId,
                    size = 96.dp,
                    isKids = isKids,
                  )
                  if (isManageMode) {
                    Box(
                      modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${profile.name}",
                            tint = Color.White,
                          )
                        }
                      }
                    }
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = profile.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = ZenixTextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (isKids) "Kids Safe" else profile.preferredGenres.take(2).joinToString(" • ").ifBlank { "All Access" },
                  style = MaterialTheme.typography.labelSmall,
                  color = ZenixTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            if (profiles.size < 5) {
              item(key = "add_profile") {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showCreateSheet = true }
                    .padding(8.dp)
                    .testTag("add_profile_button"),
                ) {
                  Surface(
                    color = ZenixSurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ZenixOutline),
                    modifier = Modifier.size(96.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_profile),
                        tint = ZenixTextPrimary,
                        modifier = Modifier.size(36.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = stringResource(R.string.add_profile),
                    style = MaterialTheme.typography.titleMedium,
                    color = ZenixTextSecondary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateSheet) {
    ProfileEditorSheet(
      existingProfile = null,
      canDelete = false,
      onDismiss = { showCreateSheet = false },
      onSave = { name, avatarId, maturity, genres, autoplay ->
        onCreateProfile(name, avatarId, maturity, genres, autoplay)
        showCreateSheet = false
      },
    )
  }

  val targetEdit = editingProfile
  if (targetEdit != null) {
    val profileCount = (profilesState as? UiState.Success)?.data?.size ?: 1
    ProfileEditorSheet(
      existingProfile = targetEdit,
      canDelete = profileCount > 1,
      onDismiss = { editingProfile = null },
      onSave = { name, avatarId, maturity, genres, autoplay ->
        onUpdateProfile(targetEdit.id, name, avatarId, maturity, genres, autoplay)
        editingProfile = null
      },
      onDelete = {
        onDeleteProfile(targetEdit.id)
        editingProfile = null
      },
    )
  }
}
