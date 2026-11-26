package com.hz6826.campusnet.remote

import com.xjtu.toolbox.plugin.api.PluginTool
import org.json.JSONObject

/** 查询状态；`refresh=true` 才真的去查一次（会顺带测外网，慢）。 */
internal class StatusTool(private val remote: RemoteClient) : PluginTool {
    override fun name(): String = "status"

    override fun execute(argsJson: String): String {
        val args = runCatching { JSONObject(argsJson.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        val refresh = args.optBoolean("refresh", false)
        // 工具层只给这么多：一次要 200 条会把小岱的上下文塞满。
        val logs = args.optInt("logs", 0).coerceIn(0, 20)
        return remote.status(refresh = refresh, logs = logs).toString()
    }
}

/** 用本体里保存的账号登录；不收参数，密码不经手插件。 */
internal class LoginTool(private val remote: RemoteClient) : PluginTool {
    override fun name(): String = "login"

    override fun execute(argsJson: String): String = remote.login().toString()
}

/** 注销。 */
internal class LogoutTool(private val remote: RemoteClient) : PluginTool {
    override fun name(): String = "logout"

    override fun execute(argsJson: String): String = remote.logout().toString()
}
