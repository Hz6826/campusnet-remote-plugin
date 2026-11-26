package com.hz6826.campusnet.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 和「校园网络助手」本体通信的薄客户端。
 *
 * 走的是 binder：`bindService` 拿到本体返回的 [Messenger]，发一条带 `replyTo` 的消息，
 * 结果回投到 [replyHandler]。整个过程是同步等待（[request] 会阻塞到有结果或超时），
 * 所以只能在工具线程 / 页面自己的后台线程里调，**主线程千万别碰**。
 *
 * 为什么不用 AIDL：插件是 dex 加载的，把 AIDL 生成的 Stub 塞进插件 APK 容易踩 descriptor
 * 不一致的坑；四条消息用 Messenger 传反而更省事。
 */
internal class RemoteClient(private val context: Context) {

    fun hello(): JSONObject = request(MSG_HELLO, null, HELLO_TIMEOUT)

    fun status(refresh: Boolean = false, logs: Int = 0, timeoutMs: Long = STATUS_TIMEOUT): JSONObject =
        request(
            MSG_STATUS,
            Bundle().apply {
                putBoolean("refresh", refresh)
                putInt("logs", logs)
            },
            timeoutMs,
        )

    fun login(timeoutMs: Long = 45_000): JSONObject = request(MSG_LOGIN, null, timeoutMs)

    fun logout(timeoutMs: Long = 20_000): JSONObject = request(MSG_LOGOUT, null, timeoutMs)

    /** 本体装了没；装了但版本太旧由 [hello] 的 versionCode 判断。 */
    fun isInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE, 0)
    }.isSuccess

    private fun request(what: Int, args: Bundle?, timeoutMs: Long): JSONObject {
        val latch = CountDownLatch(1)
        var result: JSONObject? = null

        val reply = Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_RESULT && msg.arg1 == what) {
                    result = msg.data?.getString("json")?.let { raw ->
                        runCatching { JSONObject(raw) }.getOrNull()
                    }
                    latch.countDown()
                }
            }
        })

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val messenger = Messenger(service)
                val message = Message.obtain(null, what).apply {
                    replyTo = reply
                    data = args ?: Bundle()
                }
                val sent = runCatching { messenger.send(message) }.isSuccess
                if (!sent) {
                    result = error("not_installed")
                    latch.countDown()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }

        val intent = Intent().setClassName(PACKAGE, SERVICE_CLASS)
        val bound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)
        if (!bound) return error("not_installed")

        return try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                error("timeout")
            } else {
                result ?: error("no_result")
            }
        } finally {
            runCatching { context.unbindService(connection) }
        }
    }

    companion object {
        const val PACKAGE = "com.hz6826.campusnetlogin"
        const val MAIN_ACTIVITY = "$PACKAGE.MainActivity"
        const val SERVICE_CLASS = "$PACKAGE.remote.RemoteControlService"

        private const val MSG_HELLO = 1
        private const val MSG_STATUS = 2
        private const val MSG_LOGIN = 3
        private const val MSG_LOGOUT = 4
        private const val MSG_RESULT = 100

        private const val HELLO_TIMEOUT = 4_000L
        private const val STATUS_TIMEOUT = 20_000L

        fun error(code: String): JSONObject = JSONObject().put("ok", false).put("error", code)

        /** 从可能带 `null` 的 JSON 里取一个能显示的值。 */
        fun JSONObject.text(key: String): String {
            val raw = optString(key)
            return if (raw.isBlank() || raw == "null") "—" else raw
        }
    }
}
