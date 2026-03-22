package cat.narezany.itp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import android.webkit.CookieManager
import kotlin.concurrent.thread

class NotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 15000L // 15 seconds
    private lateinit var prefs: SharedPreferences

    companion object {
        const val CHANNEL_ID_FOREGROUND = "itp_foreground_service"
        const val CHANNEL_ID_ALERTS = "itp_alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_ALERT = 1002
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForNotifications()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("ITP_PREFS", Context.MODE_PRIVATE)
        createNotificationChannels()
        startForeground(NOTIFICATION_ID_FOREGROUND, createForegroundNotification())
        handler.post(checkRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("test_notif", false) == true) {
            forceTestFlag = true
            checkForNotifications()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val fgChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Фоновая работа",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление для работы чеккера уведомлений"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Уведомления сайта",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Показывает новые уведомления ИТД"
            }

            manager.createNotificationChannel(fgChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle("ИТП")
            .setContentText("Приложение работает для получения уведомлений")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @Volatile private var forceTestFlag = false
    
    private fun checkForNotifications() {
        val forceTest = forceTestFlag
        forceTestFlag = false
        
        thread {
            try {
                // Fetch cookies from WebView natively
                var cookiesStr = ""
                handler.post {
                    val c1 = CookieManager.getInstance().getCookie("https://итд.com/") ?: ""
                    val c2 = CookieManager.getInstance().getCookie("https://xn--d1ah4a.com/") ?: ""
                    cookiesStr = "$c1; $c2"
                }
                Thread.sleep(200)

                val userAgentStr = "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
                val url = java.net.URL("https://итд.com/api/notifications/stream")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Cookie", cookiesStr)
                conn.setRequestProperty("User-Agent", userAgentStr)
                conn.setRequestProperty("Accept", "text/event-stream")
                conn.readTimeout = 10000
                conn.connectTimeout = 10000

                var foundData = false
                conn.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("data:")) {
                            val json = line.substring(5).trim()
                            if (json.isNotEmpty() && json != "{}") {
                                val hash = json.hashCode().toString()
                                val lastHash = prefs.getString("last_notification_hash", "")
                                if (hash != lastHash || forceTest) {
                                    prefs.edit().putString("last_notification_hash", hash).apply()
                                    // For testing, just show the JSON straight into notification payload!
                                    showSystemNotification("Новое уведомление [RAW]", "Данные с API:", json)
                                    foundData = true
                                    break
                                }
                            }
                        }
                    }
                }
                
                if (!foundData && forceTest) {
                    showSystemNotification("Тест API", "Пусто", "Нет данных новых уведомлений в стриме (JSON).")
                }
                conn.disconnect()
            } catch (e: java.net.SocketTimeoutException) {
                if (forceTest) showSystemNotification("Тайм-аут API", "Ожидание", "Сайт не отдал уведомления за 10 секунд.")
            } catch (e: Exception) {
                Log.e("ITP_NOTIF", "Error reading SSE endpoint", e)
                if (forceTest) {
                    showSystemNotification("Ошибка сервера", e.javaClass.simpleName, e.message ?: "")
                }
            }
        }
    }

    private fun showSystemNotification(author: String, action: String, content: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_URL, "https://итд.com/notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = if (content.length > 50) content.take(50) + "..." else content
        val title = "$author $action"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_ALERT, notification)
    }
}
