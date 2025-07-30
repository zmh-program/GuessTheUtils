package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.modules.BuilderNotification;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Commands extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "gtu";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtu [reload|status|toggle|replay|livetest|test]";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = (EntityPlayer) sender;
        
        GuessTheUtils.LOGGER.info("Processing command with " + args.length + " arguments");
        
        if (args.length == 0) {
            sendMessage(player, "GuessTheUtils v" + GuessTheUtils.VERSION);
            sendMessage(player, "Use /gtu status to see module status");
            sendMessage(player, "Available commands: reload, status, toggle, replay, livetest, test");
            return;
        }
        
        GuessTheUtils.LOGGER.info("Executing command: " + args[0]);
        
        switch (args[0].toLowerCase()) {
            case "reload":
                GuessTheUtilsConfig.syncConfig();
                sendMessage(player, "Configuration reloaded!");
                GuessTheUtils.LOGGER.info("Config reloaded by " + player.getName());
                break;
                
            case "status":
                sendMessage(player, "=== GuessTheUtils Status ===");
                sendMessage(player, "Game Tracker: " + getStatus(GuessTheUtilsConfig.enableGameTracker));
                sendMessage(player, "Custom Scoreboard: " + getStatus(GuessTheUtilsConfig.enableCustomScoreboard));
                sendMessage(player, "Chat Cooldown: " + getStatus(GuessTheUtilsConfig.enableChatCooldownTimer));
                sendMessage(player, "Shortcut Reminder: " + getStatus(GuessTheUtilsConfig.enableShortcutReminder));
                sendMessage(player, "Builder Notification: " + getStatus(GuessTheUtilsConfig.enableBuilderNotification));
                break;
                
            case "toggle":
                if (args.length < 2) {
                    sendMessage(player, "Usage: /gtu toggle <module>");
                    return;
                }
                toggleModule(player, args[1]);
                break;
                
            case "replay":
                GuessTheUtils.replay.save();
                sendMessage(player, "Replay saved!");
                break;
                
            case "livetest":
                GuessTheUtils.testing = !GuessTheUtils.testing;
                if (GuessTheUtils.testing) GuessTheUtils.liveE2ERunner.currentTick = 0;
                sendMessage(player, "Live testing: " + (GuessTheUtils.testing ? "Enabled" : "Disabled"));
                break;
                
            case "test":
                if (args.length < 2) {
                    sendMessage(player, "Usage: /gtu test <module>");
                    sendMessage(player, "Available tests: cooldown, shortcut, notification");
                    return;
                }
                runTest(player, args[1]);
                break;
                
            default:
                sendMessage(player, "Unknown command. Use /gtu for help.");
                break;
        }
    }
    
    private void toggleModule(EntityPlayer player, String module) {
        switch (module.toLowerCase()) {
            case "gametracker":
                GuessTheUtilsConfig.enableGameTracker = !GuessTheUtilsConfig.enableGameTracker;
                sendMessage(player, "Game Tracker: " + getStatus(GuessTheUtilsConfig.enableGameTracker));
                break;
            case "scoreboard":
                GuessTheUtilsConfig.enableCustomScoreboard = !GuessTheUtilsConfig.enableCustomScoreboard;
                sendMessage(player, "Custom Scoreboard: " + getStatus(GuessTheUtilsConfig.enableCustomScoreboard));
                break;
            case "chatcooldown":
                GuessTheUtilsConfig.enableChatCooldownTimer = !GuessTheUtilsConfig.enableChatCooldownTimer;
                sendMessage(player, "Chat Cooldown: " + getStatus(GuessTheUtilsConfig.enableChatCooldownTimer));
                break;
            case "shortcutreminder":
                GuessTheUtilsConfig.enableShortcutReminder = !GuessTheUtilsConfig.enableShortcutReminder;
                sendMessage(player, "Shortcut Reminder: " + getStatus(GuessTheUtilsConfig.enableShortcutReminder));
                break;
            case "buildernotification":
                GuessTheUtilsConfig.enableBuilderNotification = !GuessTheUtilsConfig.enableBuilderNotification;
                sendMessage(player, "Builder Notification: " + getStatus(GuessTheUtilsConfig.enableBuilderNotification));
                break;
            default:
                sendMessage(player, "Unknown module: " + module);
                sendMessage(player, "Available modules: gametracker, scoreboard, chatcooldown, shortcutreminder, buildernotification");
                break;
        }
    }
    
    private void runTest(EntityPlayer player, String testType) {
        switch (testType.toLowerCase()) {
            case "cooldown":
                testCooldown(player);
                break;
            case "shortcut":
                testShortcut(player);
                break;
            case "notification":
                testNotification(player);
                break;
            default:
                sendMessage(player, "Unknown test: " + testType);
                sendMessage(player, "Available tests: cooldown, shortcut, notification");
                break;
        }
    }
    
    private void testCooldown(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableChatCooldownTimer) {
            sendMessage(player, "Chat Cooldown Timer is disabled. Enable it first with: /gtu toggle chatcooldown");
            return;
        }
        
        sendMessage(player, "Testing Chat Cooldown Timer...");
        
        GuessTheUtils.chatCooldown.enable();
        sendMessage(player, "Cooldown timer enabled - check the screen for timer display");
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                GuessTheUtils.chatCooldown.disable();
                player.addChatMessage(new ChatComponentText("[GTU] Cooldown timer disabled"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void testShortcut(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableShortcutReminder) {
            sendMessage(player, "Shortcut Reminder is disabled. Enable it first with: /gtu toggle shortcutreminder");
            return;
        }
        
        sendMessage(player, "Testing Shortcut Reminder...");
        sendMessage(player, "Simulating theme update with 'trash can'");
        
        GTBEvents.ThemeUpdateEvent testEvent = new GTBEvents.ThemeUpdateEvent("trash can");
        GuessTheUtils.shortcutReminder.onThemeUpdate(testEvent);
        sendMessage(player, "Theme update sent - check for shortcut reminders in chat");
    }
    
    private void testNotification(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableBuilderNotification) {
            sendMessage(player, "Builder Notification is disabled. Enable it first with: /gtu toggle buildernotification");
            return;
        }
        
        sendMessage(player, "Testing Builder Notification...");
        sendMessage(player, "Sending test notification - you should receive a system notification");
        
        BuilderNotification.NotificationUtil.sendNotification("Guess the Build Test", "This is a test notification from GuessTheUtils!");
        sendMessage(player, "Test notification sent - check for system notification");
    }
    
    private String getStatus(boolean enabled) {
        return enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
    }
    
    private void sendMessage(EntityPlayer player, String message) {
        ChatComponentText prefix = new ChatComponentText("[GTU] ");
        prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
        
        ChatComponentText msg = new ChatComponentText(message);
        msg.getChatStyle().setColor(EnumChatFormatting.WHITE);
        
        prefix.appendSibling(msg);
        player.addChatMessage(prefix);
    }
} 