package org.leotechs.whitelistdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PlayerCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, UUID> CACHE = new HashMap<>();
    private static Path cacheFile;

    public static void init(Path configDir) {
        cacheFile = configDir.resolve("whitelistdb/player_cache.json");
        load();
    }

    public static void cachePlayer(String name, UUID uuid) {
        CACHE.put(name.toLowerCase(Locale.ROOT), uuid);
        save();
    }

    public static UUID getUuid(String username) {
        return CACHE.get(username.toLowerCase(Locale.ROOT));
    }

    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    private static void load() {
        if (!Files.exists(cacheFile)) return;
        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> raw = GSON.fromJson(reader, type);
            if (raw != null) raw.forEach((k, v) -> CACHE.put(k, UUID.fromString(v)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void save() {
        try {
            Files.createDirectories(cacheFile.getParent());
            Map<String, String> raw = new HashMap<>();
            CACHE.forEach((k, v) -> raw.put(k, v.toString()));
            try (Writer writer = Files.newBufferedWriter(cacheFile)) {
                GSON.toJson(raw, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}