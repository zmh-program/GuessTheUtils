package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.Tick;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomScoreboardTest {
    private final String e2eTestsDir = "src/test/java/resources/e2eTests/";
    private GTBEvents events;
    private CustomScoreboard scoreboard;

    @BeforeAll
    public static void beforeAll() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        events = new GTBEvents();
        scoreboard = new CustomScoreboard(events);
    }

    @Test
    void e2eTests() throws IOException {
        listFilesInDir(e2eTestsDir).forEach(file -> {
            try {
                setUp();
                processTestFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Set<String> listFilesInDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    private void processTestFile(String filePath) throws IOException {
        System.out.println("Running test file " + e2eTestsDir + filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(e2eTestsDir + filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Tick tick = new Tick(line);
                events.processTickUpdate(tick);
            }
        }
    }
}
