// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client entrypoint: starts the read-only info server on 127.0.0.1:3421.
 */
public class AdvancedInfoFetchMod implements ClientModInitializer {
	public static final Logger LOG = Logger.getLogger("aif");

	private InfoServer server;

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

		Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "aif-shutdown"));
	}
}
