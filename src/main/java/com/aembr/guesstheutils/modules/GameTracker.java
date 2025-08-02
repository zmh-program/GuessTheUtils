package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.*;

public class GameTracker extends GTBEvents.Module {
    public static GTBEvents.GameState state = GTBEvents.GameState.NONE;

    public Game game;
    public CustomScoreboard scoreboard;

    public GameTracker(GTBEvents events) {
        super(events);

        scoreboard = new CustomScoreboard(this);

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
            if (game != null) {
                List<GTBEvents.FormattedName> formattedNames = new ArrayList<GTBEvents.FormattedName>();
                for (String playerName : e.players()) {
                    formattedNames.add(new GTBEvents.FormattedName(playerName));
                }
                game.onCorrectGuess(formattedNames);
            }
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

    private void onTick(Minecraft client) {
        if (game == null || !events.isInGtb()) return;
        game.players.forEach(player -> player.inactiveTicks++);
        CustomScoreboard.tickCounter++;
    }

    private static int getThemePointAward(String theme) {
        return (theme.length() < 6) ? 1 : (theme.length() < 9) ? 2 : 3;
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
        Set<Player> players = new HashSet<Player>();
        for (GTBEvents.InitialPlayerData p : event.players()) {
            players.add(new Player(p.name(), p.rankColor(), p.title(), p.emblem(), p.isUser()));
        }
        game = new Game(this, players);
    }

    public void onTick() {
        if (game != null) {
            game.onTick();
        }
    }

    public void clearGame() {
        game = null;
    }

    public void clearGameWithError(String error) {
        Utils.sendMessage("Error: " + error);
        game = null;
    }

    @Override
    public GTBEvents.Module.ErrorAction getErrorAction() {
        return GTBEvents.Module.ErrorAction.LOG_AND_CONTINUE;
    }

    public static class Player {
        public enum LeaverState { NORMAL, POTENTIAL_LEAVER, LEAVER }
        public String name;
        public String rank;
        public String prefix;
        public String title;
        public String emblem;
        public int[] points;
        public int buildRound;
        public boolean isUser;
        public int scoreMismatchCounter = 0;
        public int inactiveTicks = 0;
        public LeaverState leaverState = LeaverState.NORMAL;

        public Player(String name, String rankColor, String title, String emblem, boolean isUser) {
            this.name = name;

            this.rank = rankColor;
            if (this.rank == null || this.rank.isEmpty()) this.rank = EnumChatFormatting.WHITE.toString();
            this.prefix = rankColor;
            this.title = title;
            this.emblem = emblem;
            this.isUser = isUser;
            this.points = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            this.buildRound = 0;
        }
        
        // Simplified constructor for testing
        public Player(String name) {
            this(name, "white", "", "", false);
        }

        int getTotalPoints() {
            int sum = 0;
            for (int point : points) {
                sum += point;
            }
            return sum;
        }

        public int getCurrentRoundPoints(int currentRound) {
            if (currentRound <= 0 || currentRound > points.length) {
                return 0;
            }
            return points[currentRound - 1];
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

    public static class Game {
        GameTracker tracker;
        public Set<Player> players;

        public int currentRound = 0;
        public int totalRounds;
        public int skippedRounds = 0;
        public int potentialLeaverAmount = 0;
        public boolean userRoundSkipped = false;

        public String currentTheme = "";
        public String currentTimer = "";
        public Player currentBuilder = null;
        public int correctGuessesThisRound = 0;
        boolean oneSecondAlertReached = false;

        List<Utils.Pair<Player, Integer>> latestTrueScore;

        GTBEvents.GameState leaveState;
        int leaveRound = -1;
        Player leaveBuilder;

        public Game(GameTracker tracker, Set<Player> players) {
            this.tracker = tracker;
            this.players = players;
            this.totalRounds = players.size();
        }
        
        // Constructor for testing
        public Game() {
            this.players = new java.util.HashSet<>();
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
                for (Player p : players) {
                    if (p.points[currentRound - 1] == 0) {
                        p.leaverState = Player.LeaverState.LEAVER;
                    }
                }
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
            List<Utils.Pair<Player, Integer>> converted = new ArrayList<Utils.Pair<Player, Integer>>();
            for (GTBEvents.TrueScore trueScore : scores) {
                if (trueScore == null) continue;
                Player player = getPlayerFromName(trueScore.getPlayer());
                if (player == null) {
                    tracker.clearGameWithError("Player " + trueScore.getPlayer() + " not found in player list!");
                    return;
                }

                onActivity(player);

                if (verifyPoints(player, trueScore.getScore())) {
                    player.scoreMismatchCounter = 0;
                } else {
                    if (player.buildRound == currentRound && player.points[currentRound - 1] == 0
                            && correctGuessesThisRound != 0) {
                        currentBuilder.points[currentRound - 1] = trueScore.getScore() - player.getTotalPoints();
                    } else {
                        if (player.scoreMismatchCounter > 0) {
                            tracker.clearGameWithError("Score mismatch!");
                            return;
                        }
                        player.scoreMismatchCounter++;
                    }
                }
                converted.add(new Utils.Pair<Player, Integer>(player, trueScore.getScore()));
            }

            latestTrueScore = converted;

            List<Player> playersSortedByPoints = new ArrayList<Player>();
            for (Player p : players) {
                if (!p.leaverState.equals(Player.LeaverState.LEAVER)) {
                    playersSortedByPoints.add(p);
                }
            }
            Collections.sort(playersSortedByPoints, new Comparator<Player>() {
                @Override
                public int compare(Player p1, Player p2) {
                    return Integer.compare(p2.getTotalPoints(), p1.getTotalPoints());
                }
            });

            List<Utils.Pair<Player, Integer>> topNames = new ArrayList<Utils.Pair<Player, Integer>>();
            for (int i = 0; i < Math.min(3, latestTrueScore.size()); i++) {
                if (latestTrueScore.get(i) == null) continue;
                topNames.add(latestTrueScore.get(i));
            }

            if (topNames.isEmpty()) return;
            List<Player> guaranteedToAppearInScoreboard = new ArrayList<Player>();
            Map<Integer, List<Player>> pointsMap = new HashMap<Integer, List<Player>>();
            for (Player player : playersSortedByPoints) {
                if (!pointsMap.containsKey(player.getTotalPoints())) {
                    pointsMap.put(player.getTotalPoints(), new ArrayList<Player>());
                }
                pointsMap.get(player.getTotalPoints()).add(player);
            }

            List<Integer> sortedPoints = new ArrayList<Integer>(pointsMap.keySet());
            Collections.sort(sortedPoints, Collections.reverseOrder());

            for (Integer points : sortedPoints) {
                List<Player> pointsGroup = pointsMap.get(points);
                if (pointsGroup.size() > topNames.size() - guaranteedToAppearInScoreboard.size()) break;
                guaranteedToAppearInScoreboard.addAll(pointsGroup);
            }

            for (Player player : guaranteedToAppearInScoreboard) {
                boolean found = false;
                for (Utils.Pair<Player, Integer> p : topNames) {
                    if (p.a().equals(player)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
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

        public void onTick() {
            for (Player player : players) {
                if (!player.isUser && !player.leaverState.equals(Player.LeaverState.LEAVER)) {
                    player.inactiveTicks++;
                }
            }
        }

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
            for (Player player : players) {
                if (player.leaverState.equals(Player.LeaverState.POTENTIAL_LEAVER)) {
                    player.leaverState = Player.LeaverState.NORMAL;
                }
            }

            List<Player> candidatePlayers = new ArrayList<Player>();
            for (Player p : players) {
                if (p.buildRound <= 0 && !p.isUser && !Objects.equals(p, currentBuilder)) {
                    candidatePlayers.add(p);
                }
            }

            Collections.sort(candidatePlayers, new Comparator<Player>() {
                @Override
                public int compare(Player p1, Player p2) {
                    int leaverStateCompare = p2.getLeaverState().compareTo(p1.getLeaverState());
                    if (leaverStateCompare != 0) return leaverStateCompare;
                    return Integer.compare(p2.getInactiveTicks(), p1.getInactiveTicks());
                }
            });

            for (int i = 0; i < Math.min(potentialLeaverAmount, candidatePlayers.size()); i++) {
                Player p = candidatePlayers.get(i);
                if (!p.leaverState.equals(Player.LeaverState.LEAVER)) {
                    p.leaverState = Player.LeaverState.POTENTIAL_LEAVER;
                }
            }
        }

        private Player getPlayerFromName(String name) {
            for (Player p : players) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
            return null;
        }

        private int getThemePointAward(String theme) {
            return (theme.length() < 6) ? 1 : (theme.length() < 9) ? 2 : 3;
        }
    }
}