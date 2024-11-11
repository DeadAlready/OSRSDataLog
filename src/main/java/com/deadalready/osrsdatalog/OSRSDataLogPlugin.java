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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarbitComposition;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import okhttp3.*;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
        name = "OSRS DataLog"
)
public class OSRSDataLogPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;
//
//	 @Inject
//	 private ConfigManager configManager;

    @Inject
    private OSRSDataLogConfig config;

    @Inject
    private Gson gson;

    @Inject
    private OkHttpClient okHttpClient;

    private static final int SECONDS_BETWEEN_UPLOADS = 10;
    private static final int SECONDS_BETWEEN_MANIFEST_CHECKS = 1200;

    private String manifestUrl = "";
    private String submitUrl = "";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int VARBITS_ARCHIVE_ID = 14;
    private Map<Integer, VarbitComposition> varbitCompositions = new HashMap<>();

    public static final String CONFIG_GROUP_KEY = "OSRSDataLog";
    // THIS VERSION SHOULD BE INCREMENTED EVERY RELEASE WHERE WE ADD A NEW TOGGLE
    public static final int VERSION = 1;

    private Manifest manifest;
    private Map<PlayerProfile, PlayerData> playerDataMap = new HashMap<>();

    @Provides
    OSRSDataLogConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(OSRSDataLogConfig.class);
    }

    @Override
    public void startUp() {
        manifestUrl = config.getManifestUrl();
        submitUrl = config.getSubmitUrl();
        clientThread.invoke(() -> {
            if (client.getIndexConfig() == null || client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) {
                log.debug("Failed to get varbitComposition, state = {}", client.getGameState());
                return false;
            }
            final int[] varbitIds = client.getIndexConfig().getFileIds(VARBITS_ARCHIVE_ID);
            for (int id : varbitIds) {
                varbitCompositions.put(id, client.getVarbit(id));
            }
            return true;
        });

        checkManifest();
    }

    @Override
    protected void shutDown() {
        log.debug("OSRSDataLog stopped!");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e) {
        if (e.getGroup().equals(CONFIG_GROUP_KEY)) {
            manifestUrl = config.getManifestUrl();
            submitUrl = config.getSubmitUrl();
            checkManifest();
        }
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void submitTask() {
        // TODO: do we want other GameStates?
        if (client.getGameState() != GameState.LOGGED_IN || varbitCompositions.isEmpty()) {
            return;
        }

        if (manifest == null || client.getLocalPlayer() == null) {
            log.debug("Skipped due to bad manifest: {}", manifest);
            return;
        }

        String username = client.getLocalPlayer().getName();
        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);

        PlayerData newPlayerData = getPlayerData();
        PlayerData oldPlayerData = playerDataMap.computeIfAbsent(profileKey, k -> new PlayerData());
        log.info("{}", newPlayerData);
        log.info("{}", oldPlayerData);
        if (newPlayerData.equals(oldPlayerData)) {
            return;
        }

        subtract(newPlayerData, oldPlayerData);
        submitPlayerData(profileKey, newPlayerData, oldPlayerData);
    }

    @Schedule(
            period = SECONDS_BETWEEN_MANIFEST_CHECKS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void manifestTask() {
        if (client.getGameState() == GameState.LOGGED_IN) {
            checkManifest();
        }
    }


    private int getVarbitValue(int varbitId) {
        VarbitComposition v = varbitCompositions.get(varbitId);
        if (v == null) {
            return -1;
        }

        int value = client.getVarpValue(v.getIndex());
        int lsb = v.getLeastSignificantBit();
        int msb = v.getMostSignificantBit();
        int mask = (1 << ((msb - lsb) + 1)) - 1;
        return (value >> lsb) & mask;
    }

    private PlayerData getPlayerData() {
        PlayerData out = new PlayerData();
        for (int varbitId : manifest.varbits) {
            out.varb.put(varbitId, getVarbitValue(varbitId));
        }
        for (int varpId : manifest.varps) {
            out.varp.put(varpId, client.getVarpValue(varpId));
        }
        for (Skill s : Skill.values()) {
            out.level.put(s.getName(), client.getRealSkillLevel(s));
        }
        return out;
    }

    private void subtract(PlayerData newPlayerData, PlayerData oldPlayerData) {
        oldPlayerData.varb.forEach(newPlayerData.varb::remove);
        oldPlayerData.varp.forEach(newPlayerData.varp::remove);
        oldPlayerData.level.forEach(newPlayerData.level::remove);
    }

    private void merge(PlayerData oldPlayerData, PlayerData delta) {
        oldPlayerData.varb.putAll(delta.varb);
        oldPlayerData.varp.putAll(delta.varp);
        oldPlayerData.level.putAll(delta.level);
    }

    private void submitDataToFile(PlayerDataSubmission submission) {
        if (config.writeDestination() == OSRSDataLogConfig.WriteDestinationMode.SERVER) {
            // user wants to send data to server only
            return;
        }
        File logFile = new File(RuneLite.RUNELITE_DIR, "osrsdatalog-" + client.getAccountHash() + ".txt");
        String jsonObjectString = gson.toJson(submission);

        // Write JSON object as a line to the file
        try (FileWriter fileWriter = new FileWriter(logFile.getPath(), true)) {
            fileWriter.write(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " - " + jsonObjectString + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void submitPlayerData(PlayerProfile profileKey, PlayerData delta, PlayerData old) {

        PlayerDataSubmission submission = new PlayerDataSubmission(
                profileKey.getUsername(),
                profileKey.getProfileType().name(),
                delta
        );

        if (config.writeDestination() != OSRSDataLogConfig.WriteDestinationMode.SERVER) {
            submitDataToFile(submission);
        }

        if (config.writeDestination() != OSRSDataLogConfig.WriteDestinationMode.SERVER || !submitUrl.isEmpty()) {
            merge(old, delta);
        }
        if (config.writeDestination() == OSRSDataLogConfig.WriteDestinationMode.FILE || submitUrl.isEmpty()) {
            return;
        }

        Request request = new Request.Builder()
                .url(submitUrl)
                .post(RequestBody.create(JSON, gson.toJson(submission)))
                .build();

        Call call = okHttpClient.newCall(request);
        call.timeout().timeout(3, TimeUnit.SECONDS);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        log.debug("Failed to submit: {}", response.code());
                        return;
                    }
                    merge(old, delta);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void constructManifestFromConfig() {
        int[] varbits = new int[0];
        int[] varps = new int[0];
        if (!config.getManifestVarbits().isEmpty()) {
            varbits = Arrays.stream(config.getManifestVarbits()
                            .replaceAll(" ", "")
                            .split(",")
                    )
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
        if (!config.getManifestVarps().isEmpty()) {
            varps = Arrays.stream(config.getManifestVarps()
                            .replaceAll(" ", "")
                            .split(",")
                    )
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
        manifest = new Manifest(
                varbits,
                varps
        );
    }

    private void checkManifest() {
        if (config.manifestSource() == OSRSDataLogConfig.ManifestSource.LOCAL) {
            constructManifestFromConfig();
            return;
        }
        if (manifestUrl.isEmpty()) {
            return;
        }
        Request request = new Request.Builder()
                .url(manifestUrl)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("Failed to get manifest: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        log.debug("Failed to get manifest: {}", response.code());
                        return;
                    }
                    InputStream in = response.body().byteStream();
                    manifest = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Manifest.class);
                } catch (JsonParseException e) {
                    log.debug("Failed to parse manifest: ", e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
