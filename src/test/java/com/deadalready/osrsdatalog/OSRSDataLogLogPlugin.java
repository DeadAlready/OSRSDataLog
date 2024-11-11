package com.deadalready.osrsdatalog;

import ch.qos.logback.classic.Level;
import javax.inject.Singleton;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@PluginDescriptor(
	name = "[Debug] DataSync Logging"
)
public class OSRSDataLogLogPlugin extends Plugin
{

	@Override
	protected void startUp()
	{
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
		((Logger) LoggerFactory.getLogger("com.deadalready.osrsdatasync")).setLevel(Level.DEBUG);
	}

	@Override
	protected void shutDown()
	{
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
	}
}
