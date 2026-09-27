// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import com.example.httpd.HttpdProvider;
import com.example.httpd.PathHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The read-only info endpoints, mounted under the shared server's {@code /aif} prefix.
 *
 * <ul>
 *   <li>{@code GET /aif/} returns the manual.</li>
 *   <li>{@code GET /aif/info} returns position/facing/vitals/effects as plain text.</li>
 *   <li>{@code GET /aif/inventory} returns the carried items as plain text.</li>
 *   <li>{@code GET /aif/msg} returns the chat lines that arrived since the previous call.</li>
 *   <li>{@code GET /aif/sound} returns the sounds played since the previous call.</li>
 *   <li>{@code GET /aif/keysnd} returns only the noteworthy sounds since the previous call.</li>
 *   <li>{@code GET /aif/world} returns dimension, time and weather.</li>
 * </ul>
 *
 * <p>The HTTP server itself (127.0.0.1:3420) belongs to MGHttpdProvider; this class only serves
 * the paths it is handed below the prefix, so {@code /info} here means {@code /aif/info}.</p>
 */
public final class InfoEndpoint implements PathHandler {
	public static final String PREFIX = "/aif";
	public static final String NAME = "AdvancedInfoFetcher — 只读状态 (坐标/背包/聊天/声音/世界)";

	/** What the provider's {@code GET /} index lists for this mod. */
	public static final List<HttpdProvider.Endpoint> ENDPOINTS = List.of(
			new HttpdProvider.Endpoint("GET", "/aif/", "使用说明"),
			new HttpdProvider.Endpoint("GET", "/aif/info", "玩家状态 (别名 /aif/player)"),
			new HttpdProvider.Endpoint("GET", "/aif/inventory", "背包与容器 (别名 /aif/inv)"),
			new HttpdProvider.Endpoint("GET", "/aif/world", "维度/时间/天气 (别名 /aif/dimension)"),
			new HttpdProvider.Endpoint("GET", "/aif/msg", "上次读取后的聊天 (别名 /aif/chat)"),
			new HttpdProvider.Endpoint("GET", "/aif/sound", "上次读取后的声音 (别名 /aif/sounds)"),
			new HttpdProvider.Endpoint("GET", "/aif/keysnd", "上次读取后的重要声音 (别名 /aif/keysounds)"));

	private static final Logger LOG = Logger.getLogger("aif");

	private final InfoProvider provider;

	public InfoEndpoint(InfoProvider provider) {
		this.provider = provider;
	}

	@Override
	public void handle(HttpExchange exchange, String path) throws IOException {
		try {
			String method = exchange.getRequestMethod();
			if (!"GET".equals(method) && !"HEAD".equals(method)) {
				exchange.getResponseHeaders().set("Allow", "GET, HEAD");
				respond(exchange, 405, "method not allowed: " + method + " (use GET)\n");
				return;
			}

			if (isInfoPath(path)) {
				handleInfo(exchange);
				return;
			}
			if (isInventoryPath(path)) {
				handleInventory(exchange);
				return;
			}
			if (isMessagePath(path)) {
				handleMessages(exchange);
				return;
			}
			if (isSoundPath(path)) {
				handleSounds(exchange);
				return;
			}
			if (isKeySoundPath(path)) {
				handleKeySounds(exchange);
				return;
			}
			if (isWorldPath(path)) {
				handleWorld(exchange);
				return;
			}
			respond(exchange, 200, Help.text());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "request failed", e);
			respond(exchange, 500, "internal error: " + e + "\n");
		}
		// the provider closes the exchange
	}

	private static boolean isInfoPath(String path) {
		return "/info".equals(path) || "/info.txt".equals(path) || "/player".equals(path);
	}

	private static boolean isInventoryPath(String path) {
		return "/inventory".equals(path) || "/inventory.txt".equals(path) || "/inv".equals(path);
	}

	private static boolean isMessagePath(String path) {
		return "/msg".equals(path) || "/msg.txt".equals(path) || "/chat".equals(path);
	}

	private static boolean isSoundPath(String path) {
		return "/sound".equals(path) || "/sound.txt".equals(path) || "/sounds".equals(path);
	}

	private static boolean isKeySoundPath(String path) {
		return "/keysnd".equals(path) || "/keysnd.txt".equals(path) || "/keysounds".equals(path);
	}

	private static boolean isWorldPath(String path) {
		return "/world".equals(path) || "/world.txt".equals(path) || "/dimension".equals(path);
	}

	private void handleInfo(HttpExchange exchange) throws IOException {
		respondSnapshot(exchange, "player info", provider::info);
	}

	private void handleInventory(HttpExchange exchange) throws IOException {
		respondSnapshot(exchange, "inventory", provider::inventory);
	}

	private void handleWorld(HttpExchange exchange) throws IOException {
		respondSnapshot(exchange, "world", provider::world);
	}

	private void handleMessages(HttpExchange exchange) throws IOException {
		handleDrain(exchange, "chat messages", provider::messages);
	}

	private void handleSounds(HttpExchange exchange) throws IOException {
		handleDrain(exchange, "sound events", provider::sounds);
	}

	private void handleKeySounds(HttpExchange exchange) throws IOException {
		handleDrain(exchange, "important sounds", provider::keySounds);
	}

	/**
	 * Reads and drains one of the since-last-time backlogs ({@code /msg}, {@code /sound},
	 * {@code /keysnd}). A {@code HEAD} is rejected instead of honoured: it would consume the backlog
	 * (the drain happens while building the body) and then throw the body away.
	 */
	private void handleDrain(HttpExchange exchange, String what, Supplier<String> snapshot)
			throws IOException {
		if ("HEAD".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Allow", "GET");
			respond(exchange, 405, "method not allowed: HEAD would consume the " + what + " (use GET)\n");
			return;
		}
		respondSnapshot(exchange, what, snapshot);
	}

	/**
	 * Runs {@code snapshot} and writes it, mapping the usual failure modes onto status codes.
	 *
	 * @param what label used in the 500 body
	 */
	private void respondSnapshot(HttpExchange exchange, String what, Supplier<String> snapshot)
			throws IOException {
		if (!provider.isReady()) {
			respond(exchange, 409, "game not ready: " + provider.unavailableReason() + "\n");
			return;
		}

		String body;
		try {
			body = snapshot.get();
		} catch (RuntimeException e) {
			LOG.log(Level.WARNING, what + " failed", e);
			respond(exchange, 500, what + " failed: " + e + "\n");
			return;
		}

		if (body == null) {
			respond(exchange, 409, "no world loaded (still on a menu?)\n");
			return;
		}
		respond(exchange, 200, body);
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		if ("HEAD".equals(exchange.getRequestMethod()) || bytes.length == 0) {
			exchange.sendResponseHeaders(status, -1);
			return;
		}
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}
}
