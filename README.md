# Banira Codex（香草志）

面向 **Minecraft 1.16 +** 的基础库模组，提供配置、事件、网络、GUI 组件、消息与通知、玩家持久化数据等可复用能力。Mod ID：
`banira_codex`（常量见 `BaniraCodex.MODID`）。

---

## 1. 作为依赖接入

### 1.1 `mods.toml` 声明

在依赖方模组的 `META-INF/mods.toml` 中增加对本模组的依赖（版本范围按实际发布版本调整）：

```toml
[[dependencies.your_mod_id]]
modId = "banira_codex"
mandatory = true
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "BOTH"
```

若仅使用服务端或客户端子集能力，可将 `side` 改为 `SERVER` / `CLIENT`（与 `mods.toml` 中本模组声明方式一致即可）。

### 1.2 Gradle 编译依赖

本仓库通过 `maven-publish` 发布到 **Maven Local**（`publishMavenJavaPublicationToMavenLocalRepository`，产物在
`~/.m2/repository`）。在依赖方 `build.gradle` 中：

```gradle
repositories {
    mavenLocal()
}

dependencies {
    // 使用 fg.deobf 以便与 Forge 运行时代码映射一致；版本号与 banira_codex 的 mod_version 一致
    compileOnly fg.deobf("xin.vanilla.banira:banira_codex:${banira_codex_version}")
    // 若需在运行环境带上该 jar（例如独立测试），可改用 implementation / runtimeOnly，并保证游戏内仍安装 banira_codex
}
```

坐标说明（与 `build.gradle` 中 `publishing` 一致）：

| 项            | 值                                                                       |
|--------------|-------------------------------------------------------------------------|
| `groupId`    | `xin.vanilla.banira`（`gradle.properties` 的 `mod_group_id`）              |
| `artifactId` | `banira_codex`                                                          |
| `version`    | `forge-${minecraft_version}-${mod_version}`，例如 `forge-1.16.5-1.0.0.dev` |

未发布到 Maven Local 时，也可将构建出的 jar 放入 `libs/` 并用 `flatDir` 或 `compileOnly files(...)` 引用。

### 1.3 稳定性说明：`internal` 包

源码中 **`xin.vanilla.banira.internal`** 下的类型视为本模组内部实现，**不建议**作为稳定 API 依赖。部分 `common`
层工具类会因实现需要引用 `internal`（例如默认网络通道）；若你希望长期稳定，优先使用下文列出的 **公共入口类**，或在本仓库侧再抽一层正式
API。

---

## 2. 全局入口与运行时状态

| 能力                                  | 说明                                                            |
|-------------------------------------|---------------------------------------------------------------|
| `BaniraCodex.MODID` / `ARTIFACT_ID` | 模组标识                                                          |
| `BaniraCodex.serverInstance()`      | 当前服务端实例与是否运行中的键值封装（`xin.vanilla.banira.common.data.KeyValue`） |
| `BaniraCodex.playerDataManager`     | 本模组自用的 `PlayerDataManager` 实例（见第 4 节自建实例方式）                   |

---

## 3. 配置（Forge TOML + 注解）

**`ForgeConfigAdapter`**（`xin.vanilla.banira.common.config`）从带 **`@Config` / `@ConfigEntry`** 等注解的配置类生成
`ForgeConfigSpec` 并注册到 Forge，支持嵌套分类、范围、枚举、列表及多语言 Tooltip 等。配置类需实现接口以便生成 **fluent 代理**
（详见类内 JavaDoc 示例）。

- 注册：`ForgeConfigAdapter.register(YourConfig.class, yourModId)`
- 读取/写入：`YourConfig.get()` 或通过 `ForgeConfigAdapter.getHolder(...)` 拿到 `ConfigHolder` 做 GUI 等扩展
- 多配置汇总：**`ConfigRegistry`** 可按配置名获取已注册的 `ConfigHolder`

与 **配置同步** 相关的网络包在 `xin.vanilla.banira.common.network.packet`（如 `ConfigSyncToServer`、
`ConfigSnapshotToClient` 等），若依赖方扩展自己的配置同步逻辑，需自行评估是否与现有通道冲突。

---

## 4. 事件与调度

### 4.1 `BaniraEventBus`（`xin.vanilla.banira.common.util`）

对 Forge 常用事件做分类封装，避免在业务代码里散落 `@SubscribeEvent`。支持：

