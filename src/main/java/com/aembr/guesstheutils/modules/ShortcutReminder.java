package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Utils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortcutReminder extends GTBEvents.Module {
    public static final String SHORTCUTS_FILENAME = "guesstheutils-shortcuts.json";
    private Map<String, List<String>> shortcuts = new HashMap<>();

    String currentTheme = "";
    List<Map.Entry<String, List<String>>> currentShortcuts = new ArrayList<>();

    Formatting shortcutColor = Formatting.GOLD;
    Formatting themeColor = Formatting.GREEN;


    public ShortcutReminder(GTBEvents events) {
        super(events);
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, this::onThemeUpdate, this);
        events.subscribe(GTBEvents.RoundEndEvent.class, e -> this.reset(), this);
    }

    public void onThemeUpdate(GTBEvents.ThemeUpdateEvent event) {
        currentTheme = event.theme();
        currentShortcuts = getShortcuts(currentTheme);

        if (currentShortcuts.isEmpty()) return;

        MutableText reminderMessage = Text.literal("");
        for (Map.Entry<String, List<String>> shortcut : currentShortcuts) {
            String shortcutKey = shortcut.getKey();
            List<String> themes = shortcut.getValue();

            MutableText shortcutText = Text.literal(shortcutKey)
                    .formatted(shortcutColor).formatted(Formatting.BOLD);

            reminderMessage.append(shortcutText)
                    .append(Text.literal(" works for ").formatted(Formatting.GRAY));

            for (int i = 0; i < themes.size(); i++) {
                MutableText themeText = Text.literal(themes.get(i))
                        .formatted(themeColor);

                reminderMessage.append(themeText);

                if (i < themes.size() - 1) {
                    reminderMessage.append(Text.literal(", ").formatted(Formatting.GRAY));
                }
            }

            reminderMessage.append(Text.literal(". ").formatted(Formatting.GRAY));
        }
        Utils.sendMessage(reminderMessage);
    }

    public void reset() {
        currentTheme = "";
        currentShortcuts = new ArrayList<>();
    }

    public List<Map.Entry<String, List<String>>> getShortcuts(String theme) {
        if (theme.isEmpty()) return new ArrayList<>();
        return shortcuts.entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(theme))
                ).toList();
    }

    public void init() {
        // Get the config directory
        Path configPath = FabricLoader.getInstance().getConfigDir();
        File configFile = new File(configPath.toFile(), SHORTCUTS_FILENAME);

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
        try (InputStream inputStream = GuessTheUtils.class.getResourceAsStream("/" + SHORTCUTS_FILENAME);
             FileOutputStream outputStream = new FileOutputStream(configFile)) {
            if (inputStream == null) {
                throw new IOException("Default config file not found in resources");
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private HashMap<String, List<String>> loadConfig(File configFile) {
        Gson gson = new Gson();
        HashMap<String, List<String>> config = new HashMap<>();

        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<HashMap<String, List<String>>>() {}.getType();
            config = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }
}
