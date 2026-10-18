package com.virex.music.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.virex.music.R
import com.virex.music.VirexApplication
import com.virex.music.domain.model.Track

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
 override suspend fun getForegroundInfo(): ForegroundInfo {
  val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Music downloads", NotificationManager.IMPORTANCE_LOW))
  val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
   .setSmallIcon(android.R.drawable.stat_sys_download)
   .setContentTitle("Downloading music")
   .setContentText(inputData.getString(KEY_TITLE) ?: "Preparing download")
   .setOngoing(true)
   .setProgress(0, 0, true)
   .build()
  return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(NOTIFICATION_ID, notification)
 }
 override suspend fun doWork(): Result {
  setForeground(getForegroundInfo())
  val id = inputData.getString(KEY_ID).orEmpty()
  val url = inputData.getString(KEY_URL).orEmpty()
  if (id.isBlank() || !url.startsWith("https://")) return Result.failure()
  val track = Track(
   id = id,
   title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Unknown title" },
   artist = inputData.getString(KEY_ARTIST).orEmpty().ifBlank { "Unknown artist" },
   album = inputData.getString(KEY_ALBUM).orEmpty(),
   artworkUrl = inputData.getString(KEY_ARTWORK).orEmpty(),
   durationMs = inputData.getLong(KEY_DURATION, 0L),
   streamUrl = url
  )
  return try {
   val app = applicationContext as VirexApplication
   setProgress(workDataOf("state" to "Downloading", "percent" to 0))
   app.repository.download(track, inputData.getBoolean(KEY_HIGH_QUALITY, false)).getOrThrow()
   setProgress(workDataOf("state" to "Complete", "percent" to 100))
   Result.success()
  } catch (e: Exception) {
   if (runAttemptCount < 2) Result.retry() else Result.failure()
  }
 }
 companion object {
  const val CHANNEL = "virex_downloads"
  const val NOTIFICATION_ID = 4107
  const val KEY_ID = "track_id"
  const val KEY_URL = "stream_url"
  const val KEY_TITLE = "title"
  const val KEY_ARTIST = "artist"
  const val KEY_ALBUM = "album"
  const val KEY_ARTWORK = "artwork"
  const val KEY_DURATION = "duration"
  const val KEY_HIGH_QUALITY = "high_quality"
 }
}
