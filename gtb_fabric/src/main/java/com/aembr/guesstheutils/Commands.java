package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.modules.BuilderNotification;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("guesstheutils")
                .then(ClientCommandManager.literal("replay")
                        .executes((command) -> {
                            GuessTheUtils.replay.save();
                            return 1;
                        }))

                .then(ClientCommandManager.literal("livetest")
                        .executes((command) -> {
                            GuessTheUtils.testing = !GuessTheUtils.testing;
                            if (GuessTheUtils.testing) GuessTheUtils.liveE2ERunner.currentTick = 0;
                            return 1;
                        }))

                .then(ClientCommandManager.literal("config")
                        .executes((command) -> {
                            GuessTheUtils.openConfig = true;
                            return 1;
                        }))

                .then(ClientCommandManager.literal("test")
                        .then(ClientCommandManager.argument("module", StringArgumentType.word())
                                .executes((command) -> {
                                    String module = StringArgumentType.getString(command, "module");
                                    runTest(command.getSource(), module);
                                    return 1;
                                }))
                        .executes((command) -> {
                            sendMessage(command.getSource(), "Usage: /guesstheutils test <module>");
                            sendMessage(command.getSource(), "Available tests: cooldown, shortcut, notification");
                            return 1;
                        }))

        );
    }

    private static void runTest(FabricClientCommandSource source, String testType) {
        switch (testType.toLowerCase()) {
            case "cooldown":
                testCooldown(source);
                break;
            case "shortcut":
                testShortcut(source);
                break;
            case "notification":
                testNotification(source);
                break;
            default:
                sendMessage(source, "Unknown test: " + testType);
                sendMessage(source, "Available tests: cooldown, shortcut, notification");
                break;
        }
    }

    private static void testCooldown(FabricClientCommandSource source) {
        if (!GuessTheUtilsConfig.CONFIG.instance().enableChatCooldownModule) {
            sendMessage(source, "Chat Cooldown Timer is disabled. Enable it in the config first.");
            return;
        }

        sendMessage(source, "Testing Chat Cooldown Timer...");

        GuessTheUtils.chatCooldown.enable();
        sendMessage(source, "Cooldown timer enabled - check the screen for timer display");

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                GuessTheUtils.chatCooldown.disable();
                if (GuessTheUtils.CLIENT.player != null) {
                    GuessTheUtils.CLIENT.player.sendMessage(Text.literal("[GTU] Cooldown timer disabled"), false);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void testShortcut(FabricClientCommandSource source) {
        if (!GuessTheUtilsConfig.CONFIG.instance().enableShortcutReminderModule) {
            sendMessage(source, "Shortcut Reminder is disabled. Enable it in the config first.");
            return;
        }

        sendMessage(source, "Testing Shortcut Reminder...");
        sendMessage(source, "Simulating theme update with 'house'");

        GTBEvents.ThemeUpdateEvent testEvent = new GTBEvents.ThemeUpdateEvent("house");
        GuessTheUtils.shortcutReminder.onThemeUpdate(testEvent);
        sendMessage(source, "Theme update sent - check for shortcut reminders in chat");
    }

    private static void testNotification(FabricClientCommandSource source) {
        if (!GuessTheUtilsConfig.CONFIG.instance().enableBuilderNotificationModule) {
            sendMessage(source, "Builder Notification is disabled. Enable it in the config first.");
            return;
        }

        sendMessage(source, "Testing Builder Notification...");
        sendMessage(source, "Sending test notification - you should receive a system notification");

        BuilderNotification.NotificationUtil.sendNotification("Guess the Build Test", "This is a test notification from GuessTheUtils!");
        sendMessage(source, "Test notification sent - check for system notification");
    }

    private static void sendMessage(FabricClientCommandSource source, String message) {
        if (GuessTheUtils.CLIENT.player != null) {
            GuessTheUtils.CLIENT.player.sendMessage(
                GuessTheUtils.prefix.copy().append(Text.literal(message)), false
            );
        }
    }
}