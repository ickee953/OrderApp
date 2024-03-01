/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.api.ApiHelper
import ru.fl.marketplace.app.data.api.RetrofitBuilder
import ru.fl.marketplace.app.data.db.AppDatabase
import ru.fl.marketplace.app.data.db.ItemDao
import ru.fl.marketplace.app.data.repository.ItemRepository
import ru.fl.marketplace.app.utils.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadAllItemsWorker(context: Context, parameters: WorkerParameters)
    : CoroutineWorker(context, parameters) {

    companion object {
        const val CHANNEL_ID = "notification_channel_download_all"
        const val NOTIFICATION_ID = 0
        const val MESSAGE_PARAM = "MESSAGE_PARAM"
        const val PROGRESS = "PROGRESS"
    }

    private lateinit var builder: NotificationCompat.Builder

    private val repository: ItemRepository = ItemRepository( ApiHelper( RetrofitBuilder.apiService) )
    private val itemDao: ItemDao = AppDatabase.getDatabase(context).itemsDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        setForeground(createForegroundInfo())

        return@withContext kotlin.runCatching {
            NotificationManagerCompat.from(applicationContext).apply {
                builder.setProgress(100, 0, true)
                notify(NOTIFICATION_ID, builder.build())
            }

            val received = repository.getItems()
            itemDao.deleteAll()
            var loadedItemsCnt = 0
            received.forEach {
                item ->  itemDao.insert( item )
                val receivedItem = repository.getItem( item.id )
                itemDao.update( receivedItem )

                loadedItemsCnt++
                val progress = received.size/ 100 * loadedItemsCnt

                setProgressAsync(workDataOf(PROGRESS to progress))

                NotificationManagerCompat.from(applicationContext).apply {
                    builder.setProgress(100, progress, false)
                    notify(NOTIFICATION_ID, builder.build())
                }
            }

            NotificationManagerCompat.from(applicationContext).apply {
                // When done, update the notification one more time to remove the progress bar
                builder.apply {
                    setContentText(
                        applicationContext.getString(R.string.notification_channel_download_complete)
                    )
                    setProgress(0, 0, false)
                }

                notify(NOTIFICATION_ID, builder.build())
            }

            Result.success()
        }.getOrElse {
            NotificationManagerCompat.from(applicationContext).apply {
                builder.apply {
                    setContentText(it.localizedMessage)
                    setContentTitle(applicationContext.getString(R.string.notification_channel_download_error))
                    setProgress(0, 0, false)
                    setOngoing(false)
                }

                notify(NOTIFICATION_ID, builder.build())
            }

            Result.failure(
                Data.Builder().putString(
                    MESSAGE_PARAM,
                    it.localizedMessage
                ).build()
            )
        }
    }

    private fun createForegroundInfo(): ForegroundInfo{
        NotificationUtils.createNotificationChannel(applicationContext, CHANNEL_ID)
        //notification builder setup
        builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setContentTitle(applicationContext.getString(R.string.notification_channel_name))
            setSmallIcon(R.drawable.ic_baseline_get_app_24)
            setProgress(100, 0, true)
            setOngoing(true)
        }

        return ForegroundInfo(NOTIFICATION_ID, builder.build())
    }

}
