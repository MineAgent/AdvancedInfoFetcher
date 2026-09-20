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

				/info 输出格式
				  玩家：<用户名>
				  维度：<命名空间ID>
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

				示例
				  curl http://127.0.0.1:3421
				  curl http://127.0.0.1:3421/info
				  curl http://127.0.0.1:3421/inventory
				  ./aifetch info
				  ./aifetch inventory

				返回
				  200  纯文本 (Content-Type: text/plain; charset=utf-8)
				  405  方法不允许 (只支持 GET/HEAD)
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
				  数据在客户端主线程(渲染线程)读取, 保证是完整的一帧快照
				  命令行用法: ./aifetch info | ./aifetch inventory
				""";
	}
}
