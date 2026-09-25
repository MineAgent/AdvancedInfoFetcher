// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client entrypoint: starts the read-only info server on 127.0.0.1:3421, and shuts it down and
 * ends the JVM when the client exits (so Minecraft's post-main watchdog cannot write a crash
 * report).
 */
public class AdvancedInfoFetchMod implements ClientModInitializer {
	public static final Logger LOG = Logger.getLogger("aif");

	private InfoServer server;
	private boolean stopped;

	@Override
	public void onInitializeClient() {
		server = new InfoServer(new PlayerInfoProvider());

		try {
			server.start();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "advanced-info-fetch could not bind to " + InfoServer.HOST + ":"
					+ InfoServer.PORT + " - is another instance already running?", e);
			return;
		}

		// HttpServer's "HTTP-Dispatcher" thread is not a daemon, and other mods leave non-daemon
		// threads behind as well, so without an explicit exit Minecraft's post-main watchdog would
		// write a crash report 15 s after a normal quit. The watcher stops this server and ends the
		// JVM once the render thread is gone.
		ClientExitWatcher.onClientExit(this::shutdown);
		// Still tear down on a real JVM shutdown (crash, SIGTERM, System.exit, ...).
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "aif-shutdown"));
	}

	/** Idempotent teardown, shared by the client-exit watcher and the JVM shutdown hook. */
	private synchronized void shutdown() {
		if (stopped) {
			return;
		}
		stopped = true;
		server.stop();
	}
}
