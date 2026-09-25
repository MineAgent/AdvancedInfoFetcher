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
GET /world       世界信息：维度/时间/天数/游戏刻/天气
                 （别名 /dimension、/world.txt）
GET /msg         聊天信息：自上次请求 /msg 以来聊天栏出现的一切
                 （别名 /chat、/msg.txt）
GET /sound       声音信息：自上次请求 /sound 以来客户端播放的所有声音
                 （别名 /sounds、/sound.txt）
GET /keysnd      重要声音：同 /sound，但过滤掉脚步/音乐/ambient/ui/天气
                 （与 /sound 共用队列，读取同样清空；别名 /keysounds、/keysnd.txt）
```

```bash
curl http://127.0.0.1:3421/info
curl http://127.0.0.1:3421/inventory
curl http://127.0.0.1:3421/world
curl http://127.0.0.1:3421/msg
curl http://127.0.0.1:3421/sound
curl http://127.0.0.1:3421/keysnd
./aifetch info
./aifetch inventory
./aifetch world
./aifetch msg
./aifetch sound
./aifetch keysnd
```

## 输出格式

`200 text/plain; charset=utf-8`，每行一条。

`GET /info`：

```
玩家：DSH
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

> 维度原本在 `/info` 里，1.6.0 起移到 `/world`（和时间、天气放在一起）。

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

`GET /keysnd`：

和 `/sound` 完全一样的行格式，但只输出**重要**声音，把这几类过滤掉：

| 过滤掉的类别 | 规则 |
| --- | --- |
| 脚步 | 路径以 `.step` 结尾（`minecraft:block.stone.step`、`minecraft:entity.zombie.step` 等） |
| 音乐 | `music.*`（含 `music.overworld.*`、`music.nether.*`）、`music_disc.*` |
| 环境音 | `ambient.*` |
| UI 音效 | `ui.*` |
| 天气音效 | `weather.*` |

其余全部保留：破坏/放置方块、挖掘命中、怪物叫声、爆炸、开关门、拾取、受伤等。

* **和 `/sound` 共用同一个队列**：先读的那个把队列清空，另一个就读不到了。
  想要"全部"就只读 `/sound`，想要"重点"就只读 `/keysnd`，不要两个混着读。
* 缓存上限、溢出提示、`HEAD` 返回 `405` 都和 `/sound` 一致。

`GET /world`：

```
维度：minecraft:overworld
时间：6000
天数：12
游戏刻：295000
天气：clear
```

| 字段 | 说明 |
| --- | --- |
| `维度` | 维度命名空间 ID，例如 `minecraft:overworld`（1.6.0 起从 `/info` 移到这里） |
| `时间` | 主世界时钟的时刻，`0`-`23999`；`0`=清晨、`6000`=正午、`12000`=黄昏、`18000`=午夜。**没有昼夜循环的维度（下界、末地）输出 `不可用`** |
| `天数` | 主世界时钟经过的整天数 |
| `游戏刻` | 世界创建以来的总 tick 数（`Level#getGameTime()`） |
| `天气` | `clear` / `rain` / `thunder` |

* 26.2 把旧的 `dayTime` 换成了 world clock 体系；这里读的是**主世界时钟**
  （`Level#getOverworldClockTime()`），它才是驱动昼夜的那个。
* `天气` 取客户端**当前渲染状态**（`isRaining` / `isThundering` 基于雨/雷等级阈值），
  所以 `/weather rain|thunder|clear` 之后有约 5 秒的过渡，不会立刻翻转。
* 和 `/info`、`/inventory` 一样是**快照**（不是增量），每次请求都返回当前值。
* 在下界/末地：`时间` 输出 `不可用`，`天气` 恒为 `clear`；`天数`/`游戏刻` 仍是主世界的计数。

* 整个模组仍然**不依赖 Fabric API**，只用 Fabric Loader 自带的 Mixin。

### 退出时不再写崩溃报告

关游戏时渲染线程返回后，`Main` 会启动一个 post-main 看门狗：15 秒内 JVM 还没结束，它就写一份
`Client shutdown from post-main` 崩溃报告，然后 `System.exit(-8)`。而 JVM 只有**所有非 daemon 线程**都结束后
才会自己退出——`com.sun.net.httpserver` 每个服务都带一个非 daemon 的 `HTTP-Dispatcher` 线程（本模组一个，
装了 mcctl 之类的模组还会再有一个），Baritone 也留着非 daemon 的 worker pool。
JVM 关闭钩子救不了这个场景：JVM 根本没开始关闭，钩子不会执行。

`ClientExitWatcher` 在渲染线程（`Minecraft#getRunningThread()`）上 `join()`，线程结束后先停掉本模组的 HTTP 服务
（`HttpServer#stop(0)`），再显式 `System.exit(0)`。关闭钩子照常执行（Minecraft 自己的那个也在内），
所以看门狗永远不会触发；这时世界早已保存、窗口早已关闭（`exitWorldAndClose()` 在 `main()` 返回前就跑完了），
强制退出不会丢存档。既不依赖 Fabric API，也不用自己实现 HTTP 循环。

> 只装本模组、不装 mcctl 时同样有效（验证时就是只留 aif 一个），所以两个模组各自独立解决这个问题。

## 构建 / 安装

需要 JDK 25（Minecraft 26.2 要求）。26.1 起官方代码不再混淆，所以 Loom 不需要 mappings 配置。

```bash
./gradlew build      # 产物: build/libs/advanced-info-fetch-1.6.2.jar
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
  ClientExitWatcher.java     守候渲染线程, 客户端退出后停掉服务并结束 JVM (消除 post-main 崩溃报告)
  InfoServer.java            HTTP 服务 (只读, 只接受 GET/HEAD)
  InfoProvider.java          数据来源抽象 (info / inventory / messages / sounds / keySounds / world)
  PlayerInfoProvider.java    读取玩家坐标/方位/生命值/效果/背包/熔炉/箱子/世界状态 (Minecraft 相关代码都在这里)
  LineBuffer.java            有界线程安全行缓冲: push / 整体 drain / 过滤 drain, 溢出提示 (无 Minecraft 依赖)
  ChatLog.java               聊天记录缓冲: 抓取方 push, GET /msg drain
  SoundLog.java              声音记录缓冲: 每行 "声音ID 音量 音高", GET /sound 全给, GET /keysnd 过滤
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
curl http://127.0.0.1:3421/world
curl http://127.0.0.1:3421/msg
curl http://127.0.0.1:3421/sound
curl http://127.0.0.1:3421/keysnd
```

## 许可证

LGPL-3.0-only：完整文本见 [`LICENSE`](LICENSE)，其中引用的 GPL-3.0 见 [`LICENSE.GPL-3.0`](LICENSE.GPL-3.0)。
源码文件头部标有 `SPDX-License-Identifier: LGPL-3.0-only`。
