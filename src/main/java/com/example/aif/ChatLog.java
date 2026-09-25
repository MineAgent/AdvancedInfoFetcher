// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Transcript of the client's chat HUD, consumed by {@code GET /msg}.
 *
 * <p>Every message that reaches the chat HUD is appended here (the mixin on
 * {@code ChatComponent#addMessage} does that); {@link #drain()} hands out everything collected
 * since the previous drain and clears the buffer, so {@code GET /msg} reports exactly the messages
 * that arrived since the last {@code GET /msg}.</p>
 *
 * <p>This class deliberately has no Minecraft imports: the mixin runs on the render thread while
 * the HTTP handler drains on a worker thread, so a handful of plain {@code synchronized} methods
 * are enough and the buffer can be exercised without launching the game.</p>
 */
public final class ChatLog {
	/** The one buffer shared by the mixin (producer) and the HTTP handler (consumer). */
	public static final ChatLog INSTANCE = new ChatLog();

	/**
	 * Upper bound on retained messages. A client that never polls would otherwise grow the heap
	 * forever; past the cap the oldest messages are dropped and {@link #drain()} reports how many.
	 */
	public static final int CAPACITY = 16384;

	private final Deque<String> messages = new ArrayDeque<>();
	private long dropped;

	ChatLog() {
	}

	/**
	 * Appends one message, escaping embedded line breaks so the entry always occupies exactly one
	 * line; {@code null} and empty messages are ignored. Called on the client thread.
	 */
	public synchronized void add(String text) {
		if (text == null) {
			return;
		}
		String line = oneLine(text);
		if (line.isEmpty()) {
			return;
		}
		if (messages.size() >= CAPACITY) {
			messages.removeFirst();
			dropped++;
		}
		messages.addLast(line);
	}

	/**
	 * @return every message collected since the previous call, one per line, and clears the buffer;
	 *         an empty string when nothing new arrived
	 */
	public synchronized String drain() {
		StringBuilder out = new StringBuilder(256);

		if (dropped > 0) {
			out.append("注意：消息过多，缓冲区已丢弃 ").append(dropped).append(" 条早期消息\n");
			dropped = 0;
		}

		while (!messages.isEmpty()) {
			out.append(messages.removeFirst()).append('\n');
		}

		return out.toString();
	}

	/**
	 * Keeps one message on one line by escaping embedded breaks to the literal two character
	 * sequences {@code \r} / {@code \n} - the same treatment vanilla gives its {@code [CHAT]} log
	 * line, so {@code GET /msg} can promise exactly one line per message.
	 */
	private static String oneLine(String text) {
		return text.replace("\r", "\\r").replace("\n", "\\n");
	}
}
