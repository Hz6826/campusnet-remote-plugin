package com.hz6826.campusnet.remote

import android.content.Intent
import com.xjtu.toolbox.plugin.api.PluginContext
import com.xjtu.toolbox.plugin.api.PluginPage
import com.xjtu.toolbox.plugin.api.UiAlign
import com.xjtu.toolbox.plugin.api.UiBadgeNode
import com.xjtu.toolbox.plugin.api.UiBadgeTone
import com.xjtu.toolbox.plugin.api.UiButtonNode
import com.xjtu.toolbox.plugin.api.UiButtonStyle
import com.xjtu.toolbox.plugin.api.UiCardNode
import com.xjtu.toolbox.plugin.api.UiColumnNode
import com.xjtu.toolbox.plugin.api.UiGridNode
import com.xjtu.toolbox.plugin.api.UiIconNode
import com.xjtu.toolbox.plugin.api.UiKeyValueNode
import com.xjtu.toolbox.plugin.api.UiNode
import com.xjtu.toolbox.plugin.api.UiRowNode
import com.xjtu.toolbox.plugin.api.UiTextColor
import com.xjtu.toolbox.plugin.api.UiTextNode
import com.xjtu.toolbox.plugin.api.UiTextStyle
import com.xjtu.toolbox.plugin.api.UiWeightNode
import org.json.JSONObject

/**
 * 遥控器主页：上面是本体那张状态卡的复刻，中间是在线信息（页面主体），
 * 下面一个登录/注销按钮，最底下一排三个入口卡（跳本体对应页面）。
 *
 * `render()` 在主线程跑，只读缓存；所有网络/binder 都在 [runRemote] 起的线程里做。
 */
