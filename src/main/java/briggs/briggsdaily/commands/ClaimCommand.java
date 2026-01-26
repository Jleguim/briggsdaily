package briggs.briggsdaily.commands;

import briggs.briggsdaily.Config;
import briggs.briggsdaily.Loot;
import briggs.briggsdaily.PlayerTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        boolean isOp = player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN);
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

        if (Permissions.check(context.getSource(), "group.donator")) {
            if (data.claims < 2) {
                claimDaily(context, player);
                return 1;
            }
        }

        Instant lastTime = Instant.parse(data.lastClaim);
        ZoneId zone = ZoneId.systemDefault();

        LocalDate lastDate = lastTime.atZone(zone).toLocalDate();
        LocalDate currentDate = LocalDate.now(zone);

        if (!currentDate.equals(lastDate)) {
            PlayerTracker.resetClaims(player.getStringUUID());
            claimDaily(context, player);
            return 1;
        }

        LocalDate nextDate = lastDate.plusDays(1);
        Instant nextClaim = nextDate.atStartOfDay(zone).toInstant();
        Duration untilNextClaim = Duration.between(Instant.now(), nextClaim);
        context.getSource().sendFailure(Component.literal(String.format("Podrás claimear en %d hora(s).", untilNextClaim.toHours())));
        return 1;
    }
}