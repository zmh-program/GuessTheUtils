package com.aembr.guesstheutils;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("guesstheutils")
                .then(ClientCommandManager.literal("replay")
                        .executes((command) -> {
                            GuessTheUtils.replay.save();
                            return 1;
                        }))
        );
    }
}