package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GameTracker extends GTBEvents.Module {
    public static GTBEvents.GameState state = GTBEvents.GameState.NONE;

    Game game;
    CustomScoreboard scoreboard;

    public GameTracker(GTBEvents events) {
        super(events);

        scoreboard = new CustomScoreboard(this);

        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
        events.subscribe(GTBEvents.GameStartEvent.class, this::onGameStart, this);
        events.subscribe(GTBEvents.StateChangeEvent.class, e -> {
            state = e.current();
            if (state.equals(GTBEvents.GameState.ROUND_PRE) && game != null) {
                game.currentTheme = "";
            }
        }, this);
        events.subscribe(GTBEvents.BuilderChangeEvent.class, e -> {
            if (game != null) game.onBuilderChange(e.current());
        }, this);

        events.subscribe(GTBEvents.RoundStartEvent.class, e -> {
            if (game != null) game.onRoundStart(e.currentRound(), e.totalRounds());
        }, this);

        events.subscribe(GTBEvents.RoundEndEvent.class, e -> {
            if (game != null) game.onRoundEnd(e.skipped());
        }, this);

        events.subscribe(GTBEvents.RoundSkipEvent.class, e -> {
            if (game != null) game.onRoundSkipped();
        }, this);

        events.subscribe(GTBEvents.CorrectGuessEvent.class, e -> {
            if (game != null) game.onCorrectGuess(e.players());
        }, this);

        events.subscribe(GTBEvents.ThemeUpdateEvent.class, e -> {
            if (game != null) game.onThemeUpdate(e.theme());
        }, this);

        events.subscribe(GTBEvents.GameEndEvent.class, e -> {
            if (game != null) game.onGameEnd(e.scores());
        }, this);

        events.subscribe(GTBEvents.TrueScoresUpdateEvent.class, e -> {
            if (game != null) game.onTrueScoresUpdate(e.scores());
        }, this);

        events.subscribe(GTBEvents.UserLeaveEvent.class, e -> {
            if (game != null) game.onUserLeave();
        }, this);

        events.subscribe(GTBEvents.UserRejoinEvent.class, this::onUserRejoin, this);

        events.subscribe(GTBEvents.PlayerChatEvent.class, e -> {
            if (game != null) game.onPlayerChat(e.player(), e.message());
        }, this);

        events.subscribe(GTBEvents.OneSecondAlertEvent.class, e -> {
            if (game != null) game.onOneSecondAlert();
        }, this);

        events.subscribe(GTBEvents.TimerUpdateEvent.class, e -> {
            if (game != null) game.currentTimer = e.timer();
        }, this);
    }

    private void onUserRejoin(GTBEvents.UserRejoinEvent userRejoinEvent) {
        if (game == null || game.leaveState == null || game.leaveRound == -1 || game.leaveBuilder == null) {
            Utils.sendMessage("Tracking is disabled for this game, as you weren't present at the start " +
                    "(most commonly caused by restarting the game).");
            return;
        }

        game.onUserRejoin();
    }

    private void onGameStart(GTBEvents.GameStartEvent event) {
        Set<Player> players = new HashSet<>();
        event.players().forEach(p ->
                players.add(new Player(p.name(), p.rankColor(), p.title(), p.emblem(), p.isUser())));
        game = new Game(this, players);
    }

    private void onTick(MinecraftClient client) {
        if (game == null || !events.isInGtb()) return;
        game.players.forEach(player -> player.inactiveTicks++);
        CustomScoreboard.tickCounter++;
    }

    private static int getThemePointAward(String theme) {
        return (theme.length() < 6) ? 1 : (theme.length() < 9) ? 2 : 3;
    }

    private void clearGame() {
        game = null;
    }

    private void clearGameWithError(String error) {
        clearGame();
        throw new RuntimeException(error);
    }

    @Override
    public ErrorAction getErrorAction() {
        return ErrorAction.LOG_AND_CONTINUE;
    }

    static class Player {
        enum LeaverState { NORMAL, POTENTIAL_LEAVER, LEAVER }
        String name;
        Formatting rank;
        Text title;
        Text emblem;
        int[] points;
        int buildRound;
        boolean isUser;
        int scoreMismatchCounter = 0;
        int inactiveTicks = 0;
        LeaverState leaverState = LeaverState.NORMAL;

        Player(String name, Formatting rank, Text title, Text emblem, boolean isUser) {
            this.name = name;
            this.rank = rank;
            this.title = title;
            this.emblem = emblem;
            this.isUser = isUser;
            this.points = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            this.buildRound = 0;
        }

        int getTotalPoints() {
            return Arrays.stream(points).sum();
        }

        public int getInactiveTicks() {
            return inactiveTicks;
        }

        public LeaverState getLeaverState() {
            return leaverState;
        }

        @Override
        public String toString() {
            return "Player{" +
                    "name='" + name + '\'' +
                    ", points=" + Arrays.toString(points) +
                    ", buildRound=" + buildRound +
                    ", isUser=" + isUser +
                    ", inactiveTicks=" + inactiveTicks +
                    ", leaverState=" + leaverState +
                    '}';
        }
    }

    static class Game {
        GameTracker tracker;
        Set<Player> players;

        int currentRound = 0;
        int totalRounds;
        int skippedRounds = 0;
        int potentialLeaverAmount = 0;
        boolean userRoundSkipped = false;

        String currentTheme = "";
        String currentTimer = "";
        Player currentBuilder = null;
        int correctGuessesThisRound = 0;
        boolean oneSecondAlertReached = false;

        List<Utils.Pair<Player, Integer>> latestTrueScore;

        // when the user leaves mid-game, we need to remember some stuff for when they join back
        GTBEvents.GameState leaveState;
        int leaveRound = -1;
        Player leaveBuilder;

        public Game(GameTracker tracker, Set<Player> players) {
            this.tracker = tracker;
            this.players = players;
            this.totalRounds = players.size();
        }

        public void onBuilderChange(String builderName) {
            if (builderName != null) {
                Player builder = getPlayerFromName(builderName);
                if (builder == null) {
                    tracker.clearGameWithError("Player " + builderName + " not found in player list!");
                    return;
                }
                currentBuilder = builder;
                onActivity(currentBuilder);
                return;
            }
            currentBuilder = null;
        }

        public void onRoundStart(int current, int total) {
            currentRound = current;
            oneSecondAlertReached = false;

            // this check will only be true if the user left during the end of one round,
            // and rejoined before the start of another. we just need to check if they haven't skipped any rounds
            if (leaveRound != -1) {
                if (currentRound == leaveRound + 1) {
                    Utils.sendMessage("Tracking data valid!");
                    clearLeaveState();
                } else {
                    Utils.sendMessage("Tracking data invalid! Falling back to vanilla scoreboard.");
                    tracker.clearGame();
                    return;
                }
            }

            if (currentBuilder == null) {
                tracker.clearGameWithError("currentBuilder is null!");
                return;
            }
            currentBuilder.buildRound = currentRound;
            currentTheme = "";
            correctGuessesThisRound = 0;

            totalRounds = total;
            int missing = players.size() - skippedRounds - total;
            if (missing != potentialLeaverAmount) {
                potentialLeaverAmount = missing;
                updatePotentialLeavers();
            }
        }

        public void onRoundEnd(boolean skipped) {
            boolean actuallySkipped = (currentBuilder.isUser && userRoundSkipped) || skipped;
            if (!actuallySkipped && !oneSecondAlertReached) {
                players.stream().filter(p -> p.points[currentRound - 1] == 0)
                        .forEach(p -> p.leaverState = Player.LeaverState.LEAVER);
            }
            userRoundSkipped = false;
        }

        public void onRoundSkipped() {
            currentBuilder.buildRound = -1;
            skippedRounds++;
        }

        public void onCorrectGuess(List<GTBEvents.FormattedName> players) {
            for (GTBEvents.FormattedName fName : players) {
                Player player = getPlayerFromName(fName.name());
                if (player == null) {
                    tracker.clearGameWithError("Player " + fName.name() + " not found in player list!");
                    return;
                }
                player.points[currentRound - 1] = Math.max(1, 3 - correctGuessesThisRound);
                onActivity(player);
                correctGuessesThisRound++;
            }

            if (currentBuilder == null) {
                tracker.clearGameWithError("currentBuilder is null!");
                return;
            }

            if (currentBuilder.points[currentRound - 1] == 0 && !currentTheme.isEmpty()) {
                currentBuilder.points[currentRound - 1] = getThemePointAward(currentTheme);
            }
        }

        public void onThemeUpdate(String theme) {
            currentTheme = theme;
            if (currentBuilder == null) {
                tracker.clearGameWithError("currentBuilder is null!");
                return;
            }
            if (currentBuilder.points[currentRound - 1] == 0 && correctGuessesThisRound > 0) {
                currentBuilder.points[currentRound - 1] = getThemePointAward(currentTheme);
            }
        }

        public void onGameEnd(Map<String, Integer> scores) {
            for (Map.Entry<String, Integer> expected : scores.entrySet()) {
                Player player = getPlayerFromName(expected.getKey());
                if (player == null) {
                    // there is a possibility for names to be cut off here, but it's the end of the game anyway
                    continue;
                }

                if (!verifyPoints(player, expected.getValue())) {
                    tracker.clearGameWithError("Scores do not match expected!");
                    return;
                }
            }
            tracker.clearGame();
        }

        public void onTrueScoresUpdate(List<GTBEvents.TrueScore> scores) {
            List<Utils.Pair<Player, Integer>> converted = new ArrayList<>();
            for (GTBEvents.TrueScore trueScore : scores) {
                if (trueScore == null) continue;
                Player player = getPlayerFromName(trueScore.fName().name());
                if (player == null) {
                    tracker.clearGameWithError("Player " + trueScore.fName().name() + " not found in player list!");
                    return;
                }

                onActivity(player);

                if (verifyPoints(player, trueScore.points())) {
                    player.scoreMismatchCounter = 0;
                } else {
                    if (player.buildRound == currentRound && player.points[currentRound - 1] == 0
                            && correctGuessesThisRound != 0) {
                        currentBuilder.points[currentRound - 1] = trueScore.points() - player.getTotalPoints();
                    } else {
                        // Sometimes the scoreboard is slow, so we want to wait a bit before we sound the alarm
                        if (player.scoreMismatchCounter > 0) { // increase this to wait longer
                            tracker.clearGameWithError("Score mismatch!");
                            return;
                        }
                        player.scoreMismatchCounter++;
                    }
                }
                converted.add(new Utils.Pair<>(player, trueScore.points()));
            }

            latestTrueScore = converted;

            List<Player> playersSortedByPoints = players.stream()
                    .filter(p -> !p.leaverState.equals(Player.LeaverState.LEAVER))
                    .sorted((p1, p2) -> Integer.compare(p2.getTotalPoints(), p1.getTotalPoints())).toList();

            List<Utils.Pair<Player, Integer>> topNames = new ArrayList<>();
            for (int i = 0; i < Math.min(3, latestTrueScore.size()); i++) {
                if (latestTrueScore.get(i) == null) continue;
                topNames.add(latestTrueScore.get(i));
            }

            if (topNames.isEmpty()) return;
            List<Player> guaranteedToAppearInScoreboard = new ArrayList<>();
            Map<Integer, List<Player>> pointsMap = new HashMap<>();
            for (Player player : playersSortedByPoints) {
                pointsMap.computeIfAbsent(player.getTotalPoints(), k -> new ArrayList<>()).add(player);
            }
            List<List<Player>> sortedGroupedPlayers = new ArrayList<>();
            pointsMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Integer, List<Player>>comparingByKey().reversed())
                    .forEach(entry -> sortedGroupedPlayers.add(entry.getValue()));

            for (List<Player> pointsGroup : sortedGroupedPlayers) {
                if (pointsGroup.size() > topNames.size() - guaranteedToAppearInScoreboard.size()) break;
                guaranteedToAppearInScoreboard.addAll(pointsGroup);
            }

            for (Player player : guaranteedToAppearInScoreboard) {
                if (topNames.stream().noneMatch(p -> p.a().equals(player))) {
                    player.leaverState = Player.LeaverState.LEAVER;
                }
            }
        }

        public void onUserLeave() {
            leaveState = state;
            leaveRound = currentRound;
            leaveBuilder = currentBuilder;

            if (leaveBuilder.isUser && leaveState.equals(GTBEvents.GameState.ROUND_BUILD)) {
                userRoundSkipped = true;
            }
        }

        public void onUserRejoin() {
            if (currentBuilder.isUser) {
                clearLeaveState();
                return;
            }

            if (leaveState.equals(GTBEvents.GameState.ROUND_BUILD) || state.equals(GTBEvents.GameState.ROUND_BUILD)) {
                Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                tracker.clearGame();
                return;
            }

            if (currentBuilder.equals(leaveBuilder)) {
                if (!state.equals(leaveState)) {
                    Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                    tracker.clearGame();
                    return;
                }
                clearLeaveState();

            } else {
                if (leaveState.equals(GTBEvents.GameState.ROUND_END) && state.equals(GTBEvents.GameState.ROUND_PRE)) {
                    Utils.sendMessage("Checking if tracking data for this game is still valid...");
                } else {
                    Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                    tracker.clearGame();
                }
            }
        }

        public void onPlayerChat(String playerName, String message) {
            Player player = getPlayerFromName(playerName);
            if (player == null) {
                tracker.clearGameWithError("Player " + playerName + " not found in player list!");
                return;
            }
            onActivity(player);
        }

        public void onOneSecondAlert() {
            oneSecondAlertReached = true;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean verifyPoints(Player player, Integer expectedPoints) {
            return player.getTotalPoints() == expectedPoints;
        }

        private void clearLeaveState() {
            leaveState = null;
            leaveRound = -1;
            leaveBuilder = null;
        }

        private void onActivity(Player player) {
            player.inactiveTicks = 0;
            if (player.leaverState.equals(Player.LeaverState.POTENTIAL_LEAVER)) {
                updatePotentialLeavers();
            } else if (player.leaverState.equals(Player.LeaverState.LEAVER)) {
                player.leaverState = Player.LeaverState.NORMAL;
                updatePotentialLeavers();
            }
        }

        private void updatePotentialLeavers() {
            players.stream().filter(player -> player.leaverState.equals(Player.LeaverState.POTENTIAL_LEAVER))
                    .forEach(player -> player.leaverState = Player.LeaverState.NORMAL);

            //System.out.println("leaver amount is " + potentialLeaverAmount);

            players.stream()
                    //.peek(s -> System.out.println("Starting players: " + s.name))
                    .filter(p -> p.buildRound <= 0)
                    .filter(p -> !p.isUser)
                    .filter(p -> !Objects.equals(p, currentBuilder))
                    //.peek(s -> System.out.println("Filtered players: " + s.name))
//                    .filter(p -> {
//                        if (latestTrueScore == null) return true;
//                        else return latestTrueScore.stream().noneMatch(score -> score.a().equals(p));
//                    })
                    .sorted(Comparator.comparing(Player::getLeaverState).reversed() // LEAVER first
                            .thenComparing(Comparator.comparingInt(Player::getInactiveTicks).reversed()))
                    //.peek(s -> System.out.println("Sorted by inactivity: " + s.name))
                    .limit(potentialLeaverAmount)
                    .filter(p -> !p.leaverState.equals(Player.LeaverState.LEAVER))
                    .forEach(p -> {
                        p.leaverState = Player.LeaverState.POTENTIAL_LEAVER;
                        //System.out.println("Setting " + p.name + " to potential leaver!");
                    });
        }

        private Player getPlayerFromName(String name) {
            return players.stream().filter(p -> p.name.equals(name)).findAny().orElse(null);
        }
    }
}
