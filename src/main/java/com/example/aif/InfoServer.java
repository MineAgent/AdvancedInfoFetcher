// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiny read-only HTTP server bound to {@code 127.0.0.1:3421}.
 *
 * <ul>
 *   <li>{@code GET /} returns the manual.</li>
 *   <li>{@code GET /info} returns position/facing/vitals/effects as plain text.</li>
 *   <li>{@code GET /inventory} returns the carried items as plain text.</li>
 *   <li>{@code GET /msg} returns the chat lines that arrived since the previous call.</li>
 *   <li>{@code GET /sound} returns the sounds played since the previous call.</li>
 * </ul>
 */
public final class InfoServer {
	public static final String HOST = "127.0.0.1";
	public static final int PORT = 3421;

	private static final Logger LOG = Logger.getLogger("aif");

	private final InfoProvider provider;
	private HttpServer server;
	private ExecutorService pool;

	public InfoServer(InfoProvider provider) {
		this.provider = provider;
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress(HOST, PORT), 16);
		server.createContext("/", this::handle);
		pool = Executors.newFixedThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable, "aif-http");
			thread.setDaemon(true);
			return thread;
		});
		server.setExecutor(pool);
		server.start();
		LOG.info("advanced-info-fetch listening on http://" + HOST + ":" + PORT);
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
		if (pool != null) {
			pool.shutdownNow();
			pool = null;
		}
	}

	private void handle(HttpExchange exchange) throws IOException {
		try {
			String method = exchange.getRequestMethod();
			if (!"GET".equals(method) && !"HEAD".equals(method)) {
				exchange.getResponseHeaders().set("Allow", "GET, HEAD");
				respond(exchange, 405, "method not allowed: " + method + " (use GET)\n");
				return;
			}

			if (isInfoPath(exchange.getRequestURI().getPath())) {
				handleInfo(exchange);
				return;
			}
			if (isInventoryPath(exchange.getRequestURI().getPath())) {
				handleInventory(exchange);
				return;
			}
			if (isMessagePath(exchange.getRequestURI().getPath())) {
				handleMessages(exchange);
				return;
			}
			if (isSoundPath(exchange.getRequestURI().getPath())) {
				handleSounds(exchange);
				return;
			}
			respond(exchange, 200, Help.text());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "request failed", e);
			respond(exchange, 500, "internal error: " + e + "\n");
		} finally {
			exchange.close();
		}
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

	private void handleInfo(HttpExchange exchange) throws IOException {
		respondSnapshot(exchange, "player info", provider::info);
	}

	private void handleInventory(HttpExchange exchange) throws IOException {
		respondSnapshot(exchange, "inventory", provider::inventory);
	}

	private void handleMessages(HttpExchange exchange) throws IOException {
		handleDrain(exchange, "chat messages", provider::messages);
	}

	private void handleSounds(HttpExchange exchange) throws IOException {
		handleDrain(exchange, "sound events", provider::sounds);
	}

	/**
	 * Reads and drains one of the since-last-time backlogs ({@code /msg}, {@code /sound}). A
	 * {@code HEAD} is rejected instead of honoured: it would consume the backlog (the drain happens
	 * while building the body) and then throw the body away.
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
