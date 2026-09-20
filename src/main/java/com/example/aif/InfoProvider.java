// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

/**
 * Source of the text returned by {@code GET /info}. Implementations decide how the data is
 * gathered; the HTTP layer does not care.
 */
public interface InfoProvider {
	/**
	 * @return the body to send, or {@code null} when no world/player is loaded
	 */
	String info();

	/** @return true when the game is up and the data can be gathered */
	boolean isReady();

	/** @return a human readable reason why {@link #isReady()} is false */
	String unavailableReason();
}
