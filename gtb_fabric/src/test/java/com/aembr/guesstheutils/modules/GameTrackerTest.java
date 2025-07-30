package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.TestRunner;
import com.aembr.guesstheutils.Tick;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameTrackerTest {
    private final String e2eTestsDir = "../../src/test/java/resources/tests/e2e/";
    private GTBEvents events;
    private GameTracker tracker;

    @BeforeAll
    public static void beforeAll() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        events = new GTBEvents();
        tracker = new GameTracker(events);
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

    private Set<File> listFilesInDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toFile)
                    .collect(Collectors.toSet());
        }
    }

    private void processTestFile(File filePath) throws IOException {
        System.out.println("Running test file " + e2eTestsDir + filePath);
        TestRunner runner = new TestRunner(new File(e2eTestsDir + filePath));
        runner.play(events);
    }

    @Test
    void testLessThan3TrueScores() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/modules/game_tracker/TestLessThan3TrueScores.json"));
        runner.play(events);
    }


    @Test
    void testBuilderCarryOverBetweenGames() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/modules/game_tracker/TestNescafePoints.json"));
        runner.play(events);

        GameTracker.Player nescafe = tracker.game.players.stream().filter(p -> p.name.equals("Nescafe755")).findAny().orElse(null);
        assert nescafe != null;
        assert nescafe.getTotalPoints() == 3;
    }

    @Test
    void testNescafeGettingMarkedAsLeaverIncorrectly() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/modules/game_tracker/TestBuggyLeaverDetection.json"));
        runner.play(events);

        GameTracker.Player nescafe = tracker.game.players.stream().filter(p -> p.name.equals("Nescafe755")).findAny().orElse(null);
        assert nescafe != null;
        assert nescafe.leaverState.equals(GameTracker.Player.LeaverState.NORMAL);
    }
}
