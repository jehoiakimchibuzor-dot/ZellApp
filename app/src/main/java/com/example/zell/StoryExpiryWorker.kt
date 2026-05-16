package com.example.zell

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * StoryExpiryWorker — runs in the background every 6 hours.
 *
 * What it does:
 * - Looks at the current user's stories in Firestore
 * - Deletes any story that was posted more than 24 hours ago
 * - This keeps the database clean so old stories don't pile up forever
 *
 * Think of it like Instagram's auto-delete for stories — it just happens
 * silently in the background while the app is open or even closed.
 */
class StoryExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success() // not logged in, nothing to do

        return try {
            val cutoffTime = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS

            // Fetch the user's story list from Firestore
            val userDoc = db.collection("users").document(uid).get().await()
            val stories = userDoc.get("stories") as? List<Map<String, Any>> ?: emptyList()

            // Find which stories are expired
            val expiredStoryIds = stories.mapNotNull { story ->
                val timestamp = story["timestamp"]
                val storyTime = when (timestamp) {
                    is com.google.firebase.Timestamp -> timestamp.toDate().time
                    is Long -> timestamp
                    else -> null
                }
                val id = story["id"] as? String
                if (storyTime != null && storyTime < cutoffTime) id else null
            }

            if (expiredStoryIds.isNotEmpty()) {
                // Remove expired stories from the user's stories array
                val freshStories = stories.filter { story ->
                    val id = story["id"] as? String
                    id !in expiredStoryIds
                }

                db.collection("users").document(uid)
                    .update("stories", freshStories)
                    .await()

                CrashlyticsLogger.i("StoryExpiryWorker", "Deleted ${expiredStoryIds.size} expired stories for user $uid")
            }

            Result.success()
        } catch (e: Exception) {
            CrashlyticsLogger.e("StoryExpiryWorker", "Failed to clean up expired stories", e)
            // Retry later if it failed (WorkManager will try again automatically)
            Result.retry()
        }
    }

    companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
        private const val WORK_NAME = "story_expiry_cleanup"

        /**
         * Call this once when the app starts (in MainActivity).
         * WorkManager makes sure only one copy runs — calling this multiple times is safe.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StoryExpiryWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // don't restart if already scheduled
                request
            )
        }
    }
}

/**
 * A simple helper you can call anywhere to filter out expired stories
 * before showing them on screen — just an extra safety net on top of
 * the background cleanup.
 *
 * Usage:
 *   val validStories = user.stories.filterNotExpired()
 */
fun List<StoryItem>.filterNotExpired(): List<StoryItem> {
    val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
    return filter { story ->
        val time = story.timestamp?.time ?: Long.MAX_VALUE
        time > cutoff  // keep it if it's NEWER than the cutoff
    }
}
