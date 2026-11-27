package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.domain.model.MaturityLevel
import com.example.domain.model.MediaContent
import com.example.domain.model.RatingSentiment
import com.example.domain.model.TitleRating
import com.example.domain.model.ViewerProfile
import com.example.domain.model.WatchHistoryEntry
import com.example.domain.model.WatchlistItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ZenixRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun sanitizeId(raw: String): String {
    val clean = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    return clean.take(128).ifBlank { UUID.randomUUID().toString().replace("-", "") }
  }

  suspend fun ensureUserAccount(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val currentUid = requireUserId()
      check(currentUid == uid) { "Authenticated user mismatch." }
      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      val displayName = (user.displayName?.trim().takeIf { !it.isNullOrEmpty() } ?: "Zenix Viewer").take(80)
      val email = user.email?.trim()?.take(160)
      val photoUrl = user.photoUrl?.toString()?.take(500)

      if (!existing.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to displayName,
          "email" to email,
          "photoUrl" to photoUrl,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
        docRef.set(payload).await()
      } else {
        val updatePayload = mapOf(
          "displayName" to displayName,
          "email" to email,
          "photoUrl" to photoUrl,
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
        docRef.update(updatePayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeProfiles(userId: String): Flow<List<ViewerProfile>> = flow {
    val path = "users/$userId/profiles"
    emitAll(
      db.collection("users").document(userId).collection("profiles")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(ViewerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createProfile(
    userId: String,
    name: String,
    avatarId: String,
    maturityLevel: MaturityLevel,
    preferredGenres: List<String>,
    autoplayNext: Boolean,
  ): Result<String> {
    val profileId = "prof_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val path = "users/$userId/profiles/$profileId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user write rejected." }
      val cleanName = name.trim().take(24)
      require(cleanName.isNotEmpty()) { "Profile name cannot be empty." }
      val genres = preferredGenres.distinct().take(8)
      val payload = mapOf(
        "userId" to uid,
        "name" to cleanName,
        "avatarId" to avatarId.take(32).ifBlank { "crimson_fox" },
        "maturityLevel" to maturityLevel.name,
        "preferredGenres" to genres,
        "autoplayNext" to autoplayNext,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("users").document(uid).collection("profiles").document(profileId)
        .set(payload)
        .await()
      Result.success(profileId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    userId: String,
    profileId: String,
    name: String,
    avatarId: String,
    maturityLevel: MaturityLevel,
    preferredGenres: List<String>,
    autoplayNext: Boolean,
  ): Result<Unit> {
    val path = "users/$userId/profiles/$profileId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user update rejected." }
      val cleanId = sanitizeId(profileId)
      val cleanName = name.trim().take(24)
      require(cleanName.isNotEmpty()) { "Profile name cannot be empty." }
      val genres = preferredGenres.distinct().take(8)
      db.collection("users").document(uid).collection("profiles").document(cleanId)
        .update(
          mapOf(
            "name" to cleanName,
            "avatarId" to avatarId.take(32).ifBlank { "crimson_fox" },
            "maturityLevel" to maturityLevel.name,
            "preferredGenres" to genres,
            "autoplayNext" to autoplayNext,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteProfile(userId: String, profileId: String): Result<Unit> {
    val path = "users/$userId/profiles/$profileId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user delete rejected." }
      val cleanId = sanitizeId(profileId)
      db.collection("users").document(uid).collection("profiles").document(cleanId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  fun observeWatchlist(userId: String, profileId: String): Flow<List<WatchlistItem>> = flow {
    val cleanProfileId = sanitizeId(profileId)
    val path = "users/$userId/profiles/$cleanProfileId/watchlist"
    emitAll(
      db.collection("users").document(userId)
        .collection("profiles").document(cleanProfileId)
        .collection("watchlist")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(WatchlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun toggleWatchlist(
    userId: String,
    profileId: String,
    content: MediaContent,
    currentlyInList: Boolean,
  ): Result<Boolean> {
    val cleanProfileId = sanitizeId(profileId)
    val cleanContentId = sanitizeId(content.id)
    val path = "users/$userId/profiles/$cleanProfileId/watchlist/$cleanContentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user watchlist write rejected." }
      val docRef = db.collection("users").document(uid)
        .collection("profiles").document(cleanProfileId)
        .collection("watchlist").document(cleanContentId)
      if (currentlyInList) {
        docRef.delete().await()
        Result.success(false)
      } else {
        val payload = mapOf(
          "userId" to uid,
          "profileId" to cleanProfileId,
          "contentId" to cleanContentId,
          "title" to content.title.take(120),
          "genre" to content.primaryGenre.take(40),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        Result.success(true)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeWatchHistory(userId: String, profileId: String): Flow<List<WatchHistoryEntry>> = flow {
    val cleanProfileId = sanitizeId(profileId)
    val path = "users/$userId/profiles/$cleanProfileId/watchHistory"
    emitAll(
      db.collection("users").document(userId)
        .collection("profiles").document(cleanProfileId)
        .collection("watchHistory")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(WatchHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun savePlaybackProgress(
    userId: String,
    profileId: String,
    content: MediaContent,
    episodeId: String,
    positionMs: Long,
    durationMs: Long,
  ): Result<Unit> {
    val cleanProfileId = sanitizeId(profileId)
    val cleanContentId = sanitizeId(content.id)
    val path = "users/$userId/profiles/$cleanProfileId/watchHistory/$cleanContentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user progress write rejected." }
      val safePos = positionMs.coerceAtLeast(0L)
      val safeDur = durationMs.coerceAtLeast(0L)
      val percent = if (safeDur > 0L) ((safePos * 100L) / safeDur).toInt().coerceIn(0, 100) else 0
      val isFinished = percent >= 95
      val safeEpisodeId = episodeId.take(40)

      val docRef = db.collection("users").document(uid)
        .collection("profiles").document(cleanProfileId)
        .collection("watchHistory").document(cleanContentId)
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "profileId" to cleanProfileId,
            "contentId" to cleanContentId,
            "title" to content.title.take(120),
            "genre" to content.primaryGenre.take(40),
            "episodeId" to safeEpisodeId,
            "positionMs" to safePos,
            "durationMs" to safeDur,
            "progressPercent" to percent,
            "completed" to isFinished,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "title" to content.title.take(120),
            "genre" to content.primaryGenre.take(40),
            "episodeId" to safeEpisodeId,
            "positionMs" to safePos,
            "durationMs" to safeDur,
            "progressPercent" to percent,
            "completed" to isFinished,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeRatings(userId: String, profileId: String): Flow<List<TitleRating>> = flow {
    val cleanProfileId = sanitizeId(profileId)
    val path = "users/$userId/profiles/$cleanProfileId/ratings"
    emitAll(
      db.collection("users").document(userId)
        .collection("profiles").document(cleanProfileId)
        .collection("ratings")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(TitleRating::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun setRating(
    userId: String,
    profileId: String,
    contentId: String,
    sentiment: RatingSentiment?,
  ): Result<Unit> {
    val cleanProfileId = sanitizeId(profileId)
    val cleanContentId = sanitizeId(contentId)
    val path = "users/$userId/profiles/$cleanProfileId/ratings/$cleanContentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cross-user rating write rejected." }
      val docRef = db.collection("users").document(uid)
        .collection("profiles").document(cleanProfileId)
        .collection("ratings").document(cleanContentId)
      if (sentiment == null) {
        docRef.delete().await()
      } else {
        val existing = docRef.get().await()
        if (!existing.exists()) {
          docRef.set(
            mapOf(
              "userId" to uid,
              "profileId" to cleanProfileId,
              "contentId" to cleanContentId,
              "sentiment" to sentiment.name,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        } else {
          docRef.update(
            mapOf(
              "sentiment" to sentiment.name,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }
}
