package com.example.smsgateway

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class SmsService : Service() {
    private var server: SimpleHttpServer? = null
    private val PORT = 8080
    private val API_KEY = "4f9d7b2c-8a1e-4c6b-9f3a-2d5e7b8c1a9f"

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        server = SimpleHttpServer(PORT, this)
        server?.start()
    }

    override fun onDestroy() {
        server?.stop()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val channelId = "sms_bridge_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "WiFi SMS Bridge", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bulk SMS Gateway")
            .setContentText("Running — listening for SMS send requests")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    class SimpleHttpServer(port: Int, private val ctx: SmsService) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession?): Response {
            val uri = session?.uri ?: "/"
            if (uri.startsWith("/send")) {
                val q = session.parameters
                val key = q["apikey"]?.get(0)
                if (key != ctx.API_KEY) {
                    return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"error\":\"invalid apikey\"}")
                }
                val number = q["number"]?.get(0) ?: ""
                val message = q["message"]?.get(0) ?: ""
                if (number.isBlank() || message.isBlank()) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"missing number or message\"}")
                }
                return try {
                    val smsManager = SmsManager.getDefault()
                    val parts = smsManager.divideMessage(message)
                    for (part in parts) {
                        smsManager.sendTextMessage(number, null, part, null, null)
                    }
                    val ok = JSONObject()
                    ok.put("status", "queued")
                    ok.put("to", number)
                    newFixedLengthResponse(Response.Status.OK, "application/json", ok.toString())
                } catch (e: Exception) {
                    val err = JSONObject()
                    err.put("error", e.message)
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", err.toString())
                }
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"running\"}")
        }
    }
}
