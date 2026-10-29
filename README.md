# 安逸软件汇 — Android 原生版（Kotlin 重构）

> 从 Flutter `flutter_softlib` 全量移植的原生项目。包名 `com.soft.anyi`、签名 `ayrjh.jks` 与 Flutter 版一致，可直接覆盖安装。

## 技术栈

| 层 | 方案 |
|---|---|
| UI | 原生 View 体系（LinearLayout/RecyclerView，无 Compose，启动快、包小） |
| 网络 | OkHttp 单例连接池（对应 Flutter ApiHttp 共享客户端提速方案） |
| 图片 | Coil（列表降采样，对应 memCacheWidth） |
| 音视频 | Media3 / ExoPlayer |
| 数据 | org.json + SharedPreferences（无 Room 依赖，保持轻量） |

## 目录

```
app/src/main/java/com/soft/anyi/
├── SoftApp.kt            # 全局初始化（对应 main.dart）
├── data/                 # 网络层
│   ├── Http.kt           #   OkHttp 单例 + 统一 {code,msg,data} 解析 + token 失效处理
│   ├── ApiHost.kt        #   服务端入口（改域名只改这里）
│   ├── ConfigRepo.kt     #   远程配置（磁盘缓存优先）
│   ├── SoftRepo.kt       #   软件列表/详情/分类/解析
│   ├── UserRepo.kt       #   登录/注册/签到/积分/卡密/邀请
│   ├── VersionRepo.kt    #   版本检查（已修 Flutter 版取最旧 bug）
│   ├── MessageRepo.kt    #   消息（含 wait 长轮询）
│   ├── PayRepo.kt        #   会员/充值下单轮询
│   ├── PostRepo.kt       #   广场帖子
│   └── MiscRepos.kt      #   工具/点赞/愿望/线报/卡密商店等
├── ui/
│   ├── splash/           # 开屏：倒计时 + 维护拦截 + 公告 + 版本弹窗
│   ├── main/             # 主框架 5 Tab（首页/工具/广场/线报/我的）+ Liquid 底栏
│   ├── home/  search/    # 软件列表 / 搜索
│   ├── details/          # 详情 / 评论 / 全屏图查看器
│   ├── tools/            # 工具箱 + 内置浏览器 + 播放页
│   ├── square/ tips/ mine/  # 广场 / 线报 / 我的
│   └── common/           # Ui 工具 / WebView / Pager
└── util/                 # AppSign/DeviceInfo/Sfx/ApkInstaller/FavStore
```

## 构建

```bash
./gradlew assembleRelease     # CI 已配 JDK17 + 缓存
```

CI：`.github/workflows/build.yml`，推 tag `v*` 自动发 Release。

## 服务端接口

100% 复用现有 `flrjk.52yfx.cn` 的 `/api/softlib/*` 接口与 `X-App-Sign` 签名头，服务端零改动。

## 待补（下一步）

- [ ] QQ 登录 SDK 桥（openid/access_token → qq_sdk_login）
- [ ] 下载进度通知栏（DownloadManager 已接，自定义进度 UI 待加）
- [ ] 广场发帖图片上传
- [ ] 管理端入口（isAdmin 时显示）
