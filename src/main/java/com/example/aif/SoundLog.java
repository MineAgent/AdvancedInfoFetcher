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

	/**
	 * Returns only the noteworthy sounds played since the previous call, but clears the buffer just
	 * like {@link #drain()} does - {@code /sound} and {@code /keysnd} share one queue.
	 *
	 * <p>Noteworthy means everything that is not a footstep ({@code *.step}), music
	 * ({@code music.*}, {@code music_disc.*}), {@code ambient.*}, a UI sound ({@code ui.*}) or a
	 * weather sound ({@code weather.*}).</p>
	 *
	 * @return the noteworthy sounds, one per line, in playback order
	 */
	public String drainImportant() {
		return buffer.drain(SoundLog::isImportant);
	}

	/**
	 * @param line a formatted {@code "<id> <volume> <pitch>"} entry
	 * @return true when the entry is not one of the filtered out categories
	 */
	private static boolean isImportant(String line) {
		String path = pathOf(line);
		return !path.endsWith(".step")
				&& !path.startsWith("music.")
				&& !path.startsWith("music_disc.")
				&& !path.startsWith("ambient.")
				&& !path.startsWith("ui.")
				&& !path.startsWith("weather.");
	}

	/** @return the path part of the entry's sound id, e.g. {@code block.stone.break} */
	private static String pathOf(String line) {
		int space = line.indexOf(' ');
		String id = space < 0 ? line : line.substring(0, space);
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(colon + 1);
	}

	/** @return a fixed 2 decimal rendering, e.g. {@code 1.00} or {@code 0.80} */
	private static String decimal(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
