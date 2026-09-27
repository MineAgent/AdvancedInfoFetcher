// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2026 MineAgent

package com.example.aif;

import com.example.httpd.HttpdProvider;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint: mounts the read-only info endpoints under {@code /aif} on MGHttpdProvider's
 * shared server (127.0.0.1:3420).
 */
public class AdvancedInfoFetchMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HttpdProvider.register(InfoEndpoint.PREFIX, InfoEndpoint.NAME, InfoEndpoint.ENDPOINTS,
				new InfoEndpoint(new PlayerInfoProvider()));
	}
}
