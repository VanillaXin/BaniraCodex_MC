<div align="center">

| [中文](../README.md) | [English](README_en.md) | [日本語](README_ja.md) |
|:------------------:|:-----------------------:|:-------------------:|

<img src="../assets/logo.png" alt="Banira Codex" width="240" />

# Banira Codex（香草志）

**Minecraft Forge、Fabric、NeoForge 向けのクロスバージョン基盤ライブラリ MOD。**

</div>

---

## 目次

- [Banira Codex](#banira-codex香草志)
    - [目次](#目次)
    - [意味](#意味)
    - [はじめに](#はじめに)
    - [特徴](#特徴)
    - [設定](#設定)
    - [依存 MOD としての導入](#依存-mod-としての導入)
    - [安定した公開 API](#安定した公開-api)
    - [イベントとライフサイクル](#イベントとライフサイクル)
    - [ネットワーク](#ネットワーク)
    - [クライアント機能](#クライアント機能)
    - [データとパス](#データとパス)
    - [バージョン移行](#バージョン移行)
    - [ビルド](#ビルド)
    - [ライセンス](#ライセンス)

## 意味

- **香草 (Vanilla)**: Minecraft のバニラ環境を指し、バニラらしい外観と互換性を保つという方針も表しています。
- **志 (Codex)**: 知識を記録し、集約して整理するもの。
- **香草志 (Banira Codex)**: クロスバージョン MOD 開発に必要な共通機能を集約し、異なるローダーでも安定した方法で利用できるようにする基盤です。

## はじめに

Banira Codex は、他の Minecraft MOD に設定、ネットワーク、イベント、プレイヤーデータ、通知、入力、HUD、GUI などの共通機能を提供します。

1 つの jar ですべての Minecraft バージョンとローダーを同時に支えることは目的としていません。対応する組み合わせごとに独立したブランチと成果物を用意し、依存
MOD には同じ名前、意味、構造の公開 API を提供します。Minecraft バージョンやローダーを切り替えても、通常は業務ロジックで
Forge、Fabric、NeoForge の型を置き換える必要はありません。

## 特徴

- **呼び出し側 API の安定性**: 依存 MOD はローダー固有のイベント、ネットワークコンテキスト、設定型ではなく
  `xin.vanilla.banira.api` を使用します。
- **バージョン別リリース**: ローダーと Minecraft バージョンごとに独立したブランチ、成果物、Maven バージョンを持ちます。
- **明確なローダー境界**: Forge、Fabric、NeoForge の実装は `xin.vanilla.banira.internal.<loader>` に配置されます。
- **安全なクライアント分離**: クライアントイベント、入力、HUD、GUI API は `xin.vanilla.banira.api.client`
  に分離され、専用サーバーはクライアントクラスを読み込みません。
- **実際の差異だけを吸収**: 公開する意味は統一し、ローダーや Minecraft API の実差分だけを各ブランチ内部で処理します。

## 設定

香草志設定エディター、または以下のファイルを直接編集して設定を変更できます。各項目の意味と有効範囲は、ゲーム内ツールチップおよび生成されたファイルのコメントを参照してください。

### 共通ファイル

- Vanilla Xin シリーズ共通設定：`config/vanilla.xin/common_config.json`
- 通知履歴：`config/vanilla.xin/notification_log.json`
- 通知タイプの表示設定：`config/vanilla.xin/notification_type_settings.json`
- クイックアクションの配置：`config/vanilla.xin/quick_action.json`
- Vanilla Xin シリーズのプレイヤーデータ：`world/vanilla.xin/playerdata/*.nbt`

### モジュールファイル

- 共通およびサーバー動作設定：[`config/banira_codex-common.toml`](/config/banira_codex-common.toml)
- クライアント設定：[`config/banira_codex-client.toml`](/config/banira_codex-client.toml)

Banira はローダーに依存しない設定スコープを公開します。

```java
ConfigScope.COMMON
ConfigScope.CLIENT
ConfigScope.SERVER
```

依存 MOD は `BaniraConfigs`、`BaniraConfigViews`、公開設定 holder を使用して独自設定を登録、保存、同期、表示できます。業務コードへローダー固有の設定型を公開する必要はありません。

## 依存 MOD としての導入

### Maven Local

Banira Codex の公開座標は次の形式です。

```text
xin.vanilla.banira:banira_codex:<loader>-<minecraftVersion>-<baniraVersion>
```

例：

```text
xin.vanilla.banira:banira_codex:fabric-1.20.1-1.0.2
```

Maven Local を追加します。

```gradle
repositories {
    mavenLocal()
}
```

現在のローダーに対応する依存形式を使用します。

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

`banira_version` には `forge-1.20.1-1.0.2` のような完全なバージョンを指定します。

実行時には、対応する Banira Codex の jar をクライアントまたはサーバーの `mods` ディレクトリへ導入してください。

### ローダーメタデータ

依存 MOD は、現在のローダーメタデータにも `banira_codex` を宣言してください。

- Forge：`META-INF/mods.toml`
- Fabric：`fabric.mod.json`
- NeoForge：`META-INF/neoforge.mods.toml`

バージョン範囲は実際に使用する Banira Codex と一致させます。

## 安定した公開 API

依存 MOD は `xin.vanilla.banira.api` とそのサブパッケージを優先して使用してください。

| エントリ                                  | 用途                          |
|---------------------------------------|-----------------------------|
| `Banira`                              | 現在のプラットフォームとコアサービスの入口       |
| `BaniraConfigs` / `BaniraConfigViews` | 設定の登録、検索、設定画面               |
| `BaniraNetwork`                       | ローダー非依存のパケット登録と送信           |
| `BaniraEvents` / `BaniraLifecycle`    | サーバー、ワールド、プレイヤー、ライフサイクルイベント |
| `BaniraServer`                        | 現在のサーバー状態への安全なアクセス          |
| `BaniraPlayerData`                    | プレイヤー永続データ                  |
| `BaniraDataPaths`                     | MOD、設定、ワールドのデータパス           |
| `BaniraEnvironment`                   | 物理サイドと実行環境の判定               |
| `BaniraModPresence`                   | 任意 MOD の存在と連携状態             |
| `BaniraVirtualPermissions`            | 仮想権限の登録と確認                  |
| `BaniraNotificationTypes`             | サーバー通知タイプの登録                |

`xin.vanilla.banira.internal` はローダーアダプターと実装詳細です。安定した API ではないため、依存 MOD から import
しないでください。

## イベントとライフサイクル

`BaniraEvents` と `BaniraLifecycle` は次のローダー非依存イベントを提供します。

- common setup
- サーバーの起動、稼働、停止
- ワールドの読み込み、保存、アンロード
- プレイヤーのログイン、ログアウト、複製、ディメンション移動
- サーバー tick とプレイヤーデータ保存

クライアントイベントは `xin.vanilla.banira.api.client.event` に分離されています。

- クライアント setup と tick
- 画面のオープン、描画、クローズ
- キーボード、マウス、チャット入力
- HUD overlay 描画
- テクスチャ再読み込み

登録結果を使用してリスナーを解除できます。推奨公開 API からローダー固有のイベントオブジェクトは露出しません。

## ネットワーク

`BaniraNetwork` は共通チャンネル、パケット登録、方向、コンテキストモデルを提供します。パケットハンドラーは `SimpleChannel`
、Fabric receiver、ローダー固有 buffer に依存する必要がありません。

ネットワーク層は次をサポートします。

- C2S、S2C、双方向メッセージ
- 設定スナップショットと設定同期
- プレイヤーデータ同期
- 大きなパケットの分割、再構築、不正フラグメントの破棄
- チャンネルごとに独立した packet ID

依存 MOD は競合を避けるため、独自の MOD ID とチャンネル名を使用してください。

## クライアント機能

`xin.vanilla.banira.api.client` は次の機能を提供します。

- `BaniraInput`：キーバインド登録と状態取得
- `BaniraKeyPressTracker`：押下、長押し、解放ジェスチャー
- `BaniraMouseClickTracker`：クリック、ダブルクリック、マウスジェスチャー
- `BaniraHudEvents`：経験値バー、経験値テキスト、その他 HUD 要素の割り込みと描画
- `BaniraNotifications`：クライアント通知と通知履歴
- `BaniraLogos`：MOD ロゴの検索と上書き
- `BaniraDrawContext`：クロスバージョン描画コンテキスト

GUI 基盤は `xin.vanilla.banira.client.gui` にあります。クライアント MOD から利用できますが、ローダーと Minecraft の差分は対応する
Banira ブランチが吸収します。

## データとパス

削除済みの静的ディレクトリフィールドに依存しないでください。`BaniraDataPaths` から次を取得できます。

- Banira ルートデータディレクトリ
- MOD 専用データディレクトリ
- ワールドデータディレクトリ
- プレイヤーデータディレクトリ
- クライアントまたはサーバー設定パス

プレイヤー永続データの取得と保存には `BaniraPlayerData` を使用します。依存 MOD は独自の MOD ID またはデータ接尾辞でデータを分離してください。

## バージョン移行

Minecraft バージョンまたはローダーを変更する場合：

1. 対応するブランチから構築された Banira Codex 依存へ置き換えます。
2. `xin.vanilla.banira.api` の呼び出し構造は維持します。
3. 本当にローダー固有 API が必要なコードだけを、依存 MOD 側の `internal.<loader>` に分離します。
4. Banira の loader adapter をコピーしたり、Banira の `internal` に依存したりしないでください。
5. 対象ブランチのクライアント、専用サーバー、ネットワーク smoke フローで実際の動作を確認します。

## ビルド

docs ブランチには共通バッチビルド入口があります。

```bat
scripts\build-all.bat
```

デフォルトでは、ローカルの `forge/*`、`fabric/*`、`neoforge/*` ブランチを動的にすべて構築します。`dev/*`、`maintenance/*`
など他の名前空間は含みません。各ブランチは現在の作業ツリーを切り替えず、detached 一時 worktree で構築されます。Banira
のビルドでは `publishToMavenLocal` も実行します。

Gradle を実行せず、選択されたブランチと JDK 検出だけを確認します。

```bat
scripts\build-all.bat -ListOnly
```

glob 式でブランチを選択できます。

```bat
scripts\build-all.bat -BranchExpression "forge/*"
scripts\build-all.bat -BranchExpression "*/21.1"
scripts\build-all.bat -BranchExpression "forge/*,!forge/16.5"
scripts\build-all.bat -BranchExpression "fabric/18.2"
```

`!` で始まる式は一致するブランチを除外します。以前のパラメーター名 `-Branches` も別名として利用できます。

単一コードブランチでは、従来どおり直接実行できます。

```bash
./gradlew clean test assemble publishToMavenLocal
```

## ライセンス

MIT License

問題や提案がある場合は、Issue または Pull request を作成してください。
