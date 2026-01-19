package briggs.briggsdaily;

import briggs.briggsdaily.commands.BriggsDailyCommand;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import briggs.briggsdaily.commands.ClaimCommand;

public class BriggsDaily implements ModInitializer {
    public static final String MOD_ID = "briggsdaily";
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Initialized!");
        PlayerCacheTracker.load();
        Config.load();
        Loot.load();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            String uid = player.getStringUUID();
            PlayerCacheTracker.updateJoin(uid);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((MinecraftServer server, CloseableResourceManager resourceManager, boolean success) -> {
            if (success) {
                PlayerCacheTracker.load();
                Loot.load();
                Config.load();
                LOGGER.info("Reloaded config, cache and loot!");
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ClaimCommand.register(dispatcher);
            BriggsDailyCommand.register(dispatcher, registryAccess);
        });
    }
}