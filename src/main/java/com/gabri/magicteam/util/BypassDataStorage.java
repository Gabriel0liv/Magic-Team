package com.gabri.magicteam.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BypassDataStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_PATH = FMLPaths.CONFIGDIR.get().resolve("magicteam_bypass.json");

    public static void save(Set<UUID> bypassedPlayers) {
        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            GSON.toJson(bypassedPlayers, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save Magic-Team bypass data", e);
        }
    }

    public static Set<UUID> load() {
        if (!Files.exists(STORAGE_PATH)) {
            return new HashSet<>();
        }

        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            Set<UUID> loaded = GSON.fromJson(reader, new TypeToken<HashSet<UUID>>(){}.getType());
            return loaded != null ? loaded : new HashSet<>();
        } catch (IOException e) {
            LOGGER.error("Failed to load Magic-Team bypass data", e);
            return new HashSet<>();
        }
    }
}
