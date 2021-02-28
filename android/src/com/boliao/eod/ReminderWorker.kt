package com.boliao.eod

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
//import androidx.core.app.NotificationCompat;

/**
 * A reminder worker to do the work need to generate and send reminders for charging.
 */
class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private var count = 0
    private val notMgr: NotificationManager
    private val NID = 8
    private val NCID = "2"
    private val NNAME = "My Channel Name"
    private val NDESC = "This is testing channel"

    init {
        // init notification channel
        val chan = NotificationChannel(NCID, NNAME, NotificationManager.IMPORTANCE_HIGH)
        chan.description = NDESC

        // init notification manager
        notMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notMgr.createNotificationChannel(chan)
    }

    /**
     * Where the real work lies for this worker object.
     * - simply build a notification to be sent by the noti mgr
     */
    override fun doWork(): Result {
        Log.i(TAG, "REMINDER TO CHARGE" + count++)

        // TODO notification to remind charge
        // - note the builder pattern is a common design pattern used for creating objs in the api
        val n = Notification.Builder(super.getApplicationContext(), NCID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("eod REMINDER to charge")
                .setContentText("How to fight bugs if ur phone juice run out...")
                .setAutoCancel(true)
                .build()
        notMgr.notify(NID, n)

        // indicate success or failure
        // - e.g., Result.retry() tells work manager to try again later
        // - e.g., Result.failure() tells WM not to try already
        return Result.success()
    }

    companion object {
        private val TAG = ReminderWorker::class.simpleName
    }
}