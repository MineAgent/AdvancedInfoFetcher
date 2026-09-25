// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

/**
 * Source of the text returned by {@code GET /info}, {@code GET /inventory}, {@code GET /msg},
 * {@code GET /sound}, {@code GET /keysnd} and {@code GET /world}. Implementations decide how the
 * data is gathered; the HTTP layer does not care.
 */
public interface InfoProvider {
	/**
	 * @return the body for {@code GET /info}, or {@code null} when no world/player is loaded
	 */
	String info();

	/**
	 * @return the body for {@code GET /inventory} (main inventory + offhand + armor), or
	 *         {@code null} when no world/player is loaded
	 */
	String inventory();

	/**
	 * Returns the chat messages collected since the previous call and forgets them, which is what
	 * makes {@code GET /msg} report only what is new.
	 *
	 * @return the body for {@code GET /msg}, one message per line, or an empty string when nothing
	 *         new arrived
	 */
	String messages();

	/**
	 * Returns the sounds played since the previous call and forgets them, which is what makes
	 * {@code GET /sound} report only what is new.
	 *
	 * @return the body for {@code GET /sound}, one played sound per line, or an empty string when
	 *         nothing new was played
	 */
	String sounds();

	/**
	 * Like {@link #sounds()} but reports only the noteworthy sounds, sharing the same queue: it
	 * drains whatever {@code /sound} would have drained, so calling either endpoint consumes the
	 * backlog for both.
	 *
	 * @return the body for {@code GET /keysnd}, one noteworthy sound per line, or an empty string
	 *         when nothing noteworthy was played
	 */
	String keySounds();

	/**
	 * @return the body for {@code GET /world} (dimension, time, weather), or {@code null} when no
	 *         world/player is loaded
	 */
	String world();

	/** @return true when the game is up and the data can be gathered */
	boolean isReady();

	/** @return a human readable reason why {@link #isReady()} is false */
	String unavailableReason();
}
