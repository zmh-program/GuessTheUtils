package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Utils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortcutReminder extends GTBEvents.Module {
    public static final String SHORTCUTS_FILENAME = "guesstheutils-shortcuts.yml";
    private Map<String, List<String>> shortcuts = new HashMap<String, List<String>>();

    String currentTheme = "";
    String lastTheme = "";
    List<Map.Entry<String, List<String>>> currentShortcuts = new ArrayList<Map.Entry<String, List<String>>>();

    EnumChatFormatting shortcutColor = EnumChatFormatting.GOLD;
    EnumChatFormatting themeColor = EnumChatFormatting.GREEN;


    public ShortcutReminder(GTBEvents events) {
        super(events);
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, this::onThemeUpdate, this);
        events.subscribe(GTBEvents.RoundEndEvent.class, e -> this.reset(), this);
    }

    public void onThemeUpdate(GTBEvents.ThemeUpdateEvent event) {
        currentTheme = event.theme();

        if (lastTheme.equals(currentTheme)) return;
        lastTheme = currentTheme;

        if (!GuessTheUtilsConfig.enableShortcutReminder) return;

        currentShortcuts = getShortcuts(currentTheme);

        if (currentShortcuts.isEmpty()) return;

        ChatComponentText reminderMessage = new ChatComponentText("");
        for (Map.Entry<String, List<String>> shortcut : currentShortcuts) {
            String shortcutKey = shortcut.getKey();
            List<String> themes = shortcut.getValue();

            ChatComponentText shortcutText = new ChatComponentText(shortcutKey);
            shortcutText.getChatStyle().setColor(shortcutColor).setBold(true);

            reminderMessage.appendSibling(shortcutText);
            
            ChatComponentText worksForText = new ChatComponentText(" works for ");
            worksForText.getChatStyle().setColor(EnumChatFormatting.GRAY);
            reminderMessage.appendSibling(worksForText);

            for (int i = 0; i < themes.size(); i++) {
                ChatComponentText themeText = new ChatComponentText(themes.get(i));
                themeText.getChatStyle().setColor(themeColor);
                reminderMessage.appendSibling(themeText);

                if (i < themes.size() - 1) {
                    ChatComponentText commaText = new ChatComponentText(", ");
                    commaText.getChatStyle().setColor(EnumChatFormatting.GRAY);
                    reminderMessage.appendSibling(commaText);
                }
            }

            ChatComponentText periodText = new ChatComponentText(". ");
            periodText.getChatStyle().setColor(EnumChatFormatting.GRAY);
            reminderMessage.appendSibling(periodText);
        }
        Utils.sendMessage(reminderMessage.getFormattedText());
    }

    public void reset() {
        currentTheme = "";
        lastTheme = "";
        currentShortcuts = new ArrayList<Map.Entry<String, List<String>>>();
    }

    public List<Map.Entry<String, List<String>>> getShortcuts(String theme) {
        if (theme.isEmpty()) return new ArrayList<Map.Entry<String, List<String>>>();
        
        List<Map.Entry<String, List<String>>> result = new ArrayList<Map.Entry<String, List<String>>>();
        for (Map.Entry<String, List<String>> entry : shortcuts.entrySet()) {
            for (String value : entry.getValue()) {
                if (value.equalsIgnoreCase(theme)) {
                    result.add(entry);
                    break;
                }
            }
        }
        return result;
    }

    public void init() {
        // Get the config directory - using Minecraft directory + config
        File minecraftDir = new File(".");
        File configDir = new File(minecraftDir, "config");
        File configFile = new File(configDir, SHORTCUTS_FILENAME);

        // Check if the config file exists
        if (!configFile.exists()) {
            // If it doesn't exist, copy the default config from resources
            try {
                copyDefaultConfig(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load the configuration data
        shortcuts = loadConfig(configFile);
    }

    private void copyDefaultConfig(File configFile) throws IOException {
        // Create the parent directories if they don't exist
        configFile.getParentFile().mkdirs();

        // Copy the default config file from resources
        try {
            InputStream inputStream = GuessTheUtils.class.getResourceAsStream("/" + SHORTCUTS_FILENAME);
            FileOutputStream outputStream = new FileOutputStream(configFile);
            
            if (inputStream == null) {
                throw new IOException("Default config file not found in resources");
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            throw e;
        }
    }

    private HashMap<String, List<String>> loadConfig(File configFile) {
        Yaml yaml = new Yaml();
        HashMap<String, List<String>> config = new HashMap<String, List<String>>();

        try {
            FileReader reader = new FileReader(configFile);
            config = (HashMap<String, List<String>>) yaml.load(reader);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }
}