package com.captchaalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.captchaalarm.data.AlarmDatabase
import kotlin.concurrent.thread

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL") ?: ""
        val captchaEnabled = intent?.getBooleanExtra("CAPTCHA_ENABLED", true) ?: true
        val mathEnabled = intent?.getBooleanExtra("MATH_ENABLED", true) ?: true
        val scrambleEnabled = intent?.getBooleanExtra("SCRAMBLE_ENABLED", true) ?: true

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_LABEL", alarmLabel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val captchaIntent = Intent(context, CaptchaActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("ALARM_ID", alarmId)
            putExtra("CAPTCHA_ENABLED", captchaEnabled)
            putExtra("MATH_ENABLED", mathEnabled)
            putExtra("SCRAMBLE_ENABLED", scrambleEnabled)
        }
        context.startActivity(captchaIntent)

        if (alarmId > 0) {
            thread {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val alarm = dao.getById(alarmId) ?: return@thread
                if (alarm.daysOfWeek.isNotBlank()) {
                    AlarmScheduler.schedule(context, alarm)
                } else {
                    dao.update(alarm.copy(isEnabled = false))
                }
            }
        }
    }
}
