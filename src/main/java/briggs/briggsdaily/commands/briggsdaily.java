package briggs.briggsdaily.commands;

import briggs.briggsdaily.BriggsDaily;
import briggs.briggsdaily.Config;
import briggs.briggsdaily.Loot;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class briggsdaily {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
                Commands.literal("briggsdaily")
                        .requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(Commands.literal("loot")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("json", StringArgumentType.string())
                                                .executes(briggsdaily::setLoot)))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(briggsdaily::addLoot)))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                        .executes(briggsdaily::removeLoot))))
                                .then(Commands.literal("edit")
                                        .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                        .executes(briggsdaily::editLoot))))))
                                .executes(briggsdaily::showLoot))
                        .then(Commands.literal("playtime")
                                .then(Commands.argument("minutes", IntegerArgumentType.integer())
                                        .executes(briggsdaily::setPlaytime)))
        );

    }


    static int showLoot(CommandContext<CommandSourceStack> context) {
        Loot.LootConfig config = Loot.getConfig();
        Map<String, Integer> seenCounts = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        for (Loot.LootEntryConfig entry : config.entries) {
            int matchIndex = seenCounts.getOrDefault(entry.item, 0);
            seenCounts.put(entry.item, matchIndex + 1);
            sb.append("[")
                    .append(entry.item)
                    .append("][")
                    .append(matchIndex)
                    .append("] Weight: ")
                    .append(entry.weight)
                    .append(" Count: ")
                    .append(entry.count)
                    .append("\n");
        }
        String output = sb.isEmpty() ? "No loot entries configured." : sb.toString();
        context.getSource().sendSuccess(() -> Component.literal(output), false);
        return 1;
    }


    static int setLoot(CommandContext<CommandSourceStack> context) {
        String json = StringArgumentType.getString(context, "json");
        BriggsDaily.LOGGER.info(json);
        Loot.load(json);
        BriggsDaily.LOGGER.info(Loot.getConfig().toString());
        Loot.save();
        context.getSource().sendSuccess(() -> Component.literal("Loot config updated."), false);
        return 1;
    }

    static int addLoot(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, "item").getItem();

        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");

        Loot.addEntry(itemId, weight, count);

        context.getSource().sendSuccess(() -> Component.literal("Added loot entry: " + itemId), false);
        return 1;
    }

    static int editLoot(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, "item").getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        int index = IntegerArgumentType.getInteger(context, "index");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");
        Loot.editEntry(itemId, index, weight, count);
        context.getSource().sendSuccess(() -> Component.literal("Edited loot entry " + itemId + " at index " + index), false);
        return 1;
    }


    static int removeLoot(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, "item").getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        int index = IntegerArgumentType.getInteger(context, "index");

        Loot.removeEntry(itemId, index);
        context.getSource().sendSuccess(() -> Component.literal("Removed loot entry: " + itemId), false);
        return 1;
    }

    private static int setPlaytime(CommandContext<CommandSourceStack> ctx) {
        int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
        Config.setMinimumPlaytime(minutes); // implement in Config
        ctx.getSource().sendSuccess(() -> Component.literal("Minimum playtime set to " + minutes + " minutes."), false);
        return 1;
    }
}
