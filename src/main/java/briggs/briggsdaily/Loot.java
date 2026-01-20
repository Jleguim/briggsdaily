package briggs.briggsdaily;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
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
    private static final Path LOOT_PATH = FabricLoader.getInstance().getConfigDir().resolve("briggsdaily/loot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class LootEntryConfig {
        public int weight = 1;
        public int count = 1;
        public JsonElement stackJson;
    }

    public static class LootConfig {
        public List<LootEntryConfig> entries = new ArrayList<>();
    }

    public static LootConfig config;
    public static LootTable lootTable;

    public static void load(RegistryAccess registryAccess) {
        try {
            if (!Files.exists(LOOT_PATH)) {
                Files.createDirectories(LOOT_PATH.getParent());
                config = new LootConfig();
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

        updatePool(registryAccess);
    }

    public static void updatePool(RegistryAccess registryAccess) {
        if (registryAccess == null) {
            BriggsDaily.LOGGER.warn("RegistryAccess not available, skipping loot pool build");
            return;
        }

        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(Config.rewardsPerClaim()));
        if (config == null) config = new LootConfig();

        for (LootEntryConfig entry : config.entries) {
            ItemStack stack = ItemStackSerializer.decode(entry.stackJson, registryAccess);
            if (stack.isEmpty()) {
                BriggsDaily.LOGGER.warn("Decoded empty stack for entry {}, skipping", entry.stackJson);
                continue;
            }

            LootItem.Builder lootItemBuilder = LootItem.lootTableItem(stack.getItem())
                    .setWeight(entry.weight)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(entry.count)));

            stack.getComponents()
                    .forEach(comp -> applyComponent(lootItemBuilder, comp));

            pool.add(lootItemBuilder);
        }

        lootTable = LootTable.lootTable().withPool(pool).build();
    }

    private static <T> void applyComponent(LootItem.Builder builder, TypedDataComponent<T> comp) {
        builder.apply(SetComponentsFunction.setComponent(comp.type(), comp.value()));
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

    public static void addEntry(ItemInput itemInput, int weight, int count, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();
        try {
            ItemStack stack = itemInput.createItemStack(count, false);
            addEntry(stack, weight, count, registryAccess);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void addEntry(ItemStack stack, int weight, int count, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();
        LootEntryConfig entry = new LootEntryConfig();
        entry.weight = weight;
        entry.count = count;
        entry.stackJson = ItemStackSerializer.encode(stack, registryAccess);

        config.entries.add(entry);
        updatePool(registryAccess);
        save();
    }

    public static void deleteEntry(int index, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();

        config.entries.remove(index);

        updatePool(registryAccess);
        save();
    }

    public static void editEntry(ItemStack stack, int index, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();

        LootEntryConfig target = config.entries.get(index);
        target.stackJson = ItemStackSerializer.encode(stack, registryAccess);

        updatePool(registryAccess);
        save();
    }

    public static void editEntry(ItemInput itemInput, int index, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();

        LootEntryConfig target = config.entries.get(index);

        try {
            ItemStack stack = itemInput.createItemStack(target.count, false);
            target.stackJson = ItemStackSerializer.encode(stack, registryAccess);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void editEntry(int index, int weight, int count, RegistryAccess registryAccess) {
        if (config == null) config = new LootConfig();

        LootEntryConfig target = config.entries.get(index);
        target.weight = weight;
        target.count = count;

        updatePool(registryAccess);
        save();
    }

//    public static void editEntry(ItemStack stack, int index, int weight, int count, RegistryAccess registryAccess) {
//        if (config == null) config = new LootConfig();
//
//        LootEntryConfig target = config.entries.get(index);
//        target.weight = weight;
//        target.count = count;
//        target.stackJson = ItemStackSerializer.encode(stack, registryAccess);
//
//        updatePool(registryAccess);
//        save();
//    }
}
