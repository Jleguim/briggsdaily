package briggs.briggsdaily.commands;

import briggs.briggsdaily.Config;
import briggs.briggsdaily.Loot;
import briggs.briggsdaily.PlayerTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ClaimCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim").executes(ClaimCommand::run));
    }

    static void claimDaily(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        LootTable table = Loot.lootTable;
        LootParams params = new LootParams.Builder(player.level())
                .create(LootContextParamSets.EMPTY);
        List<ItemStack> results = table.getRandomItems(params);

        MutableComponent out = Component.empty();
        out.append(Component.literal("Claimeaste tu daily! "));

        Component plus = Component.literal("\n+ ")
                .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN));

        for (ItemStack reward : results) {
            HoverEvent hover = new HoverEvent.ShowItem(reward);

            out.append(plus);
            out.append(Component.literal(reward.toString())
                    .setStyle(Style.EMPTY.withHoverEvent(hover).withColor(ChatFormatting.AQUA)));

            player.addItem(reward);
        }

        context.getSource().sendSuccess(() -> out, false);
        PlayerTracker.recordClaim(player.getStringUUID());
    }

    static int run(CommandContext<CommandSourceStack> context) {
        if (Loot.config.entries.isEmpty()) {
            return 1;
        }

        ServerPlayer player = context.getSource().getPlayer();
        assert player != null;

        PlayerTracker.PlayerData data = PlayerTracker.getOrCreate(player.getStringUUID());
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        // isOp =  context.getSource().getServer().getPlayerList().isOp(new NameAndId(player.getGameProfile()));

        if (isOp) {
            claimDaily(context, player);
            return 1;
        }

        Instant now = Instant.now();
        Duration playTime = Duration.between(Instant.parse(data.lastJoin), now);
        Duration minimumPlayTime = Duration.ofMinutes(Config.minimumPlaytime());

        if (playTime.compareTo(minimumPlayTime) < 0) {
            Duration timeLeft = minimumPlayTime.minus(playTime);
            context.getSource().sendFailure(Component.literal(String.format("Necesitas jugar %d minutos mas", timeLeft.toMinutes())));
            return 1;
        }

        if (data.lastClaim.equals("Never")) {
            claimDaily(context, player);
            return 1;
        }

        Instant lastTime = Instant.parse(data.lastClaim);
        Instant okTime = lastTime.plus(1L, ChronoUnit.DAYS);

        if (now.isAfter(okTime)) {
            claimDaily(context, player);
            return 1;
        }

        Duration nextClaim = Duration.between(now, okTime);
        context.getSource().sendFailure(Component.literal(String.format("Podrás claimear en %d hora(s).", nextClaim.toHours())));
        return 1;
    }
}