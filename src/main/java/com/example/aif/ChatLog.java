// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

/**
 * Transcript of the client's chat HUD, consumed by {@code GET /msg}.
 *
 * <p>Every message that reaches the chat HUD is appended here (the mixin on
 * {@code ChatComponent#addMessage} does that); {@link #drain()} hands out everything collected
 * since the previous drain and clears the buffer, so {@code GET /msg} reports exactly the messages
 * that arrived since the last {@code GET /msg}.</p>
 */
public final class ChatLog {
	/** The one buffer shared by the mixin (producer) and the HTTP handler (consumer). */
	public static final ChatLog INSTANCE = new ChatLog();

	private final LineBuffer buffer = new LineBuffer("消息");

	ChatLog() {
	}

	/**
	 * Appends one message, escaping embedded line breaks so the entry always occupies exactly one
	 * line; {@code null} and empty messages are ignored. Called on the client thread.
	 */
	public void add(String text) {
		if (text != null) {
			buffer.add(oneLine(text));
		}
	}

	/**
	 * @return every message collected since the previous call, one per line, and clears the buffer;
	 *         an empty string when nothing new arrived
	 */
	public String drain() {
		return buffer.drain();
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
