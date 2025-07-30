package com.aembr.guesstheutils;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

public class GTBEventsTest {
    private GTBEvents events;
    private TestEventListener listener;

    @BeforeAll
    public static void beforeAll() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        events = new GTBEvents();
        listener = new TestEventListener(events);
    }

    @Test
    void testGameStartEventSeparateTicks() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventSeparateTicks.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;

        Set<GTBEvents.InitialPlayerData> expected = new HashSet<>(List.of(
                new GTBEvents.InitialPlayerData("bubulS", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("$").formatted(Formatting.BLACK), Formatting.GREEN, false),
                new GTBEvents.InitialPlayerData("KoboIdus", Text.literal("Untrained").formatted(Formatting.GRAY), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("JARAKURE", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Fazelin", Text.literal("Amateur").formatted(Formatting.DARK_GRAY), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("FINMINETK", Text.literal("Apprentice").formatted(Formatting.DARK_GREEN), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Nekst_off", Text.literal("Experienced").formatted(Formatting.AQUA), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Yria", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("≈").formatted(Formatting.YELLOW), Formatting.AQUA, true),
                new GTBEvents.InitialPlayerData("SerdarLunatix", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Video_Virus2", Text.literal("Untrained").formatted(Formatting.GRAY), Text.empty(), Formatting.GRAY, false)
        ));
        Set<GTBEvents.InitialPlayerData> actual = ((GTBEvents.GameStartEvent) event).players();
        assert actual.equals(expected) : "Expected:\n" + expected + "\nActual:\n" + actual;
    }

    @Test
    void testGameStartEventSameTick() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventSameTick.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;

        Set<GTBEvents.InitialPlayerData> expected = new HashSet<>(List.of(
                new GTBEvents.InitialPlayerData("bubulS", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("$").formatted(Formatting.BLACK), Formatting.GREEN, false),
                new GTBEvents.InitialPlayerData("KoboIdus", Text.literal("Untrained").formatted(Formatting.GRAY), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("JARAKURE", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Fazelin", Text.literal("Amateur").formatted(Formatting.DARK_GRAY), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("FINMINETK", Text.literal("Apprentice").formatted(Formatting.DARK_GREEN), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Nekst_off", Text.literal("Experienced").formatted(Formatting.AQUA), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Yria", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("≈").formatted(Formatting.YELLOW), Formatting.AQUA, true),
                new GTBEvents.InitialPlayerData("SerdarLunatix", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Video_Virus2", Text.literal("Untrained").formatted(Formatting.GRAY), Text.empty(), Formatting.GRAY, false)
        ));
        Set<GTBEvents.InitialPlayerData> actual = ((GTBEvents.GameStartEvent) event).players();
        assert actual.equals(expected) : "Expected:\n" + expected + "\nActual:\n" + actual;
    }

    @Test
    void testGameStartEventDelayedSetupEntry() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventDelayedSetupEntry.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;
    }

    @Test
    void testGameStartEventMultipleSetupTicks() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventMultiSetup.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;

        Set<GTBEvents.InitialPlayerData> expected = new HashSet<>(List.of(
                new GTBEvents.InitialPlayerData("Mstf", Text.literal("Amateur").formatted(Formatting.DARK_GRAY), Text.empty(), Formatting.GOLD, false),
                new GTBEvents.InitialPlayerData("Cazzeeee", Text.literal("Prospect").formatted(Formatting.GREEN), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("NotAcomy", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.AQUA, false),
                new GTBEvents.InitialPlayerData("P4t_o", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.AQUA, false),
                new GTBEvents.InitialPlayerData("Lanyingtears", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.AQUA, false),
                new GTBEvents.InitialPlayerData("Yria", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("≈").formatted(Formatting.YELLOW), Formatting.AQUA, true),
                new GTBEvents.InitialPlayerData("Ao0j", Text.literal("Trained").formatted(Formatting.BLUE), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Rinkutako", Text.literal("Untrained").formatted(Formatting.GRAY), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("PepinoCL", Text.literal("Rookie").formatted(Formatting.WHITE), Text.empty(), Formatting.GRAY, false)
        ));
        Set<GTBEvents.InitialPlayerData> actual = ((GTBEvents.GameStartEvent) event).players();
        assert actual.equals(expected) : "Expected:\n" + expected + "\nActual:\n" + actual;
    }

    @Test
    void testGameStartEventIncomplete() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventIncomplete.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;

        Set<GTBEvents.InitialPlayerData> expected = new HashSet<>(List.of(
                new GTBEvents.InitialPlayerData("owwwwen", null, null, Formatting.GREEN, false),
                new GTBEvents.InitialPlayerData("Nescafe755", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("$").formatted(Formatting.BLACK), Formatting.GOLD, false),
                new GTBEvents.InitialPlayerData("Coldflames19", Text.literal("Prospect").formatted(Formatting.GREEN), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("AK4Boris", Text.literal("Prospect").formatted(Formatting.GREEN), Text.empty(), Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("Aceptum", null, null, Formatting.AQUA, false),
                new GTBEvents.InitialPlayerData("Yria", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("≈").formatted(Formatting.YELLOW), Formatting.GOLD, true),
                new GTBEvents.InitialPlayerData("vautism", null, null, Formatting.GOLD, false),
                new GTBEvents.InitialPlayerData("SophieLacksTeeth", Text.literal("Skilled").formatted(Formatting.DARK_BLUE), Text.empty(), Formatting.GREEN, false),
                new GTBEvents.InitialPlayerData("vixyi", null, null, Formatting.GRAY, false),
                new GTBEvents.InitialPlayerData("watahfall", Text.literal("Expert").formatted(Formatting.DARK_RED), Text.literal("$").formatted(Formatting.BLACK), Formatting.GOLD, false)
        ));
        Set<GTBEvents.InitialPlayerData> actual = ((GTBEvents.GameStartEvent) event).players();
        assert actual.equals(expected) : "Expected:\n" + expected + "\nActual:\n" + actual;
    }

    @Test
    void testGameStartUserBuilder() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameStartEventUserBuilder.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameStartEvent;
        assert ((GTBEvents.GameStartEvent) event).players().size() == 10;
    }

    @Test
    void testPlayerWithWhiteEmblem() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestPlayerWithWhiteEmblem.json"));
        runner.play(events);
    }

    @Test
    void testPrematureStart() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestPrematureGameStart.json"));
        runner.play(events);

        assert listener.receivedEvents.isEmpty();
    }

    @Test
    void testPrematureStart2() {
        events.subscribe(GTBEvents.GameStartEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestPrematureStart2.json"));
        runner.play(events);


        assert listener.receivedEvents.isEmpty();
    }

    @Test
    void testLimbo() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestLimbo.json"));
        runner.play(events);
    }

    @Test
    void testBuilderChangeEvent() {
        events.subscribe(GTBEvents.BuilderChangeEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestBuilderChangeEvent.json"));
        events.changeState(GTBEvents.GameState.ROUND_PRE);
        runner.play(events);

        assert listener.receivedEvents.size() == 2;

        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.BuilderChangeEvent;
        assert ((GTBEvents.BuilderChangeEvent) event).previous().isEmpty();
        assert ((GTBEvents.BuilderChangeEvent) event).current().equals("sushiryy");

        event = listener.receivedEvents.get(1);
        assert event instanceof GTBEvents.BuilderChangeEvent;
        assert ((GTBEvents.BuilderChangeEvent) event).previous().equals("sushiryy");
        assert ((GTBEvents.BuilderChangeEvent) event).current().equals("Yria");
    }

    @Test
    void testCorrectGuessEvent() {
        events.subscribe(GTBEvents.CorrectGuessEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestCorrectGuessEvent.json"));
        events.changeState(GTBEvents.GameState.ROUND_BUILD);
        runner.play(events);

        assert listener.receivedEvents.size() == 2;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.CorrectGuessEvent;
        assert ((GTBEvents.CorrectGuessEvent) event).players().equals(List.of(
                new GTBEvents.FormattedName("TroyBoy105", Formatting.GRAY)));

        event = listener.receivedEvents.get(1);
        assert event instanceof GTBEvents.CorrectGuessEvent;
        assert ((GTBEvents.CorrectGuessEvent) event).players().equals(List.of(
                new GTBEvents.FormattedName("Yria", Formatting.AQUA),
                new GTBEvents.FormattedName("nikIII123", Formatting.GRAY)));
    }

    @Test
    void testThemeHintUpdateEvent() {
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestThemeHintUpdate.json"));
        events.changeState(GTBEvents.GameState.ROUND_BUILD);
        runner.play(events);

        assert listener.receivedEvents.size() == 4;

        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.ThemeUpdateEvent;
        assert ((GTBEvents.ThemeUpdateEvent) event).theme().equals("___d____e");

        event = listener.receivedEvents.get(1);
        assert event instanceof GTBEvents.ThemeUpdateEvent;
        assert ((GTBEvents.ThemeUpdateEvent) event).theme().equals("Handshake");

        event = listener.receivedEvents.get(2);
        assert event instanceof GTBEvents.ThemeUpdateEvent;
        assert ((GTBEvents.ThemeUpdateEvent) event).theme().equals("Beach Ball");

        event = listener.receivedEvents.get(3);
        assert event instanceof GTBEvents.ThemeUpdateEvent;
        assert ((GTBEvents.ThemeUpdateEvent) event).theme().equals("Handshake");
    }

    @Test
    void testGameEndEvent() {
        events.subscribe(GTBEvents.GameEndEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGameEndEvent.json"));
        events.changeState(GTBEvents.GameState.ROUND_END);
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
        GTBEvents.BaseEvent event = listener.receivedEvents.get(0);
        assert event instanceof GTBEvents.GameEndEvent;
        Map<String, Integer> actual = ((GTBEvents.GameEndEvent) event).scores();

        HashMap<String, Integer> expected = new HashMap<>();
        expected.put("Yria", 25);
        expected.put("SnowyHazard", 15);
        expected.put("TroyBoy105", 12);
        //expected.put("AdvisingFern4551", 11);
        // the name is 16 characters long, so we disregard their score, as it will be cut off if it's in double digits
        expected.put("SwiftChess", 11);
        expected.put("sushiryy", 10);
        expected.put("Warhammer364", 8);
        expected.put("nikIII123", 8);
        expected.put("EnilingCZ", 6);
        expected.put("Eduard_01", 2);

        assert actual.equals(expected) : "Expected:\n" + expected + "\nActual:\n" + actual;
    }

    @Test
    void testGetGameStateFromScoreboard() {
        assert events.getStateFromScoreboard(List.of(
                "BUILD BATTLE",
                "12/26/24  L12D",
                "Wins: 668",
                "Score: 21,200",
                "Tokens: 4,082",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.NONE);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m94B",
                "Players: 4/10",
                "Starting in 04:47 to",
                "allow time for",
                "additional players",
                "Mode: Guess The Build",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.LOBBY);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m94B",
                "Players: 10/10",
                "Starting in 6s",
                "Mode: Guess The Build",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.LOBBY);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m94B",
                "Builder:",
                " Yria",
                "GlowingYoshi: 0",
                "_PolarBearz_: 0",
                "Nescafe755: 0",
                "...",
                "Yria: 0",
                "Starts In: 00:10",
                "Theme:",
                " ???",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.ROUND_PRE);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m94B",
                "Builder:",
                " Yria",
                "GlowingYoshi: 0",
                "Nescafe755: 0",
                "Yria: 0",
                "Time: 02:00",
                "Theme:",
                " Ice Cream Cone",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.ROUND_BUILD);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m94B",
                "Builder:",
                " Yria",
                "Nescafe755: 3",
                "Yria: 3",
                "DarkkBlue: 2",
                "Next Round: 00:05",
                "Theme:",
                " Ice Cream Cone",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.ROUND_END);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD",
                "12/26/24  m35AW",
                "1. Nescafe755: 13",
                "1. Yria: 13",
                "1. illiasteeam: 13",
                "1. _Platon_1: 13",
                "5. Katuzi3758: 7",
                "6. meoweky: 3",
                "6. 64passion_: 3",
                "8. ",
                "9. ",
                "10. ",
                "www.hypixel.net"
        )).equals(GTBEvents.GameState.POST_GAME);

        assert events.getStateFromScoreboard(List.of(
                "GUESS THE BUILD"
        )) == GTBEvents.GameState.NONE;
    }

    @Test
    void testGetTrueScoresFromScoreboard() {
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestGetTrueScores.json"));

        List<GTBEvents.TrueScore> a1 = events.getTrueScoresFromScoreboard(runner.next().scoreboardLines);

        List<GTBEvents.TrueScore> e1 = List.of(
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("_emmy", Formatting.GOLD), 3),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("theofficialwater", Formatting.AQUA), 3),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("Nescafe755", Formatting.GOLD), 2),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("Yria", Formatting.GOLD), 1)
        );

        assert a1.equals(e1) : "Actual:\n" + a1 + "\nExpected:\n" + e1;

        List<GTBEvents.TrueScore> a2 = events.getTrueScoresFromScoreboard(runner.next().scoreboardLines);

        List<GTBEvents.TrueScore> e2 = List.of(
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("Yria", Formatting.GOLD), 0),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("Nescafe755", Formatting.GOLD), 0)
        );

        assert a2.equals(e2) : "Actual:\n" + a2 + "\nExpected:\n" + e2;

        List<GTBEvents.TrueScore> a3 = events.getTrueScoresFromScoreboard(runner.next().scoreboardLines);

        List<GTBEvents.TrueScore> e3 = List.of(
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("_emmy", Formatting.GOLD), 9),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("theofficialwater", Formatting.AQUA), 5),
                new GTBEvents.TrueScore(new GTBEvents.FormattedName("Yria", Formatting.GOLD), 4)
        );

        assert a3.equals(e3) : "Actual:\n" + a3 + "\nExpected:\n" + e3;
    }

    @Test
    void testPrematureBuilder() {
        events.subscribe(GTBEvents.BuilderChangeEvent.class, event -> listener.onEvent(event), listener);
        TestRunner runner = new TestRunner(new File("src/test/java/resources/tests/events/TestPrematureBuilder.json"));
        runner.play(events);

        assert listener.receivedEvents.size() == 1;
    }

    private static class TestEventListener extends GTBEvents.Module {
        private final List<GTBEvents.BaseEvent> receivedEvents = new ArrayList<>();

        public TestEventListener(GTBEvents events) {
            super(events);
        }

        public void onEvent(GTBEvents.BaseEvent event) {
            receivedEvents.add(event);
        }

        public void clear() {
            receivedEvents.clear();
        }
    }
}
