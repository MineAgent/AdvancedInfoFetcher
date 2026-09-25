// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif.mixin;

import com.example.aif.SoundLog;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds {@link SoundLog} with every sound the client actually plays.
 *
 * <p>{@code SoundEngine#play} is the single funnel: {@code SoundManager#play} delegates to it,
 * delayed sounds reach it from {@code tickInGameSound}, and music/UI/ambient all go through it. The
 * injection sits on the way out and records only the instances the engine really started, which
 * keeps unknown, empty, unloaded and muted-to-zero sounds out of the transcript.</p>
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(
			method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)"
					+ "Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
			at = @At("RETURN"))
	private void aif$capture(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (sound == null || sound.getIdentifier() == null) {
			return;
		}

		SoundEngine.PlayResult result = cir.getReturnValue();

		if (result == SoundEngine.PlayResult.STARTED || result == SoundEngine.PlayResult.STARTED_SILENTLY) {
			SoundLog.INSTANCE.add(sound.getIdentifier().toString(), sound.getVolume(), sound.getPitch());
		}
	}
}
