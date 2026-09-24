package ru.example.callrecordertest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val action = when (state) {
            TelephonyManager.EXTRA_STATE_RINGING ->
                RecordingService.ACTION_INCOMING_RINGING

            TelephonyManager.EXTRA_STATE_OFFHOOK ->
                RecordingService.ACTION_CALL_STARTED

            TelephonyManager.EXTRA_STATE_IDLE ->
                RecordingService.ACTION_CALL_ENDED

            else -> return
        }

        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            this.action = action
            putExtra(RecordingService.EXTRA_NUMBER, number ?: "")
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
