package briggs.briggsdaily.commands;

import briggs.briggsdaily.BriggsDaily;
import briggs.briggsdaily.Config;
import briggs.briggsdaily.Loot;
import briggs.briggsdaily.PlayerCacheTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
        LootTable table = Loot.getLootTable();
        LootParams params = new LootParams.Builder(player.level())
                .create(LootContextParamSets.EMPTY);
        List<ItemStack> results = table.getRandomItems(params);

        context.getSource().sendSuccess(() -> Component.literal("Claimeaste tu daily!"), false);
        PlayerCacheTracker.recordClaim(player.getStringUUID());

        for (ItemStack reward : results) {
            player.addItem(reward);
        }
    }

    static int run(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        assert player != null;

        PlayerCacheTracker.PlayerData data = PlayerCacheTracker.getOrCreate(player.getStringUUID());
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
