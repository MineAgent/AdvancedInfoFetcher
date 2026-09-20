// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

/**
 * Source of the text returned by {@code GET /info} and {@code GET /inventory}. Implementations
 * decide how the data is gathered; the HTTP layer does not care.
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

	/** @return true when the game is up and the data can be gathered */
	boolean isReady();

	/** @return a human readable reason why {@link #isReady()} is false */
	String unavailableReason();
}
