<div align="center">

<img src="assets/logo.png" alt="Banira Codex" width="160" />

# Banira Codex（香草志）

**面向 Minecraft Forge、Fabric 与 NeoForge 的跨版本基础库模组。**

</div>

---

## 目录

- [介绍](#介绍)
- [支持范围](#支持范围)
- [设计目标](#设计目标)
- [作为依赖接入](#作为依赖接入)
- [稳定公共 API](#稳定公共-api)
- [配置](#配置)
- [事件与生命周期](#事件与生命周期)
- [网络](#网络)
- [客户端能力](#客户端能力)
- [数据与路径](#数据与路径)
- [版本迁移原则](#版本迁移原则)
- [构建](#构建)
- [许可证](#许可证)

## 介绍

Banira Codex 为其他 Minecraft 模组提供配置、网络、事件、玩家数据、通知、输入、HUD 与 GUI 等公共能力。

项目不追求用一个 jar 同时兼容所有 Minecraft 版本和加载器，而是为每个受支持组合维护独立分支与产物，并让依赖方尽可能使用同名、同语义、同结构的公共 API。依赖方切换 Minecraft 版本或加载器时，业务代码通常无需跟着替换 Forge、Fabric 或 NeoForge 类型。

模组 ID：`banira_codex`。

## 支持范围

| 加载器 | Minecraft 版本 | 分支格式 |
|---|---|---|
| Forge | 1.16.5、1.18.2、1.19.2、1.20.1、1.21.1 | `forge/<版本>` |
| Fabric | 1.16.5、1.18.2、1.19.2、1.20.1、1.21.1 | `fabric/<版本>` |
| NeoForge | 1.21.1 | `neoforge/<版本>` |

使用时必须让 Banira Codex 与依赖模组的 Minecraft 版本和加载器保持一致。

## 设计目标

- **调用方稳定**：子模组依赖 `xin.vanilla.banira.api`，不直接绑定加载器事件、网络上下文或配置类型。
- **版本独立发布**：每个加载器和 Minecraft 版本拥有独立分支、构建产物和 Maven 版本。
- **加载器边界明确**：Forge、Fabric、NeoForge 实现位于 `xin.vanilla.banira.internal.<loader>`。
- **客户端安全隔离**：客户端事件、输入、HUD 与 GUI API 位于 `xin.vanilla.banira.api.client`，服务端不得加载客户端类。
- **按真实差异适配**：公共语义保持一致，加载器或 Minecraft API 差异留在各分支内部实现。

## 作为依赖接入

### Maven Local

Banira Codex 的发布坐标为：

```text
xin.vanilla.banira:banira_codex:<loader>-<minecraftVersion>-<baniraVersion>
```

例如：

```text
xin.vanilla.banira:banira_codex:fabric-1.20.1-1.0.2
```

依赖方先声明 Maven Local：

```gradle
repositories {
    mavenLocal()
}
```

再按当前加载器使用对应依赖方式：

```gradle
dependencies {
    // Forge
    compileOnly fg.deobf("xin.vanilla.banira:banira_codex:${banira_version}")

    // Fabric
    modCompileOnly "xin.vanilla.banira:banira_codex:${banira_version}"

    // NeoForge
    compileOnly "xin.vanilla.banira:banira_codex:${banira_version}"
}
```

其中 `banira_version` 应填写完整版本，例如 `forge-1.20.1-1.0.2`。

正式运行时仍需在客户端或服务器的 `mods` 目录安装匹配的 Banira Codex jar。

### 加载器元数据

依赖模组还应在当前加载器的元数据中声明 `banira_codex`：

- Forge：`META-INF/mods.toml`
- Fabric：`fabric.mod.json`
- NeoForge：`META-INF/neoforge.mods.toml`

版本范围应与实际使用的 Banira Codex 版本保持一致。

## 稳定公共 API

依赖方优先使用 `xin.vanilla.banira.api` 及其子包：

| 入口 | 用途 |
|---|---|
| `Banira` | 当前平台及核心服务入口 |
| `BaniraConfigs` / `BaniraConfigViews` | 配置注册、查询与配置界面 |
| `BaniraNetwork` | 加载器无关的网络注册和发送 |
| `BaniraEvents` / `BaniraLifecycle` | 服务端、世界、玩家及生命周期事件 |
| `BaniraServer` | 当前服务端状态和安全访问 |
| `BaniraPlayerData` | 玩家持久化数据 |
| `BaniraDataPaths` | 模组数据、配置与世界路径 |
| `BaniraEnvironment` | 物理侧和运行环境判断 |
| `BaniraModPresence` | 模组存在性与可选集成状态 |
| `BaniraVirtualPermissions` | 虚拟权限注册与检查 |
| `BaniraNotificationTypes` | 服务端通知类型注册 |

`xin.vanilla.banira.internal` 是加载器适配和实现细节，不属于稳定 API。子模组不应 import 该包中的类型。

## 配置

公共配置模型使用 Banira 自己的作用域：

```java
ConfigScope.COMMON
ConfigScope.CLIENT
ConfigScope.SERVER
```

配置注册、保存、同步和编辑界面通过 `BaniraConfigs`、`BaniraConfigViews` 及公共配置 holder 完成。依赖方不需要直接暴露 `ForgeConfigSpec`、Fabric 配置实现或 NeoForge 配置类型。

客户端可通过统一配置编辑器展示 Banira 和子模组配置；Fabric 分支同时支持从 Mod Menu 打开客户端配置。

## 事件与生命周期

`BaniraEvents` 与 `BaniraLifecycle` 提供加载器无关事件：

- common setup
- 服务端启动、运行和停止
- 世界加载、保存和卸载
- 玩家登录、登出、克隆和维度切换
- 服务端 tick 与玩家数据保存

客户端事件单独位于 `xin.vanilla.banira.api.client.event`，包括：

- 客户端 setup 与 tick
- 屏幕打开、绘制和关闭
- 键盘、鼠标与聊天输入
- HUD overlay 绘制
- 纹理重载

监听器返回的 registration 可用于注销，加载器原生事件对象不会进入推荐公共 API。

## 网络

`BaniraNetwork` 提供统一网络通道、包注册、方向和上下文模型。公共包处理器不需要依赖 `SimpleChannel`、Fabric receiver 或加载器原生 buffer。

网络层支持：

- C2S、S2C 与双向消息
- 配置快照与配置同步
- 玩家数据同步
- 大包分片、重组和异常分片丢弃
- 每个通道独立的 packet id

子模组应使用自己的 mod ID 和通道名称，避免与 Banira Codex 或其他模组冲突。

## 客户端能力

`xin.vanilla.banira.api.client` 提供：

- `BaniraInput`：快捷键注册和状态查询
- `BaniraKeyPressTracker`：按下、长按和释放手势
- `BaniraMouseClickTracker`：单击、双击和鼠标手势
- `BaniraHudEvents`：经验条、经验文本及其他 HUD 元素拦截和绘制
- `BaniraNotifications`：客户端通知与通知记录
- `BaniraLogos`：模组 logo 查询和覆盖
- `BaniraDrawContext`：跨版本绘制上下文

GUI 基础设施位于 `xin.vanilla.banira.client.gui`。该部分可供客户端子模组使用，但加载器和 Minecraft 版本差异仍由对应 Banira 分支负责适配。

## 数据与路径

不要在子模组中依赖已移除的静态目录字段。使用 `BaniraDataPaths` 获取：

- Banira 根数据目录
- 模组专属数据目录
- 世界数据目录
- 玩家数据目录
- 客户端或服务端配置路径

`BaniraPlayerData` 用于获取和保存玩家持久化数据。子模组应使用自己的 mod ID 或数据后缀隔离数据。

## 版本迁移原则

切换版本或加载器时：

1. 替换为匹配分支构建的 Banira Codex 依赖。
2. 保持 `xin.vanilla.banira.api` 调用结构不变。
3. 将确实需要加载器原生 API 的代码隔离到子模组自己的 `internal.<loader>`。
4. 不复制 Banira 的 loader adapter，也不依赖 Banira `internal`。
5. 使用目标分支的客户端、专用服务器和网络 smoke 流程验证真实行为。

## 构建

docs 分支提供统一批量构建脚本：

```bat
scripts\build-all.bat
```

脚本会在 detached 临时 worktree 中依次构建所有维护分支，不会切换当前工作树。Banira Codex 构建完成后还会执行 `publishToMavenLocal`。

仅检查分支与 JDK 配置，不执行构建：

```bat
scripts\build-all.bat -ListOnly
```

单个代码分支仍可直接执行：

```bash
./gradlew clean test assemble publishToMavenLocal
```

## 许可证

MIT License

如有问题或建议，欢迎提交 Issues 或 Pull requests。
