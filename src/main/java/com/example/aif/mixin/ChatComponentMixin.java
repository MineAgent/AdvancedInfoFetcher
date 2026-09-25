// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif.mixin;

import com.example.aif.ChatLog;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds {@link ChatLog} with everything that reaches the chat HUD.
 *
 * <p>Vanilla funnels all three public entry points ({@code addClientSystemMessage},
 * {@code addServerSystemMessage}, {@code addPlayerMessage}) through the private
 * {@code addMessage(Component, MessageSignature, GuiMessageSource, GuiMessageTag)}, so a single
 * injection there sees plain chat, server/command feedback, client-side errors and anything a mod
 * (Baritone, for instance) prints. This is the same funnel that produces vanilla's own
 * {@code [CHAT]} log lines.</p>
 *
 * <p>Only the message text matters here; {@link ChatLog} turns it into the one-line-per-message
 * form that {@code GET /msg} promises.</p>
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Inject(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
					+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
					+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
			at = @At("HEAD"))
	private void aif$capture(Component content, MessageSignature signature, GuiMessageSource source,
			GuiMessageTag tag, CallbackInfo ci) {
		if (content != null) {
			ChatLog.INSTANCE.add(content.getString());
		}
	}
}
