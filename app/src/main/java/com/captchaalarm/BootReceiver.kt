package com.captchaalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.captchaalarm.data.AlarmDatabase
import kotlin.concurrent.thread

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        thread {
            val dao = AlarmDatabase.getInstance(context).alarmDao()
            val enabledAlarms = dao.getEnabled()
            for (alarm in enabledAlarms) {
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}
