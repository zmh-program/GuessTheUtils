package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
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
        return "/gtu [reload|status|toggle]";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = (EntityPlayer) sender;
        
        if (args.length == 0) {
            sendMessage(player, "GuessTheUtils v" + GuessTheUtils.VERSION);
            sendMessage(player, "Use /gtu status to see module status");
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                GuessTheUtilsConfig.syncConfig();
                sendMessage(player, "Configuration reloaded!");
                break;
                
            case "status":
                sendMessage(player, "=== GuessTheUtils Status ===");
                sendMessage(player, "Game Tracker: " + getStatus(GuessTheUtilsConfig.enableGameTracker));
                sendMessage(player, "Custom Scoreboard: " + getStatus(GuessTheUtilsConfig.enableCustomScoreboard));
                sendMessage(player, "Chat Cooldown: " + getStatus(GuessTheUtilsConfig.enableChatCooldownTimer));
                sendMessage(player, "Name Autocomplete: " + getStatus(GuessTheUtilsConfig.enableNameAutocomplete));
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
            default:
                sendMessage(player, "Unknown module: " + module);
                break;
        }
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