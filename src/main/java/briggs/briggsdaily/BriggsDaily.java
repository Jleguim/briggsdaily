package briggs.briggsdaily;

import briggs.briggsdaily.commands.BriggsDailyCommand;
import briggs.briggsdaily.commands.ClaimCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BriggsDaily implements ModInitializer {
    public static final String MOD_ID = "briggsdaily";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initialized!");

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            PlayerTracker.updateJoin(player.getStringUUID());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Loot.load(server.registryAccess());
            Loot.updatePool(server.registryAccess());
            PlayerTracker.load();
            Config.load();
        });


        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                Loot.load(server.registryAccess());
                Loot.updatePool(server.registryAccess());
                PlayerTracker.load();
                Config.load();
            }
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BriggsDailyCommand.register(dispatcher, registryAccess);
            ClaimCommand.register(dispatcher);
        });
    }
}