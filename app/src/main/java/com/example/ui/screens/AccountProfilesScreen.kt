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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.MaturityLevel
import com.example.domain.model.ViewerProfile
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.ProfileEditorSheet
import com.example.ui.components.QualityChip
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixEmeraldMatch
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountProfilesScreen(
  user: FirebaseUser?,
  profiles: List<ViewerProfile>,
  activeProfile: ViewerProfile,
  onSwitchProfile: (ViewerProfile) -> Unit,
  onCreateProfile: (String, String, MaturityLevel, List<String>, Boolean) -> Unit,
  onUpdateProfile: (String, String, String, MaturityLevel, List<String>, Boolean) -> Unit,
  onDeleteProfile: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var showCreateSheet by remember { mutableStateOf(false) }
  var editingProfile by remember { mutableStateOf<ViewerProfile?>(null) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian)
      .statusBarsPadding(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "header") {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Profiles & Account",
          style = MaterialTheme.typography.headlineLarge,
          color = ZenixTextPrimary,
        )
        Text(
          text = user?.email ?: user?.displayName ?: "Signed in with Google",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenixTextSecondary,
        )
      }
    }

    item(key = "active_card") {
      Surface(
        color = ZenixSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ZenixCrimson.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          ProfileAvatarBadge(
            avatarId = activeProfile.avatarId,
            size = 64.dp,
            isSelected = true,
            isKids = MaturityLevel.fromRaw(activeProfile.maturityLevel) == MaturityLevel.KIDS,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Active: ${activeProfile.name}",
              style = MaterialTheme.typography.titleLarge,
              color = ZenixTextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              QualityChip(
                text = MaturityLevel.fromRaw(activeProfile.maturityLevel).name,
                highlight = activeProfile.maturityLevel == "KIDS",
              )
              if (activeProfile.autoplayNext) {
                QualityChip(text = "Autoplay On")
              }
            }
          }
          IconButton(
            onClick = { editingProfile = activeProfile },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Edit active profile",
              tint = ZenixTextPrimary,
            )
          }
        }
      }
    }

    item(key = "all_profiles_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Switch or Manage Profiles",
          style = MaterialTheme.typography.titleLarge,
          color = ZenixTextPrimary,
        )
        if (profiles.size < 5) {
          OutlinedButton(
            onClick = { showCreateSheet = true },
            border = BorderStroke(1.dp, ZenixOutline),
            modifier = Modifier.height(40.dp).testTag("account_add_profile_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New", color = ZenixTextPrimary)
          }
        }
      }
    }

    items(profiles, key = { it.id }) { profile ->
      val isActive = profile.id == activeProfile.id
      val isKids = MaturityLevel.fromRaw(profile.maturityLevel) == MaturityLevel.KIDS
      Surface(
        color = ZenixSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isActive) ZenixEmeraldMatch.copy(alpha = 0.7f) else ZenixOutline),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 680.dp)
          .clip(RoundedCornerShape(14.dp))
          .clickable { onSwitchProfile(profile) }
          .testTag("switch_profile_${profile.id}"),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ProfileAvatarBadge(
            avatarId = profile.avatarId,
            size = 48.dp,
            isSelected = isActive,
            isKids = isKids,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = profile.name,
              style = MaterialTheme.typography.titleMedium,
              color = ZenixTextPrimary,
            )
            Text(
              text = profile.preferredGenres.joinToString(", ").ifBlank { "All Genres" },
              style = MaterialTheme.typography.bodyMedium,
              color = ZenixTextSecondary,
            )
          }
          if (isActive) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Active profile",
              tint = ZenixEmeraldMatch,
            )
          }
          IconButton(
            onClick = { editingProfile = profile },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Edit ${profile.name}",
              tint = ZenixTextSecondary,
            )
          }
        }
      }
    }

    item(key = "sign_out_section") {
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = onSignOut,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF2B1319),
          contentColor = Color(0xFFFFB4B4),
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 680.dp)
          .height(50.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.sign_out))
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
    ProfileEditorSheet(
      existingProfile = targetEdit,
      canDelete = profiles.size > 1,
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
