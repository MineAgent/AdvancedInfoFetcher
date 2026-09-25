# MC Advanced Info Fetch (AdvancedInfoFetcher)

Minecraft **26.2**（Fabric）客户端只读信息接口：把玩家状态用纯文本吐出来，方便脚本/命令行读取。

这个模组是从 [mcctl](https://github.com/MineAgent/mcctl) 里拆出来的 `/info` 功能，
独立成一个模组，单独占用 **127.0.0.1:3421**（mcctl 仍然用 3420，两个可以同时装）。

```
GET /            使用说明
GET /info        玩家信息：坐标/方位/生命值/饱食度/饱和度/状态效果
                 （别名 /player、/info.txt）
GET /inventory   背包物品：主背包/副手/盔甲，以及打开中的熔炉/箱子
                 （别名 /inv、/inventory.txt）
GET /msg         聊天信息：自上次请求 /msg 以来聊天栏出现的一切
                 （别名 /chat、/msg.txt）
GET /sound       声音信息：自上次请求 /sound 以来客户端播放的所有声音
                 （别名 /sounds、/sound.txt）
```

```bash
curl http://127.0.0.1:3421/info
curl http://127.0.0.1:3421/inventory
curl http://127.0.0.1:3421/msg
curl http://127.0.0.1:3421/sound
./aifetch info
./aifetch inventory
./aifetch msg
./aifetch sound
```

## 输出格式

`200 text/plain; charset=utf-8`，每行一条。

`GET /info`：

```
玩家：DSH
维度：minecraft:overworld
坐标：-222.94 106.0 103.1
方块：-223 106 103
方位：west
yaw：-266.7
pitch：30.1
选中：3
生命值：20.0
饱食度：18
饱和度：5.0
效果：
minecraft:haste 2 95
minecraft:speed 1 无限
```

| 字段 | 说明 |
| --- | --- |
| `玩家` | 用户名 |
| `维度` | 维度命名空间 ID，例如 `minecraft:overworld` |
| `坐标` | 精确坐标，保留 2 位小数 |
| `方块` | 所在方块坐标（整数） |
| `方位` | `north` / `south` / `east` / `west` |
| `yaw` `pitch` | 朝向角度，保留 1 位小数 |
| `选中` | 快捷栏选中格，1-9 |
| `生命值` | 当前血量，保留 1 位小数（上限 20） |
| `饱食度` | 饥饿值，整数 0-20 |
| `饱和度` | 饱和度，保留 1 位小数 |
| `效果：` | **只有存在状态效果时才出现**，按效果 ID 排序 |
| 效果行 | `<效果ID> <等级> <剩余秒数>`，等级从 1 起；永久效果剩余秒数为 `无限` |

`GET /inventory`（只有打开了容器界面时才追加对应段落，否则这两段完全不出现）：

```
背包：
minecraft:crafting_table 1
minecraft:stone 15
副手：
minecraft:torch 7
盔甲：
minecraft:diamond_helmet 1
熔炉：
类型：minecraft:furnace
原料：
minecraft:raw_iron 8
燃料：
minecraft:coal 4
产物：
空
燃烧：0.85
烧炼：0.45
```

打开箱子 / 大箱子时，同样的位置追加：

```
箱子：
类型：minecraft:generic_9x6
容量：54
1：
minecraft:stone 64
3：
minecraft:iron_ingot 16
40：
minecraft:diamond 3
```

| 字段 | 说明 |
| --- | --- |
| `背包：` | **主背包 + 快捷栏**合并，按命名空间 ID 聚合（同一物品跨格相加），按 ID 排序 |
| `副手：` | 只有副手有物品时才出现 |
| `盔甲：` | 只有盔甲栏有物品时才出现，按 头/胸/腿/脚 顺序，每个有东西的栏位一行 |
| `熔炉：` | **只有打开了熔炉界面时才出现**（熔炉 / 高炉 / 烟熏炉，共用同一个菜单） |
| `类型：`（熔炉） | `minecraft:furnace` / `minecraft:blast_furnace` / `minecraft:smoker` |
| `原料：` `燃料：` `产物：` | 三个槽位；空槽输出 `空`，否则输出 `<命名空间ID> <数量>` |
| `燃烧：` | 当前燃料剩余比例，`0.00`-`1.00`（刚点燃为 1.00） |
| `烧炼：` | 当前物品烧炼进度，`0.00`-`1.00` |
| `箱子：` | **只有打开了箱子界面时才出现**（箱子 / 陷阱箱 / 大箱子 / 木桶，都用 `ChestMenu`） |
| `类型：`（箱子） | 菜单 ID：`minecraft:generic_9x3`（27 格）/ `minecraft:generic_9x6`（54 格）等 |
| `容量：` | 箱子格数，小箱子/木桶 `27`，大箱子 `54` |
| `<槽位号>：` | 槽位从 **1** 开始；**空槽位直接跳过**，不输出任何行 |

* 不做"是不是盔甲"的判断——盔甲栏里有什么就输出什么（戴南瓜就输出 `minecraft:carved_pumpkin 1`）。
* 空栏不输出任何行；`副手：`/`盔甲：`/`效果：` 整段消失。
* 熔炉段 / 箱子段只在对应界面打开时输出；未打开任何容器时 `/inventory` 的输出与之前**完全一致**。
* 整个箱子为空时，`箱子：` 段只有 `类型：` 和 `容量：` 两行（因为空槽位全部跳过）。
* 客户端只有在容器界面打开时才知道容器内容，所以必须先右键打开；潜影盒用的是另一个菜单
  （`ShulkerBoxMenu`），当前不支持（与 craftcmd 一致）。
* 数据在客户端主线程（渲染线程）读取，拿到的是完整一致的快照。

`GET /msg`：

```
[Baritone] Baritone settings file not found, resetting.
<DSH> 你好
已将游戏模式设置为 创造模式
未知或不完整的命令。错误见下
definitely_not_a_command<--[此处]
```

| 字段 | 说明 |
| --- | --- |
| 每行 | 聊天栏里一条消息的原文（`Component#getString()`，去掉样式，不加任何前缀） |
| 顺序 | 按进入聊天栏的先后顺序 |
| 范围 | **只包含上一次 `GET /msg` 之后新出现的消息**，读取即清空 |
| 内容 | 聊天栏里的一切：玩家聊天、指令输出、`[Baritone]` 等模组输出、报错、加入/退出与死亡提示 |
| 换行 | 消息自带的换行会被转义成字面量 `\n`，所以**每条消息严格占一行**，与客户端日志里的 `[CHAT]` 行为一致 |
| 空结果 | 没有新消息时返回 `200` + 空正文 |
| 溢出 | 缓存上限 16384 条；若一直没人读取导致溢出，最早的会被丢弃，下一次输出第一行是 `注意：消息过多，缓冲区已丢弃 <数量> 条早期消息` |

* 想只拿增量就定时轮询 `/msg`；两次 `GET /msg` 之间不会重复，也不会丢（除非溢出）。
* `HEAD /msg` 返回 `405`：`HEAD` 会先取走消息再丢弃正文，所以干脆拒绝。
* 只管聊天栏；隐藏式字幕（辅助功能里的声音字幕）、动作栏（overlay）提示与 `/title` 标题不在这里。
* 抓取点挂在 `ChatComponent#addMessage` 上（Mixin）：原版三个入口
  `addClientSystemMessage` / `addServerSystemMessage` / `addPlayerMessage` 都会汇聚到这里，
  所以指令反馈、模组输出、报错一个不漏，并且**只记一次**。

`GET /sound`：

```
minecraft:block.stone.break 1.00 0.80
minecraft:entity.player.step 0.30 1.10
minecraft:block.stone.break 1.00 1.05
minecraft:block.stone.place 1.00 0.90
```

| 字段 | 说明 |
| --- | --- |
| `<声音ID>` | 声音事件的命名空间 ID（`SoundInstance#getIdentifier()`），例如 `minecraft:block.stone.break` |
| `<音量>` | 该声音实例请求的音量（`SoundInstance#getVolume()`），保留 2 位小数 |
| `<音高>` | 该声音实例请求的音高（`SoundInstance#getPitch()`），保留 2 位小数 |
| 顺序 | 按实际播放的先后顺序，**每播放一次一行**（同一个声音播放两次就是两行） |
| 范围 | **只包含上一次 `GET /sound` 之后新播放的声音**，读取即清空 |
| 空结果 | 没有新声音时返回 `200` + 空正文 |
| 溢出 | 缓存上限 16384 条；溢出时最早的会被丢弃，下一次输出第一行是 `注意：声音过多，缓冲区已丢弃 <数量> 条早期声音` |

* `<音量>` 是**请求音量**，不会因为距离变远而变小，也不含声音设置里分类音量的影响；
  想知道"大概多远"要用别的信息，这个数值本身不反映衰减。
* 只记录声音引擎**真正开始播放**的音效（含 `STARTED_SILENTLY`）：未知音效、空音效、
  音频未加载完成、以及音量算出为 0 而被跳过的都不会出现。
* 环境音（`minecraft:ambient.*`）、脚步（`minecraft:entity.player.step`）出现得很频繁，
  轮询间隔别拉太长，否则一次会读到很多行。
* `HEAD /sound` 返回 `405`，理由和 `/msg` 一样。
* 抓取点挂在 `SoundEngine#play` 上（Mixin），这是 `SoundManager#play`、延迟播放和
  `tickInGameSound` 共同的汇聚点，所以每个真正播放的声音**只记一次**。
* 整个模组仍然**不依赖 Fabric API**，只用 Fabric Loader 自带的 Mixin。

## 构建 / 安装

需要 JDK 25（Minecraft 26.2 要求）。26.1 起官方代码不再混淆，所以 Loom 不需要 mappings 配置。

```bash
./gradlew build      # 产物: build/libs/advanced-info-fetch-1.5.0.jar
```

把 jar 放进 `.minecraft/mods/`，启动后日志里会有：

```
advanced-info-fetch listening on http://127.0.0.1:3421
```

不需要 Fabric API，只要 Fabric Loader 0.19.5+；`/msg` 和 `/sound` 的抓取用 Mixin，
Mixin 由 Fabric Loader 自带（`advanced-info-fetch.mixins.json`）。

## 目录

```
src/main/java/com/example/aif/
  AdvancedInfoFetchMod.java  Fabric 客户端入口, 启动 3421 端口服务
  InfoServer.java            HTTP 服务 (只读, 只接受 GET/HEAD)
  InfoProvider.java          数据来源抽象 (info / inventory / messages / sounds)
  PlayerInfoProvider.java    读取玩家坐标/方位/生命值/效果/背包/熔炉/箱子 (Minecraft 相关代码都在这里)
  LineBuffer.java            有界线程安全行缓冲: push/drain, 溢出提示 (无 Minecraft 依赖)
  ChatLog.java               聊天记录缓冲: 抓取方 push, GET /msg drain
  SoundLog.java              声音记录缓冲: 每行 "声音ID 音量 音高", GET /sound drain
  Help.java                  GET / 返回的使用说明
src/main/java/com/example/aif/mixin/
  ChatComponentMixin.java    注入 ChatComponent#addMessage, 把每条聊天栏消息交给 ChatLog
  SoundEngineMixin.java      注入 SoundEngine#play, 把每个真正播放的声音交给 SoundLog
src/main/resources/
  advanced-info-fetch.mixins.json  Mixin 配置
tools/VerifyServer.java      脱离游戏验证 HTTP 层 (假数据源)
aifetch                      命令行封装脚本
```

脱离游戏验证 HTTP 层：

```bash
javac --release 25 -encoding UTF-8 -d /tmp/aif-verify \
  src/main/java/com/example/aif/{InfoProvider,InfoServer,Help,LineBuffer}.java tools/VerifyServer.java
java -cp /tmp/aif-verify VerifyServer
curl http://127.0.0.1:3421/info
curl http://127.0.0.1:3421/inventory
curl http://127.0.0.1:3421/msg
curl http://127.0.0.1:3421/sound
```

## 许可证

LGPL-3.0-only：完整文本见 [`LICENSE`](LICENSE)，其中引用的 GPL-3.0 见 [`LICENSE.GPL-3.0`](LICENSE.GPL-3.0)。
源码文件头部标有 `SPDX-License-Identifier: LGPL-3.0-only`。
