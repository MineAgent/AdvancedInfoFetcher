// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import java.util.Locale;

/**
 * Transcript of the sounds the client plays, consumed by {@code GET /sound}.
 *
 * <p>Every sound that the sound engine starts is appended here (the mixin on
 * {@code SoundEngine#play} does that); {@link #drain()} hands out everything collected since the
 * previous drain and clears the buffer, so {@code GET /sound} reports exactly the sounds played
 * since the last {@code GET /sound}.</p>
 */
public final class SoundLog {
	/** The one buffer shared by the mixin (producer) and the HTTP handler (consumer). */
	public static final SoundLog INSTANCE = new SoundLog();

	private final LineBuffer buffer = new LineBuffer("声音");

	SoundLog() {
	}

	/**
	 * Appends one played sound. Called on the client thread.
	 *
	 * @param id     the sound event's namespace id, e.g. {@code minecraft:block.stone.break}
	 * @param volume the volume the instance asked for ({@code SoundInstance#getVolume()}); this is
	 *               <em>not</em> reduced by distance or the sound category's volume setting
	 * @param pitch  the pitch the instance asked for ({@code SoundInstance#getPitch()})
	 */
	public void add(String id, float volume, float pitch) {
		if (id == null || id.isEmpty()) {
			return;
		}
		buffer.add(id + " " + decimal(volume) + " " + decimal(pitch));
	}

	/**
	 * @return every sound played since the previous call, one per line, and clears the buffer; an
	 *         empty string when nothing new was played
	 */
	public String drain() {
		return buffer.drain();
	}

	/** @return a fixed 2 decimal rendering, e.g. {@code 1.00} or {@code 0.80} */
	private static String decimal(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
