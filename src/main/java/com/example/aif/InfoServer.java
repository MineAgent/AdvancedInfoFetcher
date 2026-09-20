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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiny read-only HTTP server bound to {@code 127.0.0.1:3421}.
 *
 * <ul>
 *   <li>{@code GET /} returns the manual.</li>
 *   <li>{@code GET /info} returns the player snapshot as plain text.</li>
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

	private void handleInfo(HttpExchange exchange) throws IOException {
		if (!provider.isReady()) {
			respond(exchange, 409, "game not ready: " + provider.unavailableReason() + "\n");
			return;
		}

		String info;
		try {
			info = provider.info();
		} catch (RuntimeException e) {
			LOG.log(Level.WARNING, "player info failed", e);
			respond(exchange, 500, "player info failed: " + e + "\n");
			return;
		}

		if (info == null) {
			respond(exchange, 409, "no world loaded (still on a menu?)\n");
			return;
		}
		respond(exchange, 200, info);
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
