package com.aembr.guesstheutils;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Replay {
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    public static final String REPLAYS_DIR_NAME = "guesstheutils-replays";

    public static Path replayDir;
    public static TickBuffer tickBuffer = new TickBuffer(3000);

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

    public void save() {
        assert GuessTheUtils.CLIENT.player != null;

        String date = LocalDate.now().toString();

        int counter = 1;
        String filename;
        Path replayFilePath;

        do {
            filename = date + "_" + counter + ".json.gz";
            replayFilePath = replayDir.resolve(filename);
            counter++;
        } while (Files.exists(replayFilePath));

        Gson gson = new Gson();
        List<Tick.SerializedTick> buffer = tickBuffer.getBuffer().stream().toList();
        String jsonString = gson.toJson(buffer);

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(replayFilePath.toFile()))) {
            gzipOutputStream.write(jsonString.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Utils.sendMessage("Replay " + filename + " saved.");
    }

    public static class TickBuffer {
        private final Deque<Tick.SerializedTick> buffer;
        private final int maxSize;

        public TickBuffer(int maxSize) {
            this.buffer = new ArrayDeque<>(maxSize);
            this.maxSize = maxSize;
        }

        public void add(Tick tick) {
            buffer.addLast(tick.serialize());
            if (buffer.size() > maxSize) {
                buffer.removeFirst();
            }
        }

        public Deque<Tick.SerializedTick> getBuffer() {
            return buffer;
        }
    }
}