internal class HomePage(
    private val ctx: PluginContext,
    private val remote: RemoteClient,
) : PluginPage() {

    private var snapshot: JSONObject? = null
    private var busy = false
    private var label: String? = null

    override fun id(): String = "home"

    override fun onStart() {
        if (snapshot == null && !busy) refresh()
    }

    override fun render(): UiNode {
        val data = snapshot
        return UiColumnNode(
            children = listOf(
                statusCard(data),
                infoCard(data),
                actionButton(data),
                entries(),
            ),
            spacingDp = 14,
        )
    }

    override fun onAction(actionId: String, value: String?) {
        when (actionId) {
            "refresh" -> if (!busy) refresh()
            "login" -> runRemote("正在登录…") { remote.login() }
            "logout" -> runRemote("正在注销…") { remote.logout() }
            "open_login" -> open(null)
            "open_logs" -> open("logs")
            "open_settings" -> open("settings")
        }
    }

    // ── 数据 ────────────────────────────────────────────────────────────────

    private fun refresh() {
        runRemote("正在刷新…") { remote.status(refresh = true) }
    }

    private fun runRemote(working: String, block: () -> JSONObject) {
        if (busy) return
        busy = true
        label = working
        invalidate()
        Thread {
            val result = runCatching(block).getOrElse { RemoteClient.error("plugin_failed") }
            snapshot = result
            busy = false
            label = null
            invalidate()
        }.apply { isDaemon = true }.start()
    }

    // ── 界面 ────────────────────────────────────────────────────────────────

    private fun statusCard(data: JSONObject?): UiNode {
        val failed = data?.optBoolean("ok") == false
        val statusText = when {
            label != null -> label!!
            failed -> errorText(data.optString("error"))
            data == null -> "正在连接「校园网络助手」…"
            else -> data.optString("status").ifBlank { "读取中…" }
        }
        val online = data?.optBoolean("online") == true
        val campus = data?.optString("campus").orEmpty()
        val badge = when {
            online -> "已连接" to UiBadgeTone.Accent
            campus == "off_campus" -> "无需登录" to UiBadgeTone.Neutral
            else -> "等待连接" to UiBadgeTone.Warning
        }
        val ssid = data?.let { it.text("ssid") }?.takeIf { it != "—" }

        val info = mutableListOf<UiNode>(UiBadgeNode(badge.first, badge.second))
        if (ssid != null) {
            info += UiTextNode("· $ssid", UiTextStyle.Caption, secondary = true)
        }

        return UiCardNode(
            child = UiColumnNode(
                children = listOf(
                    UiTextNode(statusText, UiTextStyle.Title),
                    UiRowNode(children = info, spacingDp = 6),
                    UiTextNode(
                        text = when {
                            busy -> "处理中…"
                            failed -> "重试"
                            else -> "刷新并检测外网"
                        },
                        style = UiTextStyle.Caption,
                        color = UiTextColor.Primary,
                        actionId = if (busy) "" else "refresh",
                    ),
                ),
                spacingDp = 10,
            ),
        )
    }

    private fun infoCard(data: JSONObject?): UiNode {
        val online = data?.optBoolean("online") == true
        if (!online) {
            return UiCardNode(
                child = UiColumnNode(
                    children = listOf(
                        UiTextNode("在线信息", UiTextStyle.Section),
                        UiTextNode("还没连上校园网", UiTextStyle.Body),
                        UiTextNode(
                            "连上校园网 Wi-Fi 后，这里会显示 IP、在线时长、流量和余额。",
                            UiTextStyle.Caption,
                            secondary = true,
                        ),
                    ),
                    spacingDp = 8,
                ),
            )
        }
        val rows = listOf(
            "账号" to data.text("account"),
            "在线 IP" to data.text("ip"),
            "套餐" to data.text("product"),
            "在线时长" to formatDuration(data.optLong("usedSeconds")),
            "已用流量" to formatBytes(data.optDouble("usedBytes")),
            "余额" to "¥" + data.optDouble("balance").toString().take(6),
            "在线设备" to "${data.optInt("devices")} 台",
        ).map { (label, value) -> UiKeyValueNode(label, value) }

        return UiCardNode(
            child = UiColumnNode(
                children = listOf(UiTextNode("在线信息", UiTextStyle.Section)) + rows,
                spacingDp = 10,
            ),
        )
    }

    private fun actionButton(data: JSONObject?): UiNode {
        val enabled = !busy && data != null
        val button = if (data?.optBoolean("online") == true) {
            UiButtonNode("注销", "logout", UiButtonStyle.Tonal, enabled = enabled)
        } else {
            UiButtonNode("登录", "login", UiButtonStyle.Filled, enabled = enabled)
        }
        // 按钮默认按内容宽度摆；包一层权重才占满整行
        return UiRowNode(children = listOf(UiWeightNode(1f, button)))
    }

    private fun entries(): UiNode = UiGridNode(
        columns = 3,
        children = listOf(
            entry("登录", "user", "open_login"),
            entry("日志", "doc", "open_logs"),
            entry("设置", "task", "open_settings"),
        ),
    )

    private fun entry(title: String, icon: String, actionId: String): UiNode = UiCardNode(
        child = UiColumnNode(
            children = listOf(
                UiIconNode(icon, 22),
                UiTextNode(title, UiTextStyle.Caption),
            ),
            spacingDp = 6,
            alignment = UiAlign.Center,
        ),
        actionId = actionId,
        paddingDp = 12,
    )

    // ── 工具 ────────────────────────────────────────────────────────────────

    private fun open(page: String?) {
        val intent = Intent()
            .setClassName(RemoteClient.PACKAGE, RemoteClient.MAIN_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (page != null) intent.putExtra("page", page)
        runCatching { ctx.applicationContext.startActivity(intent) }
            .onFailure { ctx.toast("没找到「校园网络助手」") }
    }

    private fun errorText(code: String): String = when (code) {
        "not_installed" -> "没找到「校园网络助手」"
        "timeout" -> "本体没响应，可能被系统冻结了"
        "remote_disabled" -> "本体里关掉了「允许盒子遥控」"
        "not_configured" -> "本体里还没保存账号，去「登录」页填一次"
        "busy" -> "本体正忙着上一个操作"
        "not_ready" -> "本体还在启动，稍等一下再试"
        "forbidden" -> "本体不认这个调用方"
        else -> "读取失败"
    }
}
