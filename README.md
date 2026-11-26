# 校园网络遥控器

「岱宗盒子」的插件，用来遥控 **[校园网络助手](https://github.com/Hz6826/CampusNetLoginAndroid)**（CampusNetLoginAndroid）：看在线信息、登录、注销，不用来回切应用。

## 它做什么

- **校园网络**页：状态卡（已连接 / 无需登录 / 等待连接，带当前 Wi-Fi）、在线信息（账号、IP、套餐、时长、流量、余额、设备数）、登录 / 注销按钮
- 底部三个入口：登录、日志、设置 —— 直接跳到本体对应页面
- 给小岱的三个工具：`status`、`login`、`logout`

## 怎么和本体通信

账号密码是用 Android Keystore 加密存在本体里的，插件**读不到也不该读**。所以插件只通过 binder 让本体干活：

1. 插件 `bindService` 到 `com.hz6826.campusnetlogin` 的 `com.hz6826.campusnetlogin.remote.RemoteControlService`
2. 拿到本体返回的 `Messenger`，发一条带 `replyTo` 的消息（`HELLO` / `STATUS` / `LOGIN` / `LOGOUT`）
3. 本体按 `Message.sendingUid` 校验调用方包名（只认岱宗盒子正式包与 debug 包），并且要本体设置里那个「允许盒子遥控」开关开着

## 环境要求

| 需要 | 版本 |
|---|---|
| 岱宗盒子 | 声明了 `QUERY_ALL_PACKAGES` 的版本（Android 11 起看不到别的包就绑不上） |
| 校园网络助手 | versionCode 10 及以上（遥控 Service 是这一版加的） |

## 构建

```bash
./gradlew :plugin:assembleDebug
python3 package.py plugin/build/outputs/apk/debug/plugin-debug.apk -o dist/plugin.zip
```

`plugin-api/` 是宿主 API 的副本，构建时自动从 `xjtu-dzhz/dzhz` 的 `dev` 分支拉，**不要在这里改**。

## 自动构建

`.github/workflows/build.yml`：每次 push 构建 zip 传到 Artifacts；手动 Run workflow（填版本号）会发到 Release。签名私钥存在仓库 Secret `PLUGIN_SIGNING_KEY`。

## 许可证

[GPL-3.0](./LICENSE)，和盒子本体 [xjtu-dzhz/dzhz](https://github.com/xjtu-dzhz/dzhz) 一致。
