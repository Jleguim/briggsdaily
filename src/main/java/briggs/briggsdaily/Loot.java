package briggs.briggsdaily;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Loot {
    private static final Path LOOT_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("briggsdaily/loot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class LootEntryConfig {
        public String item;
        public int weight = 1;
        public int count = 1;
    }

    public static class LootConfig {
        public List<LootEntryConfig> entries = new ArrayList<>();
    }

    private static LootConfig config;
    private static LootTable lootTable;

    public static void load(String json) {
        try {
            config = GSON.fromJson(json, LootConfig.class);
            if (config == null) config = new LootConfig();
        } catch (Exception e) {
            e.printStackTrace();
            config = new LootConfig();
        }
        updatePool();
    }

    public static void load() {
        try {
            if (!Files.exists(LOOT_PATH)) {
                Files.createDirectories(LOOT_PATH.getParent());

                LootConfig defaults = new LootConfig();
                LootEntryConfig diamond = new LootEntryConfig();
                diamond.item = "minecraft:diamond";
                diamond.weight = 1;
                diamond.count = 1;
                defaults.entries.add(diamond);

                LootEntryConfig emerald = new LootEntryConfig();
                emerald.item = "minecraft:emerald";
                emerald.weight = 2;
                emerald.count = 1;
                defaults.entries.add(emerald);

                config = defaults;
                save();
            } else {
                try (Reader reader = Files.newBufferedReader(LOOT_PATH)) {
                    config = GSON.fromJson(reader, LootConfig.class);
                    if (config == null) config = new LootConfig();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            config = new LootConfig();
        }

        updatePool();
    }

    public static void updatePool() {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        if (config == null) config = new LootConfig();
        for (LootEntryConfig entry : config.entries) {
            Identifier identifier = Identifier.tryParse(entry.item);
            Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
            if (item == null || item == Items.AIR) {
                BriggsDaily.LOGGER.warn("Invalid loot item identifier: {} (skipping)", entry.item);
                continue;
            }
            pool.add(LootItem.lootTableItem(item)
                    .setWeight(entry.weight)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(entry.count))));
        }
        lootTable = LootTable.lootTable().withPool(pool).build();
    }

    public static void save() {
        try {
            Files.createDirectories(LOOT_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(LOOT_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addEntry(String item, int weight, int count) {
        if (config == null) config = new LootConfig();
        LootEntryConfig entry = new LootEntryConfig();
        entry.item = item;
        entry.weight = weight;
        entry.count = count;
        config.entries.add(entry);
        updatePool();
        save();
    }

    public static void editEntry(String itemId, int index, int newWeight, int newCount) {
        if (config == null) {
            config = new LootConfig();
        } // Find all entries with this item
        List<LootEntryConfig> matches = new ArrayList<>();
        for (LootEntryConfig entry : config.entries) {
            if (itemId.equalsIgnoreCase(entry.item)) {
                matches.add(entry);
            }
        }
        if (index < 0 || index >= matches.size()) {
            return;
        }

        LootEntryConfig target = matches.get(index);
        target.weight = newWeight;
        target.count = newCount;

        updatePool();

        save();
    }

    public static void removeEntry(String itemId, int index) {
        if (config == null) {
            config = new LootConfig();
        }

        List<LootEntryConfig> matches = new ArrayList<>();
        for (LootEntryConfig entry : config.entries) {
            if (itemId.equalsIgnoreCase(entry.item)) {
                matches.add(entry);
            }
        }

        if (index < 0 || index >= matches.size()) {
            return;
        }

        LootEntryConfig target = matches.get(index);
        config.entries.removeIf(entry -> entry.equals(target));

        updatePool();
        save();
    }


    public static LootTable getLootTable() {
        return lootTable;
    }

    public static LootConfig getConfig() {

        return config;
    }
}
