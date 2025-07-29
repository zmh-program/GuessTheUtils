package com.aembr.guesstheutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Replay {
    private List<Tick> ticks = new ArrayList<>();
    private String fileName;
    private Gson gson;
    
    public Replay() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public void initialize() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        this.fileName = "replay_" + now.format(formatter) + ".json";
        GuessTheUtils.LOGGER.info("Replay system initialized: " + fileName);
    }
    
    public void addTick(Tick tick) {
        if (tick != null && !tick.isEmpty()) {
            ticks.add(tick);
        }
    }
    
    public void save() {
        try {
            File replayDir = new File(Minecraft.getMinecraft().mcDataDir, "guesstheutils/replays");
            if (!replayDir.exists()) {
                replayDir.mkdirs();
            }
            
            File replayFile = new File(replayDir, fileName);
            
            List<Tick.SerializedTick> serializedTicks = new ArrayList<>();
            for (Tick tick : ticks) {
                serializedTicks.add(tick.serialize());
            }
            
            try (FileWriter writer = new FileWriter(replayFile)) {
                gson.toJson(serializedTicks, writer);
                GuessTheUtils.LOGGER.info("Replay saved: " + replayFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            GuessTheUtils.LOGGER.error("Failed to save replay", e);
        }
    }
    
    public static Replay load(String fileName) {
        try {
            File replayDir = new File(Minecraft.getMinecraft().mcDataDir, "guesstheutils/replays");
            File replayFile = new File(replayDir, fileName);
            
            if (!replayFile.exists()) {
                GuessTheUtils.LOGGER.warn("Replay file not found: " + fileName);
                return new Replay();
            }
            
            try (FileReader reader = new FileReader(replayFile)) {
                Gson gson = new Gson();
                JsonObject[] jsonTicks = gson.fromJson(reader, JsonObject[].class);
                
                Replay replay = new Replay();
                for (JsonObject jsonTick : jsonTicks) {
                    replay.ticks.add(new Tick(jsonTick));
                }
                
                GuessTheUtils.LOGGER.info("Replay loaded: " + fileName + " (" + replay.ticks.size() + " ticks)");
                return replay;
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to load replay: " + fileName, e);
            return new Replay();
        }
    }
    
    public static Replay load(InputStream stream) {
        try {
            if (stream == null) {
                GuessTheUtils.LOGGER.warn("Replay stream is null");
                return new Replay();
            }
            
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                Gson gson = new Gson();
                JsonObject[] jsonTicks = gson.fromJson(reader, JsonObject[].class);
                
                Replay replay = new Replay();
                if (jsonTicks != null) {
                    for (JsonObject jsonTick : jsonTicks) {
                        replay.ticks.add(new Tick(jsonTick));
                    }
                }
                
                GuessTheUtils.LOGGER.info("Replay loaded from stream (" + replay.ticks.size() + " ticks)");
                return replay;
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to load replay from stream", e);
            return new Replay();
        }
    }
    
    public List<Tick> getTicks() {
        return new ArrayList<>(ticks);
    }
    
    public int size() {
        return ticks.size();
    }
    
    public void clear() {
        ticks.clear();
    }
} 