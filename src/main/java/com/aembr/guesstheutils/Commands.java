package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.lang.ref.Cleaner;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("guesstheutils")
                .then(ClientCommandManager.literal("replay")
                        .executes((command) -> {
                            GuessTheUtils.replay.save();
                            return 1;
                        }))

//                .then(ClientCommandManager.literal("livetest")
//                        .executes((command) -> {
//                            GuessTheUtils.testing = !GuessTheUtils.testing;
//                            if (GuessTheUtils.testing) GuessTheUtils.liveE2ERunner.currentTick = 0;
//                            return 1;
//                        }))

                .then(ClientCommandManager.literal("config")
                        .executes((command) -> {
                            GuessTheUtils.openConfig = true;
                            return 1;
                        }))

        );
    }
}