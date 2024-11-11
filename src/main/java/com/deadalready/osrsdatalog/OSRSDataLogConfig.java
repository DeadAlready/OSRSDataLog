/*
 * Copyright (c) 2021, andmcadams
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.deadalready.osrsdatalog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(OSRSDataLogPlugin.CONFIG_GROUP_KEY)
public interface OSRSDataLogConfig extends Config
{
	String DATASYNC_VERSION_KEYNAME = "version";
	enum WriteDestinationMode
	{
		FILE,
		SERVER,
		BOTH
	}
	enum ManifestSource
	{
		LOCAL,
		SERVER
	}

	@ConfigItem(keyName = DATASYNC_VERSION_KEYNAME, name = "Version", description = "The last version of OSRSDataLog used by the player", hidden = true)
	default int dataSyncVersion()
	{
		return OSRSDataLogPlugin.VERSION;
	}

	@ConfigSection(
			name = "External Server",
			description = "Config for external servers",
			position = 0
	)
	String externalServer = "externalServer";

	@ConfigItem(
			keyName = "submitUrl",
			name = "SubmitUrl",
			description = "URL Data is sent to, if empty then written to file",
			position = 0,
			section = externalServer
	)
	default String getSubmitUrl()
	{
		return "";
	}

	@ConfigItem(
			keyName = "submitUrl",
			name = "",
			description = ""
	)
	void setSubmitUrl(String key);

	@ConfigItem(
			keyName = "manifestUrl",
			name = "Manifest URL",
			description = "URL to download manifest, if empty config manifest is used",
			position = 1,
			section = externalServer
	)
	default String getManifestUrl()
	{
		return "";
	}

	@ConfigItem(
			keyName = "manifestUrl",
			name = "",
			description = ""
	)
	void setManifestUrl(String key);


	@ConfigSection(
			name = "Local Manifest",
			description = "Local varbit/varp config",
			position = 1
	)
	String localManifest = "localManifest";

	@ConfigItem(
			keyName = "manifestVarbits",
			name = "Manifest Varbits",
			description = "Comma separated list of varbits to log",
			position = 2,
			section = localManifest
	)
	default String getManifestVarbits()
	{
		return "";
	}

	@ConfigItem(
			keyName = "manifestVarbits",
			name = "",
			description = ""
	)
	void setManifestVarbits(String key);

	@ConfigItem(
			keyName = "manifestVarps",
			name = "Manifest Varplayers",
			description = "Comma separated list of varps to log",
			position = 3,
			section = localManifest
	)
	default String getManifestVarps()
	{
		return "101";
	}

	@ConfigItem(
			keyName = "manifestVarps",
			name = "",
			description = ""
	)
	void setManifestVarps(String key);

	@ConfigItem(
			keyName = "manifestSource",
			name = "Load Manifest from",
			description = "Use local manifest values instead of downloading",
			position = 4
	)
	default ManifestSource manifestSource() {
		return ManifestSource.LOCAL;
	}

	@ConfigItem(
			keyName = "writeDestination",
			name = "Send data to",
			description = "Configures where data should be sent to",
			position = 5
	)
	default WriteDestinationMode writeDestination()
	{
		return WriteDestinationMode.FILE;
	}
}
