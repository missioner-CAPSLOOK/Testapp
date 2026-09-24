package ru.example.callrecordertest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        const val ACTION_START_IDLE = "START_IDLE"
        const val ACTION_INCOMING_RINGING = "INCOMING_RINGING"
        const val ACTION_CALL_STARTED = "CALL_STARTED"
        const val ACTION_CALL_ENDED = "CALL_ENDED"
        const val EXTRA_NUMBER = "NUMBER"

        private const val CHANNEL_ID = "call_recorder"
        private const val NOTIFICATION_ID = 1001
    }

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var ringing = false
    private var recording = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Ожидание звонка"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_IDLE -> updateNotification("Ожидание звонка")

            ACTION_INCOMING_RINGING -> {
                ringing = true
                updateNotification("Входящий звонок")
            }

            ACTION_CALL_STARTED -> {
                // OFFHOOK бывает и для исходящего, и после ответа на входящий.
                startRecording()
            }

            ACTION_CALL_ENDED -> {
                stopRecording()
                ringing = false
                updateNotification("Ожидание звонка")
            }
        }

        return START_STICKY
    }

    private fun startRecording() {
        if (recording) return

        val dir = File(
            getExternalFilesDir(null),
            "Recordings"
        ).apply { mkdirs() }

        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss",
            Locale.US
        ).format(Date())

        recordingFile = File(dir, "CALL_$timestamp.m4a")

        try {
            val r = MediaRecorder(this)

            // В тестовой версии используется микрофон.
            // Android обычно не разрешает обычному приложению напрямую
            // захватывать полный системный voice-call audio.
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(recordingFile!!.absolutePath)

            r.prepare()
            r.start()

            recorder = r
            recording = true
            updateNotification("Идёт тестовая запись")
        } catch (e: Exception) {
            recording = false
            recorder?.release()
            recorder = null
            recordingFile?.delete()
            recordingFile = null
            updateNotification("Не удалось запустить запись: ${e.javaClass.simpleName}")
        }
    }

    private fun stopRecording() {
        if (!recording) return

        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
            recordingFile?.delete()
        } finally {
            recorder?.release()
            recorder = null
            recording = false
            recordingFile = null
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Запись звонков",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Тест записи звонков")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
