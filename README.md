# MC Advanced Info Fetch (AdvancedInfoFetcher)

Minecraft **26.2**（Fabric）客户端只读信息接口：把玩家状态用纯文本吐出来，方便脚本/命令行读取。

这个模组是从 [mcctl](https://github.com/MineAgent/mcctl) 里拆出来的 `/info` 功能，
独立成一个模组，单独占用 **127.0.0.1:3421**（mcctl 仍然用 3420，两个可以同时装）。

```
GET /            使用说明
GET /info        玩家信息：坐标/方位/生命值/饱食度/饱和度/状态效果
                 （别名 /player、/info.txt）
GET /inventory   背包物品：主背包/副手/盔甲，以及打开中的熔炉
                 （别名 /inv、/inventory.txt）
```

```bash
curl http://127.0.0.1:3421/info
curl http://127.0.0.1:3421/inventory
./aifetch info
./aifetch inventory
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

`GET /inventory`（未打开熔炉界面时，下面的 `熔炉：` 段完全不出现）：

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

| 字段 | 说明 |
| --- | --- |
| `背包：` | **主背包 + 快捷栏**合并，按命名空间 ID 聚合（同一物品跨格相加），按 ID 排序 |
| `副手：` | 只有副手有物品时才出现 |
| `盔甲：` | 只有盔甲栏有物品时才出现，按 头/胸/腿/脚 顺序，每个有东西的栏位一行 |
| `熔炉：` | **只有打开了熔炉界面时才出现**（熔炉 / 高炉 / 烟熏炉，共用同一个菜单） |
| `类型：` | `minecraft:furnace` / `minecraft:blast_furnace` / `minecraft:smoker` |
| `原料：` `燃料：` `产物：` | 三个槽位；空槽输出 `空`，否则输出 `<命名空间ID> <数量>` |
| `燃烧：` | 当前燃料剩余比例，`0.00`-`1.00`（刚点燃为 1.00） |
| `烧炼：` | 当前物品烧炼进度，`0.00`-`1.00` |

* 不做"是不是盔甲"的判断——盔甲栏里有什么就输出什么（戴南瓜就输出 `minecraft:carved_pumpkin 1`）。
* 空栏不输出任何行；`副手：`/`盔甲：`/`效果：` 整段消失。
* 熔炉段只在熔炉界面打开时输出；未打开时 `/inventory` 的输出与之前**完全一致**。
* 客户端只有在熔炉界面打开时才知道熔炉内容，所以必须先右键打开熔炉。
* 数据在客户端主线程（渲染线程）读取，拿到的是完整一致的快照。

## 构建 / 安装

需要 JDK 25（Minecraft 26.2 要求）。26.1 起官方代码不再混淆，所以 Loom 不需要 mappings 配置。

```bash
./gradlew build      # 产物: build/libs/advanced-info-fetch-1.2.0.jar
```

把 jar 放进 `.minecraft/mods/`，启动后日志里会有：

```
advanced-info-fetch listening on http://127.0.0.1:3421
```

不需要 Fabric API，只要 Fabric Loader 0.19.5+。

## 目录

```
src/main/java/com/example/aif/
  AdvancedInfoFetchMod.java  Fabric 客户端入口, 启动 3421 端口服务
  InfoServer.java            HTTP 服务 (只读, 只接受 GET/HEAD)
  InfoProvider.java          数据来源抽象 (info / inventory)
  PlayerInfoProvider.java    读取玩家坐标/方位/生命值/效果/背包/熔炉 (Minecraft 相关代码都在这里)
  Help.java                  GET / 返回的使用说明
tools/VerifyServer.java      脱离游戏验证 HTTP 层 (假数据源)
aifetch                      命令行封装脚本
```

脱离游戏验证 HTTP 层：

```bash
javac --release 25 -encoding UTF-8 -d /tmp/aif-verify \
  src/main/java/com/example/aif/{InfoProvider,InfoServer,Help}.java tools/VerifyServer.java
java -cp /tmp/aif-verify VerifyServer
curl http://127.0.0.1:3421/info
curl http://127.0.0.1:3421/inventory
```

## 许可证

LGPL-3.0-only：完整文本见 [`LICENSE`](LICENSE)，其中引用的 GPL-3.0 见 [`LICENSE.GPL-3.0`](LICENSE.GPL-3.0)。
源码文件头部标有 `SPDX-License-Identifier: LGPL-3.0-only`。
