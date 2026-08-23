<div align="center">

| [中文](../README.md) | [English](README_en.md) | [日本語](README_ja.md) |
|:------------------:|:-----------------------:|:-------------------:|

<img src="../assets/logo.png" alt="Banira Codex" width="320" />

# Banira Codex

**A cross-version library mod for Minecraft Forge, Fabric, and NeoForge.**

</div>

---

## Table of Contents

- [Banira Codex](#banira-codex)
    - [Table of Contents](#table-of-contents)
    - [Meaning](#meaning)
    - [Introduction](#introduction)
    - [Features](#features)
    - [Configuration](#configuration)
    - [Using Banira as a Dependency](#using-banira-as-a-dependency)
    - [Stable Public API](#stable-public-api)
    - [Events and Lifecycle](#events-and-lifecycle)
    - [Networking](#networking)
    - [Client Features](#client-features)
    - [Data and Paths](#data-and-paths)
    - [Version Migration](#version-migration)
    - [Building](#building)
    - [License](#license)

## Meaning

- **Vanilla (香草)**: Refers to unmodified Minecraft and reflects the project's aim to preserve vanilla style and
  compatibility.
- **Codex (志)**: A record that collects and organizes knowledge.
- **Banira Codex (香草志)**: A collection of shared cross-version modding capabilities that lets dependent mods use a
  stable interface across loaders.

## Introduction

Banira Codex provides configuration, networking, events, player data, notifications, input, HUD, and GUI facilities for
other Minecraft mods.

The project does not attempt to support every Minecraft version and loader with one jar. Each supported combination has
its own branch and artifact, while dependent mods use public APIs with consistent names, semantics, and structure.
Switching Minecraft versions or loaders should therefore require little or no loader-specific change in business code.

## Features

- **Stable caller API**: Dependent mods use `xin.vanilla.banira.api` instead of loader event, network context, or
  configuration types.
- **Version-specific releases**: Each loader and Minecraft version has its own branch, artifact, and Maven version.
- **Explicit loader boundary**: Forge, Fabric, and NeoForge implementations live under
  `xin.vanilla.banira.internal.<loader>`.
- **Client-safe separation**: Client events, input, HUD, and GUI APIs live under `xin.vanilla.banira.api.client`;
  dedicated servers do not load client classes.
- **Evidence-based adaptation**: Public semantics stay stable while real loader or Minecraft API differences remain
  inside each branch.

## Configuration

Configuration can be changed through the Banira Codex configuration editor or by editing the files below. Refer to
in-game tooltips and generated comments for the meaning and valid range of each option.

### Shared Files

- Shared Vanilla Xin settings: `config/vanilla.xin/common_config.json`
- Notification history: `config/vanilla.xin/notification_log.json`
- Notification type display settings: `config/vanilla.xin/notification_type_settings.json`
- Quick-action layout: `config/vanilla.xin/quick_action.json`
- Shared player data: `world/vanilla.xin/playerdata/*.nbt`

### Mod Files

- Common and server-behavior Config: [`config/banira_codex-common.toml`](/config/banira_codex-common.toml)
- Client Config: [`config/banira_codex-client.toml`](/config/banira_codex-client.toml)

Banira exposes loader-neutral configuration scopes:

```java
ConfigScope.COMMON
ConfigScope.CLIENT
ConfigScope.SERVER
```

Dependent mods can register, save, synchronize, and display their own configuration through `BaniraConfigs`,
`BaniraConfigViews`, and public configuration holders without exposing loader configuration types to business code.

## Using Banira as a Dependency

### Maven Local

Banira Codex uses the following publication coordinates:

```text
xin.vanilla.banira:banira_codex:<loader>-<minecraftVersion>-<baniraVersion>
```

Example:

```text
xin.vanilla.banira:banira_codex:fabric-1.20.1-1.0.2
```

Add Maven Local:

```gradle
repositories {
    mavenLocal()
}
```

Then use the dependency form for the current loader:

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

`banira_version` must be the complete version, such as `forge-1.20.1-1.0.2`.

At runtime, install the matching Banira Codex jar in the client or server `mods` directory.

### Loader Metadata

Dependent mods should also declare `banira_codex` in their loader metadata:

- Forge: `META-INF/mods.toml`
- Fabric: `fabric.mod.json`
- NeoForge: `META-INF/neoforge.mods.toml`

The declared version range must match the Banira Codex version in use.

## Stable Public API

Dependent mods should prefer `xin.vanilla.banira.api` and its subpackages:

| Entry point                           | Purpose                                         |
|---------------------------------------|-------------------------------------------------|
| `Banira`                              | Current platform and core service entry point   |
| `BaniraConfigs` / `BaniraConfigViews` | Configuration registration, lookup, and screens |
| `BaniraNetwork`                       | Loader-neutral packet registration and sending  |
| `BaniraEvents` / `BaniraLifecycle`    | Server, world, player, and lifecycle events     |
| `BaniraServer`                        | Safe access to current server state             |
| `BaniraPlayerData`                    | Persistent player data                          |
| `BaniraDataPaths`                     | Mod, configuration, and world data paths        |
| `BaniraEnvironment`                   | Physical side and runtime environment checks    |
| `BaniraModPresence`                   | Optional mod presence and integration state     |
| `BaniraVirtualPermissions`            | Virtual permission registration and checks      |
| `BaniraNotificationTypes`             | Server notification type registration           |

`xin.vanilla.banira.internal` contains loader adapters and implementation details. It is not a stable API and must not
be imported by dependent mods.

## Events and Lifecycle

`BaniraEvents` and `BaniraLifecycle` provide loader-neutral events for:

- common setup
- server start, running, and stop
- world load, save, and unload
- player login, logout, clone, and dimension change
- server ticks and player-data saves

Client events live separately under `xin.vanilla.banira.api.client.event` and include:

- client setup and ticks
- screen open, render, and close
- keyboard, mouse, and chat input
- HUD overlay rendering
- texture reload

Registrations can be used to unregister listeners. Loader-native event objects are not exposed through the recommended
public API.

## Networking

`BaniraNetwork` provides common channels, packet registration, directions, and context models. Packet handlers do not
need `SimpleChannel`, Fabric receivers, or loader-native buffers.

The networking layer supports:

- C2S, S2C, and bidirectional messages
- configuration snapshots and synchronization
- player-data synchronization
- large-packet splitting, reassembly, and invalid-fragment rejection
- independent packet IDs for each channel

Dependent mods should use their own mod ID and channel names to avoid collisions.

## Client Features

`xin.vanilla.banira.api.client` provides:

- `BaniraInput`: key binding registration and state queries
- `BaniraKeyPressTracker`: press, hold, and release gestures
- `BaniraMouseClickTracker`: click, double-click, and mouse gestures
- `BaniraHudEvents`: interception and rendering for the experience bar, experience text, and other HUD elements
- `BaniraNotifications`: client notifications and notification history
- `BaniraLogos`: mod logo lookup and overrides
- `BaniraDrawContext`: cross-version drawing context

GUI infrastructure lives under `xin.vanilla.banira.client.gui`. Client mods may use it, while loader and Minecraft
differences remain the responsibility of the corresponding Banira branch.

## Data and Paths

Do not depend on removed static directory fields. Use `BaniraDataPaths` to obtain:

- the Banira root data directory
- a mod-specific data directory
- world data directories
- player data directories
- client or server configuration paths

Use `BaniraPlayerData` to load and save persistent player data. Dependent mods should isolate their data with their own
mod ID or data suffix.

## Version Migration

When changing Minecraft versions or loaders:

1. Replace the dependency with a Banira Codex artifact built from the matching branch.
2. Keep the `xin.vanilla.banira.api` call structure unchanged.
3. Isolate code that genuinely needs loader-native APIs under the dependent mod's own `internal.<loader>`.
4. Do not copy Banira loader adapters or depend on Banira `internal` packages.
5. Validate real behavior with the target branch's client, dedicated-server, and network smoke flows.

## Building

The docs branch provides a shared batch build entry:

```bat
scripts\build-all.bat
```

By default, it dynamically builds all local `forge/*`, `fabric/*`, and `neoforge/*` branches. Other namespaces such as
`dev/*` and `maintenance/*` are excluded. Each branch is built in a detached temporary worktree without switching the
current checkout. Banira builds also run `publishToMavenLocal`.

List selected branches and validate JDK discovery without running Gradle:

```bat
scripts\build-all.bat -ListOnly
```

Select branches with glob expressions:

```bat
scripts\build-all.bat -BranchExpression "forge/*"
scripts\build-all.bat -BranchExpression "*/21.1"
scripts\build-all.bat -BranchExpression "forge/*,!forge/16.5"
scripts\build-all.bat -BranchExpression "fabric/18.2"
```

Expressions beginning with `!` exclude matching branches. The previous parameter name `-Branches` remains available as
an alias.

A single code branch can still be built directly:

```bash
./gradlew clean test assemble publishToMavenLocal
```

## License

MIT License

Issues and pull requests are welcome.