- **服务端**：启动、启动完成、停止、每 tick 等
- **玩家**：登录、登出、维度切换、克隆、任意 `PlayerEvent`、存档等
- **世界**：保存、区块保存、卸载、世界 tick 等
- **实体**：加入世界、传送（`EntityTeleportEvent`）等
- **交互**：右键物品 / 方块 / 指定实体
- **指令**：`RegisterCommandsEvent`
- **Mod 生命周期**：`FMLCommonSetupEvent`、`FMLClientSetupEvent`（类同时注册在 Mod 总线）
- **客户端**：GUI 切换、纹理重载、DrawScreen 后、Overlay 前/后、客户端 tick、聊天、任意 `GuiScreenEvent` 等
- **`Registration`**：部分 API 可取消注册

类注释中含完整使用示例。

### 4.2 `BaniraScheduler`

- 服务端：`BaniraScheduler.schedule(server, delayTicks, runnable)`
- 客户端：`BaniraScheduler.schedule(delayTicks, runnable)`（仅客户端 Dist）

在对应侧 **tick 末尾** 按预定 tick 执行，适合延迟任务。

---

## 5. 玩家数据（按后缀隔离的 NBT）

**`PlayerDataManager`** 在 `world/playerdata` 下按 **UUID + 后缀** 持久化 `CompoundNBT`，带文件锁与脏标记，可与服务器启停、玩家存档等钩子配合（可参考
`BaniraCodex` 内对 `BaniraEventBus` 的注册方式）。

- 多模组共用：使用 **`getOrCreateInstance(Supplier<Path> playerDataDir, String modId, String suffix)`** 为不同后缀创建独立管理器，避免与
  `BaniraCodex.playerDataManager` 冲突。
- API 要点：`getOrCreate(UUID)`、`saveToDisk`、`markDirty` 等（见类文档）。

---

## 6. 网络

### 6.1 自建通道

**`NetworkHandler.create(channelName, IIdentifier)`**（`xin.vanilla.banira.common.network`）封装 `SimpleChannel` 创建；依赖方可实现
**`IIdentifier`**（参考根包 **`Identifier`**：基于 `BaniraCodex.MODID` 的 `ResourceLocation` 工厂），用 **自己的 modId**
实现以避免通道 ID 冲突。

- 普通包：`handler.register(...)`
- 大包分片：**`registerSplit`**，配合 **`SplitPacket`** 与 **`PacketUtils.sendSplitPacket*`**（
  `xin.vanilla.banira.common.util`）

### 6.2 与本模组主通道的协作（注意 ID 冲突）

**`RequestToBoth`** 使用整型 `requestType` 静态注册处理器。本仓库在 `NetworkInit` 中已占用 **1～3**（进度、维度、群系数据同步）。若依赖方要向
**同一通道**注册新请求类型，必须选用 **未占用的 ID**，并与本模组维护者约定，避免冲突。

客户端请求进度/维度/群系缓存可参考 **`AdvancementUtils`**、**`DimensionUtils`**、**`BiomeUtils`** 中的 `ensure*` /
`request*FromServer` 等（实现上会走上述请求 ID）。

---

## 7. 文本、消息与通知

### 7.1 `Component`

**`xin.vanilla.banira.common.data.Component`** 提供可序列化、可本地化、可带样式与子节点的文本模型，并可转换为聊天 *
*`ITextComponent`**（如 `toChat(lang)`）。适合与 **`Translator`**（玩家/服务端语言）一起使用。

### 7.2 `MessageUtils`

向玩家或全体发送聊天、操作栏、广播 **`Component`**，以及通过本模组网络向客户端推送 **屏幕通知**：

- `sendNotification(ServerPlayerEntity, Component, ...)`
- `broadcastNotification(Component, ...)`

通知在客户端由 **`NotificationManager`** 统一绘制与记录日志。

### 7.3 客户端本地通知

**`NotificationManager.get()`**：`addNotification(...)` 等，仅客户端（`@OnlyIn`）。

---

## 8. 指令与虚拟权限

**`VirtualPermissionManager`** 在服务端（及客户端缓存）维护 **`modId:id`** 形式的权限键集合。

- 扩展自有指令枚举时，实现 **`IVirtualPermissionType`**（`modId()`、`id()`、`op()`、`sort()`），即可与 *
  *`addVirtualPermission` / `setVirtualPermission` / `delVirtualPermission`** 等 API 配合。
