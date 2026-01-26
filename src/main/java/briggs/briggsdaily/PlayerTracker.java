package briggs.briggsdaily;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PlayerTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_PATH = FabricLoader.getInstance().getConfigDir().resolve("briggsdaily/player_cache.json");
    private static PlayerCache data;

    public static class PlayerCache {
        public Map<String, PlayerData> players = new HashMap<>();
    }

    public static class PlayerData {
        public String lastClaim;
        public String lastJoin;
        public int claims;
    }

    public static void load() {
        try (Reader reader = Files.newBufferedReader(CACHE_PATH)) {
            data = GSON.fromJson(reader, PlayerCache.class);
        } catch (IOException e) {
            data = new PlayerCache();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CACHE_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerData getOrCreate(String uuid) {
        return data.players.computeIfAbsent(uuid, id -> {
            PlayerData pd = new PlayerData();
            pd.lastJoin = Instant.now().toString();
            pd.lastClaim = "Never";
            pd.claims = 0;
            return pd;
        });
    }

    public static void resetClaims(String uuid) {
        PlayerData pd = getOrCreate(uuid);
        pd.claims = 0;
        save();
    }

    public static void updateJoin(String uuid) {
        PlayerData pd = getOrCreate(uuid);
        pd.lastJoin = Instant.now().toString();
        save();
    }

    public static PlayerData recordClaim(String uuid) {
        PlayerData pd = getOrCreate(uuid);
        pd.lastClaim = Instant.now().toString();
        pd.claims++;
        save();
        return pd;
    }
}