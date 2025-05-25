package com.aembr.guesstheutils;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class Replay {
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    public static final String REPLAYS_DIR_NAME = "guesstheutils-replays";

    public static Path replayDir;
    public static TickBuffer tickBuffer = new TickBuffer(1500);

    public void initialize() {
        try {
            replayDir = GAME_DIR.resolve(REPLAYS_DIR_NAME);
            if (!Files.exists(replayDir)) {
                Files.createDirectories(replayDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addTick(Tick tick) {
        tickBuffer.add(tick);
    }

    public void save(){
        assert GuessTheUtils.CLIENT.player != null;

        String filename = LocalDate.now() + "_" + UUID.randomUUID().toString().substring(0, 4);
        Gson gson = new Gson();
        Path replayFilePath = replayDir.resolve(filename + ".json");
        List<Tick.SerializedTick> buffer = tickBuffer.getBuffer().stream().map(Tick::serialize).toList();
        String jsonString = gson.toJson(buffer);

        try {
            Files.writeString(replayFilePath, jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Utils.sendMessage(filename + ".json saved.");
    }

    public static class TickBuffer {
        private final Deque<Tick> buffer;
        private final int maxSize;

        public TickBuffer(int maxSize) {
            this.buffer = new ArrayDeque<>(maxSize);
            this.maxSize = maxSize;
        }

        public void add(Tick tick) {
            buffer.addLast(tick);
            if (buffer.size() > maxSize) {
                buffer.removeFirst();
            }
        }

        public Deque<Tick> getBuffer() {
            return buffer;
        }
    }
}
