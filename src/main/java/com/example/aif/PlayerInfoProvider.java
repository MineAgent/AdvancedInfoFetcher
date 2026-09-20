// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the local player's state. {@link #info()} reports position, facing, vitals and active
 * effects; {@link #inventory()} reports the carried items.
 *
 * <p>Everything is read on the client (render) thread so the snapshot is consistent, then handed
 * back to the requesting HTTP thread.</p>
 */
public final class PlayerInfoProvider implements InfoProvider {
	private static final Logger LOG = Logger.getLogger("aif");

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	@Override
	public boolean isReady() {
		return mc() != null;
	}

	@Override
	public String unavailableReason() {
		return mc() == null ? "Minecraft client is not running" : "unknown";
	}

	@Override
	public String info() {
		return onClientThread(PlayerInfoProvider::infoSnapshot);
	}

	@Override
	public String inventory() {
		return onClientThread(PlayerInfoProvider::inventorySnapshot);
	}

	/**
	 * Runs {@code snapshot} on the client thread and waits for the result.
	 *
	 * @return the snapshot text, or {@code null} when it could not be produced
	 */
	private static String onClientThread(Supplier<String> snapshot) {
		Minecraft minecraft = mc();
		if (minecraft == null) {
			return null;
		}

		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			if (minecraft.isSameThread()) {
				result.complete(snapshot.get());
			} else {
				minecraft.execute(() -> {
					try {
						result.complete(snapshot.get());
					} catch (Throwable t) {
						result.completeExceptionally(t);
					}
				});
			}
		} catch (RuntimeException e) {
			LOG.log(Level.FINE, "could not queue the snapshot", e);
			return null;
		}

		try {
			return result.get(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException | TimeoutException e) {
			LOG.log(Level.WARNING, "snapshot failed", e);
			return null;
		}
	}

	/** Runs on the client thread: position, facing, vitals and active effects. */
	private static String infoSnapshot() {
		LocalPlayer player = player();
		if (player == null) {
			return null;
		}

		Inventory inventory = player.getInventory();
		BlockPos block = player.blockPosition();

		StringBuilder out = new StringBuilder(256);
		out.append("玩家：").append(player.getScoreboardName()).append('\n');
		out.append("维度：").append(player.level().dimension().identifier()).append('\n');
		out.append("坐标：").append(decimal(player.getX(), 2)).append(' ')
				.append(decimal(player.getY(), 2)).append(' ')
				.append(decimal(player.getZ(), 2)).append('\n');
		out.append("方块：").append(block.getX()).append(' ')
				.append(block.getY()).append(' ').append(block.getZ()).append('\n');
		out.append("方位：").append(player.getDirection().getName()).append('\n');
		out.append("yaw：").append(decimal(player.getYRot(), 1)).append('\n');
		out.append("pitch：").append(decimal(player.getXRot(), 1)).append('\n');
		out.append("选中：").append(inventory.getSelectedSlot() + 1).append('\n');
		out.append("生命值：").append(decimal(player.getHealth(), 1)).append('\n');
		out.append("饱食度：").append(player.getFoodData().getFoodLevel()).append('\n');
		out.append("饱和度：").append(decimal(player.getFoodData().getSaturationLevel(), 1)).append('\n');

		String effects = effectLines(player.getActiveEffects());
		if (!effects.isEmpty()) {
			out.append("效果：\n").append(effects);
		}

		return out.toString();
	}

	/** Runs on the client thread: main inventory, offhand and armor contents. */
	private static String inventorySnapshot() {
		LocalPlayer player = player();
		if (player == null) {
			return null;
		}

		Inventory inventory = player.getInventory();

		StringBuilder out = new StringBuilder(256);
		out.append("背包：\n").append(itemLines(inventory.getNonEquipmentItems(), true));

		String offhand = itemLines(List.of(player.getItemBySlot(EquipmentSlot.OFFHAND)), false);
		if (!offhand.isEmpty()) {
			out.append("副手：\n").append(offhand);
		}

		String armor = itemLines(List.of(
				player.getItemBySlot(EquipmentSlot.HEAD),
				player.getItemBySlot(EquipmentSlot.CHEST),
				player.getItemBySlot(EquipmentSlot.LEGS),
				player.getItemBySlot(EquipmentSlot.FEET)), false);
		if (!armor.isEmpty()) {
			out.append("盔甲：\n").append(armor);
		}

		return out.toString();
	}

	private static LocalPlayer player() {
		Minecraft minecraft = mc();
		return minecraft == null ? null : minecraft.player;
	}

	/**
	 * @param merge {@code true} aggregates every non-empty stack of the same item into one line,
	 *              {@code false} emits one line per non-empty stack (armor/offhand slot order)
	 * @return {@code "<namespace:id> <count>"} lines, or an empty string when everything is empty
	 */
	private static String itemLines(Iterable<ItemStack> stacks, boolean merge) {
		StringBuilder out = new StringBuilder();
		if (merge) {
			Map<String, Integer> counts = new TreeMap<>();
			for (ItemStack stack : stacks) {
				if (stack != null && !stack.isEmpty()) {
					counts.merge(itemId(stack), stack.getCount(), Integer::sum);
				}
			}
			counts.forEach((id, count) -> out.append(id).append(' ').append(count).append('\n'));
		} else {
			for (ItemStack stack : stacks) {
				if (stack != null && !stack.isEmpty()) {
					out.append(itemId(stack)).append(' ').append(stack.getCount()).append('\n');
				}
			}
		}
		return out.toString();
	}

	/**
	 * @return one {@code "<effect id> <level> <seconds left>"} line per active effect, sorted by
	 *         id, or an empty string when the player has no effects
	 */
	private static String effectLines(Iterable<MobEffectInstance> effects) {
		Map<String, MobEffectInstance> sorted = new TreeMap<>();
		for (MobEffectInstance effect : effects) {
			if (effect != null) {
				sorted.put(effectId(effect), effect);
			}
		}

		StringBuilder out = new StringBuilder();
		sorted.forEach((id, effect) -> out.append(id).append(' ')
				.append(effect.getAmplifier() + 1).append(' ')
				.append(effectDuration(effect)).append('\n'));
		return out.toString();
	}

	private static String effectId(MobEffectInstance effect) {
		Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
		return id == null ? "unknown" : id.toString();
	}

	private static String effectDuration(MobEffectInstance effect) {
		return effect.isInfiniteDuration() ? "无限" : String.valueOf(effect.getDuration() / 20);
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static String decimal(double value, int scale) {
		double factor = Math.pow(10, scale);
		return String.valueOf(Math.round(value * factor) / factor);
	}
}