- **`CommandUtils.hasVirtualPermission(Entity, IVirtualPermissionType)`** 或 *
  *`hasVirtualPermission(PlayerEntity, String fullPermissionKey)`** 用于 Brigadier 指令中鉴权。

本模组内置的 **`EnumCommandType`** 为香草志自有指令枚举；依赖方应定义 **自己的枚举** 并实现 **`IVirtualPermissionType`**
，不要改枚举 ordinal 破坏存档。

**`ICommandNotify`**：与指令执行通知标记相关的薄接口，供 `CommandUtils` 等使用。

---

## 9. 客户端 GUI

### 9.1 屏幕基类

**`BaniraScreen`**（`xin.vanilla.banira.client.gui`）：控件树、焦点、弹层、鼠标/键盘事件顺序等在类头 JavaDoc 中有说明。子类实现
**`initWidgets()`** 构建界面。

### 9.2 控件

- **`IWidget`**：控件接口
- **`BaseWidget`** 及多种内置控件：`ButtonWidget`、`InputWidget`、`LabelWidget`、`ImageWidget`、`ScrollbarWidget`、
  `SliderWidget`、各类形状与面板等（`xin.vanilla.banira.client.gui.widget`）
- 主题：**`ClientThemeManager`**、屏幕上的 **`BaniraColorConfig` / 季节主题** 字段

更细的约定（包路径、`@Accessors`、`addWidget` vs `addChild`、命中测试等）见仓库内 *
*`.cursor/skills/banira-widget-screen/SKILL.md`**（面向本仓库贡献者，依赖方也可作参考）。

### 9.3 背包快捷操作（仅客户端）

**`QuickActionRegistry.get()`**：在物品栏界面注册图标或「仅列表」项，绑定 **`QuickActionContext`** 回调；可设置 *
*`menuAnchorEntryId`** 作为右键菜单锚点。须在客户端线程注册；详见类内 JavaDoc 示例。

---

## 10. 常用工具类（节选）

以下均位于 `xin.vanilla.banira.common.util`（或注明包名），按需选用：

| 类                                                       | 用途                             |
|---------------------------------------------------------|--------------------------------|
| `PacketUtils`                                           | 向服务端/玩家发送包、分片发送                |
| `JsonUtils`                                             | JSON 与对象转换                     |
| `NBTUtils`                                              | NBT 辅助                         |
| `ItemUtils`                                             | 物品相关                           |
| `StringUtils` / `CollectionUtils` / `RandomStringUtils` | 通用集合与字符串                       |
| `DateUtils`                                             | 日期时间                           |
| `SafeExpressionEvaluator`                               | 安全表达式求值                        |
| `Translator`                                            | 翻译与玩家语言                        |
| `PlayerUtils`                                           | 玩家侧辅助（含与本模组网络协作的扩展，见实现）        |
| `CommandUtils`                                          | Brigadier 参数、维度、配置反射、虚拟权限等指令辅助 |
| `AdvancementUtils`                                      | 客户端进度数据缓存与请求                   |
| `DimensionUtils` / `BiomeUtils`                         | 维度/群系解析、服务端查询、客户端列表同步          |

`xin.vanilla.banira.common.data` 下另有 **`Color`**、**`KeyValue`**、**`CircularList`**、**`ArraySet`** 等轻量数据结构。

---

## 11. 联调建议

1. **版本**：与香草志使用相同的 **MC / Forge 主版本**（见本仓库 `gradle.properties`）。
2. **Lombok**：本模组源码使用 Lombok，依赖方若反编译或混合编译需注意注解处理器。
3. **映射**：开发环境使用 **MCP/官方映射** 时，请通过 **fg.deobf** 或等价方式引用本模组 jar，避免字段名不一致。
4. **网络与配置**：扩展 `RequestToBoth` 或配置同步前，先全局搜索占用情况，避免 ID 或包类型冲突。

---

## 12. 构建与发布本模组

```bash
./gradlew build publishToMavenLocal
```

产物 jar 输出目录见 `build.gradle` 中 `jar.destinationDirectory`（`builds/${mod_version}`）。发布 Maven Local 后，依赖方即可按第
1.2 节坐标引用。

---

若你需要把某条能力提升为「正式 API」（例如完全去掉对 `internal` 的引用或提供事件总线扩展点），建议在本仓库开 issue 或 PR
单独拆出接口类。
