package com.virex.music.downloads

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.virex.music.domain.model.Track
import java.util.concurrent.TimeUnit

object DownloadScheduler {
 fun enqueue(context: Context, track: Track, highQuality: Boolean) {
  val input = Data.Builder()
   .putString(DownloadWorker.KEY_ID, track.id)
   .putString(DownloadWorker.KEY_URL, track.streamUrl)
   .putString(DownloadWorker.KEY_TITLE, track.title)
   .putString(DownloadWorker.KEY_ARTIST, track.artist)
   .putString(DownloadWorker.KEY_ALBUM, track.album)
   .putString(DownloadWorker.KEY_ARTWORK, track.artworkUrl)
   .putLong(DownloadWorker.KEY_DURATION, track.durationMs)
   .putBoolean(DownloadWorker.KEY_HIGH_QUALITY, highQuality)
   .build()
  val request = OneTimeWorkRequestBuilder<DownloadWorker>()
   .setInputData(input)
   .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
   .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
   .build()
  WorkManager.getInstance(context).enqueueUniqueWork("download-${track.id}", ExistingWorkPolicy.KEEP, request)
 }
}
