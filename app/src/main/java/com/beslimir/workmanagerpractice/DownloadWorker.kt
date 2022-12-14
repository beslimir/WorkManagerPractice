package com.beslimir.workmanagerpractice

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(
    context,
    workerParams
) {

    override suspend fun doWork(): Result {
        startForegroundService()
        //to demonstrate a "long-running" task
        delay(5000L)

        val response = FileApi.instance.downloadImage()
        response.body()?.let { body ->
            return withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "image.jpg")
                val outputStream = FileOutputStream(file)
                outputStream.use { stream ->
                    try {
                        stream.write(body.bytes())
                    } catch (e: IOException) {
                        return@withContext Result.failure(
                            workDataOf(
                                WorkerKeys.ERROR_MSG to e.localizedMessage
                            )
                        )
                    }
                }
                Result.success(
                    workDataOf(
                        WorkerKeys.IMAGE_URI to file.toUri().toString()
                    )
                )
            }
        }
        if (!response.isSuccessful) {
            if (response.code().toString().startsWith("5")) {
                return Result.retry()
            }
            return Result.failure(
                workDataOf(
                    WorkerKeys.ERROR_MSG to "Network error"
                )
            )
        }
        return Result.failure(
            workDataOf(
                WorkerKeys.ERROR_MSG to "Unknown error"
            )
        )
    }

    private suspend fun startForegroundService() {
        try {
            setForeground(
                ForegroundInfo(
                    Random.nextInt(),
                    NotificationCompat.Builder(context, "download_channel")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentText("Downloading...")
                        .setContentTitle("Download in progress")
                        .build()
                )
            )
        } catch (e: IllegalStateException) {
            //These might occur when your app is not able to run in the foreground at this point
            Log.d("WorkManager", "startForegroundService: ${e.message}")
        }
    }

}