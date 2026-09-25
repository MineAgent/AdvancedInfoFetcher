// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

/**
 * A bounded, thread-safe queue of text lines with drain-on-read semantics.
 *
 * <p>Producers (mixin callbacks running on the render thread) append; the HTTP handler drains on a
 * worker thread and receives everything appended since the previous drain. Past {@link #CAPACITY}
 * the oldest lines are dropped and the next drain reports how many, so a client that never polls
 * cannot grow the heap forever.</p>
 *
 * <p>No Minecraft imports on purpose: the whole thing can be exercised without launching the
 * game.</p>
 */
public final class LineBuffer {
	/** Upper bound on retained lines shared by every buffer. */
	public static final int CAPACITY = 16384;

	private final String noun;
	private final Deque<String> lines = new ArrayDeque<>();
	private long dropped;

	/**
	 * @param noun what the lines are called in the overflow notice, e.g. {@code 消息} or {@code 声音}
	 */
	LineBuffer(String noun) {
		this.noun = noun;
	}

	/** Appends one line; {@code null} and empty lines are ignored. */
	public synchronized void add(String line) {
		if (line == null || line.isEmpty()) {
			return;
		}
		if (lines.size() >= CAPACITY) {
			lines.removeFirst();
			dropped++;
		}
		lines.addLast(line);
	}

	/**
	 * @return every line appended since the previous call, one per line, and clears the buffer; an
	 *         empty string when nothing new arrived
	 */
	public synchronized String drain() {
		return drain(line -> true);
	}

	/**
	 * Drains the whole buffer but only returns the lines {@code keep} accepts. The buffer is cleared
	 * either way, which is what lets two endpoints ({@code /sound} and {@code /keysnd}) share one
	 * queue and both consume it.
	 *
	 * @param keep predicate deciding which lines are returned
	 * @return the accepted lines, one per line, in arrival order
	 */
	public synchronized String drain(Predicate<String> keep) {
		StringBuilder out = new StringBuilder(256);

		if (dropped > 0) {
			out.append("注意：").append(noun).append("过多，缓冲区已丢弃 ").append(dropped)
					.append(" 条早期").append(noun).append('\n');
			dropped = 0;
		}

		while (!lines.isEmpty()) {
			String line = lines.removeFirst();
			if (keep.test(line)) {
				out.append(line).append('\n');
			}
		}

		return out.toString();
	}
}
