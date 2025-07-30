package com.aembr.guesstheutils.config;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class GuessTheUtilsConfig {
    private static Configuration config;
    
    // Module toggles
    public static boolean enableGameTracker = true;
    public static boolean enableCustomScoreboard = true;
    public static boolean enableChatCooldownTimer = true;
    public static boolean enableNameAutocomplete = true;
    public static boolean enableShortcutReminder = true;
    public static boolean enableBuilderNotification = true;
    
    // Chat cooldown settings
    public static int chatCooldownPingVolume = 50;
    public static boolean chatCooldownTimer = true;
    
    public static void init(File configFile) {
        config = new Configuration(configFile);
        syncConfig();
    }
    
    public static void syncConfig() {
        try {
            config.load();
            
            enableGameTracker = config.getBoolean("enableGameTracker", "modules", true, 
                "Enable game tracking functionality");
            enableCustomScoreboard = config.getBoolean("enableCustomScoreboard", "modules", true, 
                "Enable custom scoreboard display");
            enableChatCooldownTimer = config.getBoolean("enableChatCooldownTimer", "modules", true, 
                "Enable chat cooldown timer display");
            enableNameAutocomplete = config.getBoolean("enableNameAutocomplete", "modules", true, 
                "Enable name autocompletion");
            enableShortcutReminder = config.getBoolean("enableShortcutReminder", "modules", true, 
                "Enable shortcut reminder system");
            enableBuilderNotification = config.getBoolean("enableBuilderNotification", "modules", true, 
                "Enable builder change notifications");
                
            // Chat cooldown settings
            chatCooldownPingVolume = config.getInt("chatCooldownPingVolume", "chat_cooldown", 50, 0, 100,
                "Volume for chat cooldown notification sound (0-100)");
            chatCooldownTimer = config.getBoolean("chatCooldownTimer", "chat_cooldown", true,
                "Show chat cooldown timer overlay");
                
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Problem loading config file!", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
    
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.modID.equals(GuessTheUtils.MOD_ID)) {
            syncConfig();
        }
    }
} 