package briggs.briggsdaily.commands;

import briggs.briggsdaily.BriggsDaily;
import briggs.briggsdaily.Config;
import briggs.briggsdaily.ItemStackSerializer;
import briggs.briggsdaily.Loot;
import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

public class BriggsDailyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("briggsdaily")
                .requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("loot")
                        .then(Commands.literal("add")
                                .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(BriggsDailyCommand::addHolding)
                                                .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                        .executes(BriggsDailyCommand::addLoot)))))
                        .then(Commands.literal("show")
                                .executes(BriggsDailyCommand::showLoot))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(BriggsDailyCommand::deleteLoot)))
                        .then(Commands.literal("edit")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("weight", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                        .executes(BriggsDailyCommand::editLoot)))))
                        .then(Commands.literal("replace")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(BriggsDailyCommand::replaceHolding)
                                        .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                .executes(BriggsDailyCommand::replaceLoot)))))
                .then(Commands.literal("playtime")
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                .executes(BriggsDailyCommand::setPlaytime))));
    }

    static int addLoot(CommandContext<CommandSourceStack> context) {
        ItemInput itemInput = ItemArgument.getItem(context, "item");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");

        Loot.addEntry(itemInput, weight, count, context.getSource().registryAccess());

        context.getSource().sendSuccess(() -> Component.literal("Added loot entry: " + itemInput), false);
        return 1;
    }

    static int addHolding(CommandContext<CommandSourceStack> context) {
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");

        try {
            ItemStack stack = context.getSource().getPlayer().getActiveItem();
            Loot.addEntry(stack, weight, count, context.getSource().registryAccess());
            context.getSource().sendSuccess(() -> Component.literal("Added loot entry: " + stack), false);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Not holding anything"));
        }

        return 1;
    }

    static int showLoot(CommandContext<CommandSourceStack> context) {
        Loot.LootConfig config = Loot.config;

        MutableComponent out = Component.empty();
        int index = 0;
        for (Loot.LootEntryConfig entry : config.entries) {
            ItemStack stack = ItemStackSerializer.decode(entry.stackJson, context.getSource().registryAccess());
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                HoverEvent hover = new HoverEvent.ShowItem(stack);

                Component indexComp = Component.literal(String.format("[%d]", index));
                Component idComp = Component.literal(String.format(" [%s]", id))
                        .setStyle(Style.EMPTY.withHoverEvent(hover).withColor(ChatFormatting.AQUA));
                Component dataComp = Component.literal(String.format(" Weight: %d Count %d", entry.weight, entry.count));

                Component line = Component.empty().append(indexComp).append(idComp).append(dataComp);

                if (index > 0) out.append(Component.literal("\n"));
                out.append(line);
            }
            index++;
        }

        context.getSource().sendSuccess(() -> out, false);
        return 1;
    }

    static int deleteLoot(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");

        Loot.deleteEntry(index, context.getSource().registryAccess());

        context.getSource().sendSuccess(() -> Component.literal("Deleted loot entry: " + index), false);
        return 1;
    }

    static int editLoot(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");

        Loot.editEntry(index, weight, count, context.getSource().registryAccess());

        context.getSource().sendSuccess(() -> Component.literal("Edited loot entry: " + index), false);
        return 1;
    }

    static int replaceLoot(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        ItemInput itemInput = ItemArgument.getItem(context, "item");

        Loot.editEntry(itemInput, index, context.getSource().registryAccess());
        context.getSource().sendSuccess(() -> Component.literal("Edited loot entry: " + index), false);
        return 1;
    }

    static int replaceHolding(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");

        try {
            ItemStack stack = context.getSource().getPlayer().getActiveItem();
            Loot.editEntry(stack, index, context.getSource().registryAccess());
            context.getSource().sendSuccess(() -> Component.literal("Edited loot entry: " + index), false);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Not holding anything"));
        }

        return 1;
    }

    private static int setPlaytime(CommandContext<CommandSourceStack> ctx) {
        int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
        Config.setMinimumPlaytime(minutes);
        ctx.getSource().sendSuccess(() -> Component.literal("Minimum playtime set to " + minutes + " minutes."), false);
        return 1;
    }
}
