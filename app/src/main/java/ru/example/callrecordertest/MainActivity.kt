package ru.example.callrecordertest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 50, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Тест автоматической записи звонков"
            textSize = 24f
        }

        val status = TextView(this).apply {
            text = "\\nСтатус: приложение установлено.\\n\\nОно ждёт входящий или исходящий звонок."
            textSize = 18f
        }

        root.addView(title)
        root.addView(status)
        setContentView(root)

        requestNeededPermissions()

        // Запускаем foreground service, который остаётся активным.
        ContextCompat.startForegroundService(
            this,
            Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START_IDLE
            }
        )
    }

    private fun requestNeededPermissions() {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }
}
