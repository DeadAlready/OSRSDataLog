package com.deadalready.osrsdatalog;

import lombok.Value;
import net.runelite.client.config.RuneScapeProfileType;

@Value
public class PlayerProfile
{
    String username;
    RuneScapeProfileType profileType;
}