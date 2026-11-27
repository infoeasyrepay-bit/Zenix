package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.components.ZenixWordmark
import com.example.ui.theme.ZenixCrimson
import com.example.ui.theme.ZenixObsidian
import com.example.ui.theme.ZenixOutline
import com.example.ui.theme.ZenixSurfaceElevated
import com.example.ui.theme.ZenixTextPrimary
import com.example.ui.theme.ZenixTextSecondary
import com.example.ui.theme.ZenixVioletAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ZenixObsidian),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_solaris_1795462937833),
      contentDescription = "Zenix cinema showcase",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Black.copy(alpha = 0.55f),
              ZenixObsidian.copy(alpha = 0.72f),
              ZenixObsidian.copy(alpha = 0.96f),
              ZenixObsidian,
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ZenixWordmark(fontSizeSp = 24)
        Surface(
          color = Color.White.copy(alpha = 0.1f),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        ) {
          Text(
            text = "4K STREAMING",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Text(
          text = "Stream Originals,\nCurated For You.",
          style = MaterialTheme.typography.displayLarge,
          color = ZenixTextPrimary,
        )
        Text(
          text = stringResource(R.string.brand_tagline),
          style = MaterialTheme.typography.bodyLarge,
          color = ZenixTextSecondary,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FeatureHighlightPill(Icons.Filled.PlayCircle, "Adaptive HLS & 4K", ZenixCrimson)
          FeatureHighlightPill(Icons.Filled.Groups, "Multi-Profile Sync", Color(0xFF22D3EE))
          FeatureHighlightPill(Icons.Filled.AutoAwesome, "Gemini AI Engine", ZenixVioletAccent)
        }

        if (errorMessage != null) {
          Surface(
            color = Color(0xFF3A1216),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ZenixCrimson.copy(alpha = 0.6f)),
          ) {
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFFFD1D1),
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
              },
            )
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ZenixCrimson,
            contentColor = Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(22.dp),
            )
          } else {
            Text(
              text = stringResource(R.string.sign_in_with_google),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }

        Text(
          text = "Your viewer profiles, watch progress, and personalized recommendations sync securely across sessions.",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenixTextSecondary.copy(alpha = 0.78f),
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightPill(
  icon: ImageVector,
  label: String,
  tint: Color,
) {
  Surface(
    color = ZenixSurfaceElevated.copy(alpha = 0.86f),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, ZenixOutline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = ZenixTextPrimary,
      )
    }
  }
}
