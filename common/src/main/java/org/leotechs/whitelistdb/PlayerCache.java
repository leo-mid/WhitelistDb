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
import java.nio.file.Paths;
import java.util.*;

public class PlayerCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, UUID> NAME_TO_UUID = new HashMap<>();
    private static Path cacheFile;

    /** Initialize with the config directory (e.g. Path.of("config")). */
    public static void init(Path configDir) {
        cacheFile = configDir.resolve("whitelistdb/player_cache.json");
        load();
    }

    /** Convenience overload – uses the default "config" folder. */
    public static void init() {
        init(Paths.get("config"));
    }

    /** Cache a username → UUID mapping (persists to disk). */
    public static void cachePlayer(String username, UUID uuid) {
        NAME_TO_UUID.put(username.toLowerCase(Locale.ROOT), uuid);
        save();
    }

    /** Look up a UUID by username (case-insensitive). Returns null if unknown. */
    public static UUID getUuid(String username) {
        return NAME_TO_UUID.get(username.toLowerCase(Locale.ROOT));
    }

    /** Generate a deterministic offline-mode UUID for a username. */
    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    private static void load() {
        if (!Files.exists(cacheFile)) return;
        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> raw = GSON.fromJson(reader, type);
            if (raw != null) raw.forEach((k, v) -> NAME_TO_UUID.put(k, UUID.fromString(v)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(cacheFile.getParent());
            Map<String, String> raw = new LinkedHashMap<>();
            NAME_TO_UUID.forEach((k, v) -> raw.put(k, v.toString()));
            try (Writer writer = Files.newBufferedWriter(cacheFile)) {
                GSON.toJson(raw, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
