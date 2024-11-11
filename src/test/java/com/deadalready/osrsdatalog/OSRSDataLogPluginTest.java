package com.deadalready.osrsdatalog;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OSRSDataLogPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OSRSDataLogPlugin.class, OSRSDataLogLogPlugin.class);
		RuneLite.main(args);
	}
}
