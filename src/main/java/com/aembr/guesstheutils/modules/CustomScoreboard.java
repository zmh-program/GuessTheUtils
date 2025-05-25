package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Utils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CustomScoreboard implements GTBEvents.EventListener {
    private GTBEvents events;

    private GTBEvents.GameState state = GTBEvents.GameState.NONE;
    private final List<Player> players = new ArrayList<>();
    private int currentRound = 0;
    private String currentTheme = "";
    private Player currentBuilder = null;
    private int correctGuessesThisRound = 0;

    // when the user leaves mid-game, we need to remember these things for when they join back
    // if while they were gone there was a possibility for anyone to get points, we can't guarantee accurate tracking
    // so we inform the user, and fall back to the vanilla scoreboard
    // TODO: this can be improved with a more sophisticated analysis maybe?
    private GTBEvents.GameState leaveState;
    private int leaveRound = -1;
    private Player leaveBuilder;

    public boolean drawTest = false;
    private final List<Player> drawTestPlayers = List.of(
            new Player("Yria", new int[]{0, 0, 0, 1}, 2),
            new Player("Meow", new int[]{3, 1, 0, 1}, 0),
            new Player("ReallyLongName44", new int[]{1, 1, 1, 1}, 1),
            new Player("hello", new int[]{0, 3, 2, 1}, 0),
            new Player("help_me", new int[]{1, 2, 3, 2}, 3)
    );

    public CustomScoreboard(GTBEvents events) {
        this.events = events;
        events.subscribe(GTBEvents.GameStartEvent.class, this);
        events.subscribe(GTBEvents.StateChangeEvent.class, this);
        events.subscribe(GTBEvents.BuilderChangeEvent.class, this);
        events.subscribe(GTBEvents.RoundStartEvent.class, this);
        events.subscribe(GTBEvents.RoundEndEvent.class, this);
        events.subscribe(GTBEvents.CorrectGuessEvent.class, this);
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, this);
        events.subscribe(GTBEvents.GameEndEvent.class, this);
        events.subscribe(GTBEvents.TrueScoresUpdateEvent.class, this);
        events.subscribe(GTBEvents.UserLeaveEvent.class, this);
        events.subscribe(GTBEvents.UserRejoinEvent.class, this);
    }

    @Override
    public void onEvent(GTBEvents.BaseEvent event) {
        if (event instanceof GTBEvents.GameStartEvent) {
            players.clear();
            for (GTBEvents.InitialPlayerData playerData : ((GTBEvents.GameStartEvent) event).players()) {
                players.add(new Player(playerData.name(), playerData.isUser()));
            }
            leaveState = null;
            leaveRound = -1;
            leaveBuilder = null;
        }

        if (event instanceof GTBEvents.StateChangeEvent) {
            state = ((GTBEvents.StateChangeEvent) event).current();
            //System.out.println(state);
        }

        if (event instanceof GTBEvents.UserRejoinEvent) {
            if (leaveState == null || leaveRound == -1 || leaveBuilder == null || players.isEmpty()) {
                Utils.sendMessage("Tracking is disabled for this game, as you weren't present at the start (most commonly caused by restarting the game).");
                return;
            }
            if (currentBuilder.isUser) {
                clearLeaveState();
                return;
            }

            if (leaveState.equals(GTBEvents.GameState.ROUND_BUILD) || state.equals(GTBEvents.GameState.ROUND_BUILD)) {
                Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                clearLeaveState();
                players.clear();
                return;
            }

            if (currentBuilder.equals(leaveBuilder)) {
                if (!state.equals(leaveState)) {
                    Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                    clearLeaveState();
                    players.clear();
                    return;
                }
                clearLeaveState();
                return;
            } else {
                if (leaveState.equals(GTBEvents.GameState.ROUND_END) && state.equals(GTBEvents.GameState.ROUND_PRE)) {
                    Utils.sendMessage("Checking if tracking data for this game is still valid...");
                    return;
                }
                else {
                    Utils.sendMessage("Players may have gained points while you were absent. Falling back to vanilla scoreboard.");
                    clearLeaveState();
                    players.clear();
                }
            }
        }

        if (players.isEmpty()) return;

        if (event instanceof GTBEvents.RoundSkipEvent) {
            currentBuilder.buildRound = -1;
        }

        if (event instanceof GTBEvents.BuilderChangeEvent) {
            String builderName = ((GTBEvents.BuilderChangeEvent) event).current();
            if (builderName != null) {
                Player builder = getPlayerFromName(builderName);
                assert builder != null : "Builder " + builderName + " not found in players list!";
                currentBuilder = builder;
                return;
            }
            currentBuilder = null;
        }

        if (event instanceof GTBEvents.RoundStartEvent) {
            currentRound = ((GTBEvents.RoundStartEvent) event).currentRound();

            // this check will only be true if the user left during the end of one round,
            // and rejoined before the start of another. we just need to check if they haven't skipped any rounds
            if (leaveRound != -1) {
                if (currentRound == leaveRound + 1) {
                    Utils.sendMessage("Tracking data valid!");
                    clearLeaveState();
                } else {
                    Utils.sendMessage("Tracking data invalid! Falling back to vanilla scoreboard.");
                    clearLeaveState();
                    players.clear();
                    return;
                }
            }

            assert currentBuilder != null : "Current builder is null!";
            currentBuilder.buildRound = currentRound;
            currentTheme = "";
            correctGuessesThisRound = 0;
        }

        if (event instanceof GTBEvents.RoundEndEvent) {
            //System.out.println(players);
        }

        if (event instanceof GTBEvents.CorrectGuessEvent) {
            for (String playerName : ((GTBEvents.CorrectGuessEvent) event).players()) {
                Player player = getPlayerFromName(playerName);
                assert player != null : "Player " + playerName + " not found in players list!";
                player.points[currentRound - 1] = Math.max(1, 3 - correctGuessesThisRound);
                correctGuessesThisRound++;
            }

            if (currentBuilder.points[currentRound - 1] == 0 && !currentTheme.isEmpty()) {
                currentBuilder.points[currentRound - 1] = getThemePointAward(currentTheme);
            }
        }

        if (event instanceof GTBEvents.ThemeUpdateEvent) {
            currentTheme = ((GTBEvents.ThemeUpdateEvent) event).theme();
            if (currentBuilder.points[currentRound - 1] == 0 && correctGuessesThisRound > 0) {
                currentBuilder.points[currentRound - 1] = getThemePointAward(currentTheme);
            }
        }

        if (event instanceof GTBEvents.GameEndEvent) {
            for (Map.Entry<String, Integer> expected : ((GTBEvents.GameEndEvent) event).scores().entrySet()) {
                assert verifyPoints(expected.getKey(), expected.getValue());
            }
            players.clear();
        }

        if (event instanceof GTBEvents.TrueScoresUpdateEvent) {
            for (Utils.Pair<String, Integer> trueScore : ((GTBEvents.TrueScoresUpdateEvent) event).scores()) {
                // System.out.println("Checking true score: " + trueScore);
                if (trueScore == null) continue;
                if (!verifyPoints(trueScore.a(), trueScore.b())) {
                    Player player = getPlayerFromName(trueScore.a());
                    if (player.buildRound == currentRound && player.points[currentRound - 1] == 0
                            && correctGuessesThisRound != 0) {
                        currentBuilder.points[currentRound - 1] = trueScore.b() - player.getPointsSummed();
                    }
                }
            }
        }

        if (event instanceof GTBEvents.UserLeaveEvent) {
            leaveState = state;
            leaveRound = currentRound;
            leaveBuilder = currentBuilder;
        }
    }

    private boolean verifyPoints(String name, Integer expectedPoints) {
        Player player = getPlayerFromName(name);
        assert player != null : "Player " + name + " not found in players list!";
        // System.out.println(player.getPointsSummed() + " " + expectedPoints);
        return player.getPointsSummed() == expectedPoints;
    }

    private Player getPlayerFromName(String name) {
        return players.stream().filter(p -> p.name.equals(name)).findAny().orElse(null);
    }

    private int getThemePointAward(String theme) {
        return (theme.length() < 6) ? 1 : (theme.length() < 9) ? 2 : 3;
    }

    public void drawScoreboard(DrawContext ctx) {
        if ((players.isEmpty() || state.equals(GTBEvents.GameState.NONE) || state.equals(GTBEvents.GameState.LOBBY)
                || state.equals(GTBEvents.GameState.POST_GAME)) && !drawTest) return;

        int round = state.equals(GTBEvents.GameState.ROUND_PRE) ? currentRound + 1 : currentRound;

        TextRenderer renderer = GuessTheUtils.CLIENT.textRenderer;

        List<Player> playerList = drawTest ? drawTestPlayers : players;
        List<Player> sortedByPoints = playerList.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getPointsSummed(), p1.getPointsSummed())).toList();

        List<String> lines = new ArrayList<>();
        int width = 0;
        for (int i = 0; i < sortedByPoints.size(); i++) {
            Player player = sortedByPoints.get(i);
            String line = String.format("%d %d %s: ", i + 1, player.buildRound, player.name);
            if (player.points[round - 1] == 0) {
                line += player.getPointsSummed();
            } else {
                line += String.format("+%d %d", player.points[round - 1], player.getPointsSummed());
            }

            int lineWidth = renderer.getWidth(line);
            if (lineWidth > width) width = lineWidth;
            lines.add(line);
        }

        int height = renderer.fontHeight * lines.size();
        int x = ctx.getScaledWindowWidth() - width - 4;
        int startY = ctx.getScaledWindowHeight() - height - 50;

        for (int i = 0; i < lines.size(); i++) {
            int y = startY + i * renderer.fontHeight;
            ctx.drawText(renderer, lines.get(i), x, y, 0xFFFFFFFF, true);
        }
    }

    private void clearLeaveState() {
        leaveState = null;
        leaveRound = -1;
        leaveBuilder = null;
    }

    private static class Player {
        String name;
        int[] points;
        int buildRound;
        boolean isUser;

        public Player(String name, boolean isUser) {
            this.name = name;
            this.points = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            this.buildRound = 0;
            this.isUser = isUser;
        }

        public Player(String name, int[] points, int buildRound) {
            this.name = name;
            this.points = points;
            this.buildRound = buildRound;
        }

        public int getPointsSummed() {
            return Arrays.stream(points).sum();
        }

        @Override
        public String toString() {
            return "Player{" +
                    "name='" + name + '\'' +
                    ", points=" + Arrays.toString(points) +
                    ", buildRound=" + buildRound +
                    '}';
        }
    }
}
