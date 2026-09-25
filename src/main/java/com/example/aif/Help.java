// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

/**
 * The manual returned for {@code GET /}.
 */
public final class Help {
	private Help() {
	}

	public static String text() {
		return """
				MC Advanced Info Fetch — Minecraft 客户端信息查询 (Fabric, Minecraft 26.2)
				============================================================
				监听地址: http://127.0.0.1:3421

				  GET  /            返回本使用说明
				  GET  /info        返回玩家坐标/方位/生命值/饱食度/饱和度/状态效果 (纯文本, 每行一条)
				                    (别名: /player, /info.txt)
				  GET  /inventory   返回玩家背包/副手/盔甲物品数量, 打开熔炉/箱子时附带其信息
				                    (纯文本, 每行一条; 别名: /inv, /inventory.txt)
				  GET  /msg         返回自上次 GET /msg 以来聊天栏出现的所有信息
				                    (纯文本, 每行一条; 别名: /chat, /msg.txt)
				  GET  /sound       返回自上次 GET /sound 以来客户端播放的所有声音
				                    (纯文本, 每行一条; 别名: /sounds, /sound.txt)
				  GET  /keysnd      同 /sound 但只返回"重要"声音 (过滤脚步/音乐/ambient/ui/天气)
				                    (纯文本, 每行一条; 与 /sound 共用队列, 读取同样清空; 别名: /keysounds, /keysnd.txt)
				  GET  /world       返回当前维度/时间/天数/游戏刻/天气/是否固定时间
				                    (纯文本, 每行一条; 别名: /dimension, /world.txt)

				/info 输出格式
				  玩家：<用户名>
				  坐标：<x> <y> <z>               (保留 2 位小数)
				  方块：<x> <y> <z>               (所在方块坐标)
				  方位：<north|south|east|west>
				  yaw：<角度>
				  pitch：<角度>
				  选中：<1-9>                     (快捷栏选中格)
				  生命值：<血量>                   (保留 1 位小数, 上限 20)
				  饱食度：<0-20>
				  饱和度：<饱和度>                 (保留 1 位小数)
				  效果：                           (仅有效果时出现, 按效果 ID 排序)
				  <效果ID> <等级> <剩余秒数>        (永久效果剩余秒数为"无限")
				  (维度/时间/天气已移到 /world)

				/inventory 输出格式
				  背包：                           (主背包 + 快捷栏, 按物品 ID 聚合)
				  <命名空间ID> <数量>
				  副手：                           (仅副手有物品时出现)
				  <命名空间ID> <数量>
				  盔甲：                           (仅盔甲栏有物品时出现, 按 头/胸/腿/脚 顺序)
				  <命名空间ID> <数量>
				  熔炉：                           (仅在打开了熔炉界面时出现, 未打开则整段消失)
				  类型：<minecraft:furnace|minecraft:blast_furnace|minecraft:smoker>
				  原料：                           (槽位为空时输出"空")
				  <命名空间ID> <数量>
				  燃料：
				  <命名空间ID> <数量>
				  产物：
				  <命名空间ID> <数量>
				  燃烧：<0.00-1.00>                (当前燃料剩余比例)
				  烧炼：<0.00-1.00>                (当前物品烧炼进度)
				  箱子：                           (仅在打开了箱子界面时出现, 未打开则整段消失)
				  类型：<菜单命名空间ID>            (如 minecraft:generic_9x3 / minecraft:generic_9x6)
				  容量：<格数>                     (小箱子/木桶 27, 大箱子 54)
				  <槽位号>：                        (槽位号从 1 开始; 空槽位直接跳过不输出)
				  <命名空间ID> <数量>

				/msg 输出格式
				  <聊天栏原文>                     (每条消息一行, 按出现顺序, 不带任何前缀)
				  注意：消息过多，缓冲区已丢弃 <数量> 条早期消息   (仅当缓冲区溢出时出现在第一行)
				  没有任何新消息时返回空内容 (200, 正文为空)

				/sound 输出格式
				  <声音ID> <音量> <音高>            (每播放一次输出一行, 按播放顺序)
				                                    (音量/音高保留 2 位小数, 例: minecraft:block.stone.break 1.00 0.80)
				  注意：声音过多，缓冲区已丢弃 <数量> 条早期声音   (仅当缓冲区溢出时出现在第一行)
				  没有任何新声音时返回空内容 (200, 正文为空)

				/keysnd 输出格式
				  和 /sound 完全一样的行格式, 但只输出"重要"声音
				  被过滤掉(不输出)的: 脚步(*.step)、音乐(music.* / music_disc.*)、
				                      ambient.*、UI 音效(ui.*)、天气音效(weather.*)
				  其余全部输出, 例如方块破坏/放置、怪物叫声、爆炸、开关门、拾取等
				  与 /sound 共用同一个队列: 谁先读谁拿走, 读完整队清空
				  没有任何新声音时返回空内容 (200, 正文为空)

				/world 输出格式
				  维度：<命名空间ID>
				  时间：<0-23999>                  (主世界时钟的时刻; 0=清晨, 6000=正午, 12000=黄昏, 18000=午夜)
				  天数：<整数>                     (主世界时钟已经过的天数)
				  游戏刻：<总游戏刻>                (世界创建以来的 tick 数)
				  天气：<clear|rain|thunder>
				  固定时间：<true|false>            (true = 该维度没有昼夜循环, 如下界/末地)

				示例
				  curl http://127.0.0.1:3421
				  curl http://127.0.0.1:3421/info
				  curl http://127.0.0.1:3421/inventory
				  curl http://127.0.0.1:3421/msg
				  curl http://127.0.0.1:3421/sound
				  curl http://127.0.0.1:3421/keysnd
				  curl http://127.0.0.1:3421/world
				  ./aifetch info
				  ./aifetch inventory
				  ./aifetch msg
				  ./aifetch sound
				  ./aifetch keysnd
				  ./aifetch world

				返回
				  200  纯文本 (Content-Type: text/plain; charset=utf-8)
				  405  方法不允许 (只支持 GET/HEAD; /msg、/sound、/keysnd 只支持 GET)
				  409  游戏客户端还没启动 / 还没进入世界
				  500  读取玩家信息失败

				注意事项
				  背包是"主背包 + 快捷栏"合并后按命名空间 ID 聚合的总数, 按 ID 排序
				  副手/盔甲/效果三段只有对应内容非空时才输出, 空栏不输出任何行
				  熔炉段只在打开了熔炉界面(熔炉/高炉/烟熏炉)时输出
				  箱子段只在打开了箱子界面(箱子/陷阱箱/大箱子/木桶)时输出
				  未打开任何容器时 /inventory 与之前完全一致
				  客户端只有在容器界面打开时才知道容器内容, 所以必须先右键打开
				  不对盔甲栏做"是不是盔甲"的判断, 栏位里有什么就输出什么
				  维度不再出现在 /info 里, 用 /world 读
				  /world 的时间/天数是主世界时钟; 固定时间=true 的维度没有昼夜循环
				  天气是客户端当前渲染状态(雨/雷等级阈值), /weather 后约 5 秒过渡完才翻转
				  数据在客户端主线程(渲染线程)读取, 保证是完整的一帧快照
				  /msg 返回的是"上一次 GET /msg 之后"新出现的消息, 读取即清空
				  /msg 包含聊天栏里的一切: 聊天、指令输出、Baritone 等模组输出、报错
				  只统计聊天栏; 隐藏式字幕(辅助功能里的声音字幕)和动作栏(overlay)提示不在其中
				  每条消息严格占一行, 消息内自带的换行会转义成 \n
				  消息缓存在内存里, 上限 16384 条, 溢出时丢弃最早的并在下次输出提示
				  /sound 返回的是"上一次 GET /sound 之后"新播放的声音, 读取即清空
				  /sound 记录声音引擎真正开始播放的音效(含静音启动), 不记未播放的
				  <音量> 是声音实例请求的音量, 不随距离衰减, 也不含音量设置的影响
				  环境音/脚步等会频繁出现, 轮询间隔不要拉太长, 否则一次会读到很多行
				  /keysnd 与 /sound 共用队列: 先读的那个拿到全部/重要声音, 另一个就读不到了
				  声音缓存在内存里, 上限 16384 条, 溢出时丢弃最早的并在下次输出提示
				  命令行用法: ./aifetch info | ./aifetch inventory | ./aifetch msg | ./aifetch sound | ./aifetch keysnd | ./aifetch world
				""";
	}
}
