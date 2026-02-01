package com.captchaalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlin.math.sin

class AlarmService : Service() {

    private var audioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPlaying = false

    companion object {
        const val CHANNEL_ID = "captcha_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.captchaalarm.STOP_ALARM"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        setMaxVolume()
        startHarshAlarmSound()
        startAggressiveVibration()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Captcha Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notification channel"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val captchaIntent = Intent(this, CaptchaActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, captchaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ALARM! WAKE UP!")
            .setContentText("Solve 3 captcha challenges to dismiss")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun setMaxVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
    }

    private fun startHarshAlarmSound() {
        isPlaying = true
        Thread {
            val sampleRate = 44100
            // Generate a harsh, jarring alarm pattern:
            // alternating high-pitched tones with dissonant intervals
            val oneDuration = sampleRate / 2  // 0.5 second per tone burst
            val patternSamples = sampleRate * 4 // 4 seconds total pattern then loop
            val buffer = ShortArray(patternSamples)

            for (i in 0 until patternSamples) {
                val timeInPattern = i.toFloat() / sampleRate
                val sample: Double

                when {
                    // Harsh high beep (0-0.4s) - 2500Hz + 3100Hz dissonant
                    timeInPattern < 0.4f -> {
                        sample = 0.5 * sin(2.0 * Math.PI * 2500.0 * i / sampleRate) +
                                0.5 * sin(2.0 * Math.PI * 3100.0 * i / sampleRate)
                    }
                    // Brief silence (0.4-0.5s)
                    timeInPattern < 0.5f -> {
                        sample = 0.0
                    }
                    // Even higher harsh beep (0.5-0.9s) - 3000Hz + 3700Hz
                    timeInPattern < 0.9f -> {
                        sample = 0.5 * sin(2.0 * Math.PI * 3000.0 * i / sampleRate) +
                                0.5 * sin(2.0 * Math.PI * 3700.0 * i / sampleRate)
                    }
                    // Brief silence (0.9-1.0s)
                    timeInPattern < 1.0f -> {
                        sample = 0.0
                    }
                    // Rapid staccato (1.0-2.0s) - fast on/off at 2800Hz
                    timeInPattern < 2.0f -> {
                        val subTime = ((i - sampleRate) % (sampleRate / 10))
                        sample = if (subTime < sampleRate / 20) {
                            0.6 * sin(2.0 * Math.PI * 2800.0 * i / sampleRate) +
                            0.4 * sin(2.0 * Math.PI * 1400.0 * i / sampleRate)
                        } else 0.0
                    }
                    // Brief silence (2.0-2.2s)
                    timeInPattern < 2.2f -> {
                        sample = 0.0
                    }
                    // Sweeping siren (2.2-3.6s) - frequency sweep 2000-4000Hz
                    timeInPattern < 3.6f -> {
                        val sweepProgress = (timeInPattern - 2.2f) / 1.4f
                        val freq = 2000.0 + 2000.0 * sweepProgress
                        sample = sin(2.0 * Math.PI * freq * i / sampleRate)
                    }
                    // Silence gap (3.6-4.0s)
                    else -> {
                        sample = 0.0
                    }
                }

                buffer[i] = (sample * Short.MAX_VALUE * 0.9).toInt().toShort()
            }

            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(buffer.size * 2, minBufSize))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.apply {
                write(buffer, 0, buffer.size)
                setLoopPoints(0, buffer.size, -1) // loop forever
                play()
            }
        }.start()
    }

    private fun startAggressiveVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Aggressive vibration: short intense bursts
        val pattern = longArrayOf(0, 300, 100, 300, 100, 600, 200, 300, 100, 300, 100, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CaptchaAlarm::AlarmWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun stopAlarm() {
        isPlaying = false

        audioTrack?.apply {
            try {
                stop()
                release()
            } catch (_: Exception) {}
        }
        audioTrack = null

        vibrator?.cancel()
        vibrator = null

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
