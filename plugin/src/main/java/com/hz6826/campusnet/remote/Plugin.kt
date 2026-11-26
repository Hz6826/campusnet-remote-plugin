package com.hz6826.campusnet.remote

import com.xjtu.toolbox.plugin.api.Plugin
import com.xjtu.toolbox.plugin.api.PluginContext
import com.xjtu.toolbox.plugin.api.PluginEntry
import com.xjtu.toolbox.plugin.api.PluginPage
import com.xjtu.toolbox.plugin.api.PluginTool

/**
 * 插件入口：清单 `dex.entryClass` 指向这里，必须有无参构造。
 *
 * 这个插件只是「校园网络助手」的遥控器——账号密码留在本体里，插件通过 binder 让它干活。
 */
class CampusNetPlugin : PluginEntry {
    override fun create(context: PluginContext): Plugin = CampusNetPluginImpl(context)
}

private class CampusNetPluginImpl(context: PluginContext) : Plugin {

    private val remote = RemoteClient(context.applicationContext)
    private val home = HomePage(context, remote)

    override fun pages(): List<PluginPage> = listOf(home)

    override fun tools(): List<PluginTool> = listOf(
        StatusTool(remote),
        LoginTool(remote),
        LogoutTool(remote),
    )
}
