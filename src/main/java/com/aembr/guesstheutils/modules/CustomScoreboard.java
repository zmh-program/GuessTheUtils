package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class CustomScoreboard implements GTBEvents.EventListener {
    private GTBEvents events;

    private GTBEvents.GameState state = GTBEvents.GameState.NONE;
    final List<Player> players = new ArrayList<>();
    private int currentRound = 0;
    private int skippedRounds = 0;
    private int potentialLeaverAmount;
    private boolean userRoundSkipped = false;

    /// How long should a player be inactive for, until they're marked as such
    private final int inactivePlayerThresholdSeconds = 180;

    private String currentTheme = "";
    private Player currentBuilder = null;
    private int correctGuessesThisRound = 0;
    private boolean oneSecondAlertReached = false;

    private List<Utils.Pair<Player, Integer>> latestTrueScore;

    // when the user leaves mid-game, we need to remember these things for when they join back
    // if while they were gone there was a possibility for anyone to get points, we can't guarantee accurate tracking
    // so we inform the user, and fall back to the vanilla scoreboard
    // TODO: this can be improved with a more sophisticated analysis maybe?
    private GTBEvents.GameState leaveState;
    private int leaveRound = -1;
    private Player leaveBuilder;

    public CustomScoreboard(GTBEvents events) {
        this.events = events;

        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);

        events.subscribe(GTBEvents.GameStartEvent.class, this);
        events.subscribe(GTBEvents.StateChangeEvent.class, this);
        events.subscribe(GTBEvents.BuilderChangeEvent.class, this);
        events.subscribe(GTBEvents.RoundStartEvent.class, this);
        events.subscribe(GTBEvents.RoundEndEvent.class, this);
        events.subscribe(GTBEvents.RoundSkipEvent.class, this);
        events.subscribe(GTBEvents.CorrectGuessEvent.class, this);
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, this);
        events.subscribe(GTBEvents.GameEndEvent.class, this);
        events.subscribe(GTBEvents.TrueScoresUpdateEvent.class, this);
        events.subscribe(GTBEvents.UserLeaveEvent.class, this);
        events.subscribe(GTBEvents.UserRejoinEvent.class, this);
        events.subscribe(GTBEvents.TickUpdateEvent.class, this);
        events.subscribe(GTBEvents.PlayerChatEvent.class, this);
        events.subscribe(GTBEvents.OneSecondAlertEvent.class, this);
    }

    private void onTick(MinecraftClient minecraftClient) {
        if (!players.isEmpty() && (state.equals(GTBEvents.GameState.ROUND_PRE)
                || state.equals(GTBEvents.GameState.ROUND_BUILD) || state.equals(GTBEvents.GameState.ROUND_END))) {
            players.forEach(player -> {
                player.inactiveTicks++;

                if (player.inactiveTicks >= inactivePlayerThresholdSeconds * 20
                        && player.state.equals(Player.State.NORMAL)
                        && !player.isUser && !Objects.equals(currentBuilder, player)
                        && latestTrueScore != null
                        && latestTrueScore.stream().noneMatch(entry -> entry.a() == player)
                ){
                    player.state = Player.State.INACTIVE;
                }
            });
        }
    }

    @Override
    public void onEvent(GTBEvents.BaseEvent event) {
        if (!(event instanceof GTBEvents.TickUpdateEvent)) {
            System.out.println(event);
        }

        if (event instanceof GTBEvents.GameStartEvent) {
            players.clear();
            for (GTBEvents.InitialPlayerData playerData : ((GTBEvents.GameStartEvent) event).players()) {
                players.add(new Player(this, playerData.name(), playerData.isUser()));
            }
            currentBuilder = null;
            currentRound = 0;
            potentialLeaverAmount = 0;
            skippedRounds = 0;
            oneSecondAlertReached = false;
            clearLeaveState();
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

        if (event instanceof GTBEvents.OneSecondAlertEvent) {
            oneSecondAlertReached = true;
        }

        if (event instanceof GTBEvents.PlayerChatEvent) {
            Player player = getPlayerFromName(((GTBEvents.PlayerChatEvent) event).player());
            assert player != null;
            player.onActivity();
        }

        if (event instanceof GTBEvents.RoundSkipEvent) {
            currentBuilder.buildRound = -1;
            skippedRounds++;
        }

        if (event instanceof GTBEvents.BuilderChangeEvent) {
            String builderName = ((GTBEvents.BuilderChangeEvent) event).current();
            if (builderName != null) {
                Player builder = getPlayerFromName(builderName);
                assert builder != null : "Builder " + builderName + " not found in players list!";
                currentBuilder = builder;
                currentBuilder.onActivity();
                return;
            }
            currentBuilder = null;
        }

        if (event instanceof GTBEvents.RoundStartEvent) {
            currentRound = ((GTBEvents.RoundStartEvent) event).currentRound();
            oneSecondAlertReached = false;

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

            int missing = players.size() - skippedRounds - ((GTBEvents.RoundStartEvent) event).totalRounds();
            if (missing != potentialLeaverAmount) {
                potentialLeaverAmount = missing;
                updatePotentialLeavers();
            }
        }

        if (event instanceof GTBEvents.RoundEndEvent) {
            boolean skipped = (currentBuilder.isUser && userRoundSkipped) || ((GTBEvents.RoundEndEvent) event).skipped();
            if (!skipped && !oneSecondAlertReached) {
                System.out.println("Round ended prematurely and wasn't skipped!");
                players.stream().filter(p -> p.points[currentRound - 1] == 0)
                        .forEach(p -> {
                            p.state = Player.State.LEAVER;
                            //System.out.println("Setting " + p.name + " to leaver, because they didn't guess.");
                        });
            }
            userRoundSkipped = false;
        }

        if (event instanceof GTBEvents.CorrectGuessEvent) {
            for (String playerName : ((GTBEvents.CorrectGuessEvent) event).players()) {
                Player player = getPlayerFromName(playerName);
                assert player != null : "Player " + playerName + " not found in players list!";
                player.points[currentRound - 1] = Math.max(1, 3 - correctGuessesThisRound);
                player.onActivity();
                correctGuessesThisRound++;
            }

            assert currentBuilder != null;

            if (currentBuilder.points[currentRound - 1] == 0 && !currentTheme.isEmpty()) {
                currentBuilder.points[currentRound - 1] = getThemePointAward(currentTheme);
            }
        }

        if (event instanceof GTBEvents.ThemeUpdateEvent) {
            currentTheme = ((GTBEvents.ThemeUpdateEvent) event).theme();

            assert currentBuilder != null;

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
            List<Utils.Pair<Player, Integer>> converted = new ArrayList<>();
            for (Utils.Pair<String, Integer> trueScore : ((GTBEvents.TrueScoresUpdateEvent) event).scores()) {
                // System.out.println("Checking true score: " + trueScore);
                if (trueScore == null) continue;
                Player player = getPlayerFromName(trueScore.a());
                assert player != null;

                if (player.state.equals(Player.State.LEAVER) || player.state.equals(Player.State.INACTIVE)) {
                    player.state = Player.State.NORMAL;
                }

                if (!verifyPoints(trueScore.a(), trueScore.b())) {
                    if (player.buildRound == currentRound && player.points[currentRound - 1] == 0
                            && correctGuessesThisRound != 0) {
                        currentBuilder.points[currentRound - 1] = trueScore.b() - player.getTotalPoints();
                    }
                }
                converted.add(new Utils.Pair<>(player, trueScore.b()));
            }

            latestTrueScore = converted;

            // Detect players who should appear in the top 3, but don't (confirmed leavers)
            List<Player> playersSortedByPoints = players.stream()
                    .filter(p -> !p.state.equals(Player.State.LEAVER))
                    .sorted((p1, p2) -> Integer.compare(p2.getTotalPoints(), p1.getTotalPoints())).toList();

            List<Utils.Pair<Player, Integer>> topNames = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
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
                    player.state = Player.State.LEAVER;
                    //System.out.println("Setting " + player.name + " to leaver, because they should appear in the scoreboard.");
                }
            }
        }

        if (event instanceof GTBEvents.UserLeaveEvent) {
            leaveState = state;
            leaveRound = currentRound;
            leaveBuilder = currentBuilder;

            if (leaveBuilder.isUser && leaveState.equals(GTBEvents.GameState.ROUND_BUILD)) {
                userRoundSkipped = true;
            }
        }
    }

    void updatePotentialLeavers() {
        players.stream().filter(player -> player.state.equals(Player.State.POTENTIAL_LEAVER))
                .forEach(player -> player.state = Player.State.NORMAL);

        players.stream()
                .filter(p -> p.buildRound <= 0)
                .filter(p -> !p.isUser)
                .filter(p -> !Objects.equals(p, currentBuilder))
                .filter(p -> {
                    if (latestTrueScore == null) return true;
                    else return latestTrueScore.stream().noneMatch(score -> score.a().equals(p));
                    })
                .sorted(Comparator.comparing((Player p) -> p.state == Player.State.LEAVER ? 0 : 1)
                        .thenComparingInt(p -> p.inactiveTicks).reversed())
                .limit(potentialLeaverAmount)
                .filter(p -> !p.state.equals(Player.State.LEAVER))
                .forEach(p -> {
                    p.state = Player.State.POTENTIAL_LEAVER;
                    //System.out.println("Setting " + p.name + " to potential leaver.");
                });
    }

    private boolean verifyPoints(String name, Integer expectedPoints) {
        Player player = getPlayerFromName(name);
        assert player != null : "Player " + name + " not found in players list!";
        // System.out.println(player.getPointsSummed() + " " + expectedPoints);
        return player.getTotalPoints() == expectedPoints;
    }

    private Player getPlayerFromName(String name) {
        return players.stream().filter(p -> p.name.equals(name)).findAny().orElse(null);
    }

    private int getThemePointAward(String theme) {
        return (theme.length() < 6) ? 1 : (theme.length() < 9) ? 2 : 3;
    }

    public void drawScoreboard(DrawContext ctx) {
        if (players.isEmpty() || state.equals(GTBEvents.GameState.NONE) || state.equals(GTBEvents.GameState.LOBBY)
                || state.equals(GTBEvents.GameState.POST_GAME)) return;

        int round = state.equals(GTBEvents.GameState.ROUND_PRE) || currentRound == 0 ? currentRound + 1 : currentRound;
        if (round > players.size()) return;

        TextRenderer renderer = GuessTheUtils.CLIENT.textRenderer;

        List<Player> sortedByPoints = players.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getTotalPoints(), p1.getTotalPoints())).toList();

        List<Text> lines = new ArrayList<>();
        int width = 0;
        for (int i = 0; i < sortedByPoints.size(); i++) {
            Player player = sortedByPoints.get(i);
            String line = String.format("%d %d %s: ", i + 1, player.buildRound, player.name);
            if (player.points[round - 1] == 0) {
                line += player.getTotalPoints();
            } else {
                line += String.format("+%d %d", player.points[round - 1], player.getTotalPoints());
            }

            MutableText textLine = Text.literal(line);
            if (player.state.equals(Player.State.NORMAL)) {
                textLine.formatted(Formatting.WHITE);
            } else if (player.state.equals(Player.State.INACTIVE)) {
                textLine.formatted(Formatting.GRAY);
            } else {
                textLine.formatted(Formatting.DARK_GRAY);
            }


            int lineWidth = renderer.getWidth(line);
            if (lineWidth > width) width = lineWidth;
            lines.add(textLine);
        }

        int height = renderer.fontHeight * lines.size();
        int x = ctx.getScaledWindowWidth() - width - 4;
        int startY = ctx.getScaledWindowHeight() - height - 20;

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

    static class Player {
        enum State { NORMAL, INACTIVE, POTENTIAL_LEAVER, LEAVER }

        CustomScoreboard customScoreboard;
        String name;
        int[] points;
        int buildRound;
        boolean isUser;

        public int inactiveTicks = 0;
        public State state = State.NORMAL;

        public Player(CustomScoreboard customScoreboard, String name, boolean isUser) {
            this.name = name;
            this.points = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            this.buildRound = 0;
            this.isUser = isUser;
            this.customScoreboard = customScoreboard;
        }

        public int getTotalPoints() {
            return Arrays.stream(points).sum();
        }

        public void onActivity() {
            inactiveTicks = 0;
            if (state.equals(State.POTENTIAL_LEAVER)) {
                customScoreboard.updatePotentialLeavers();
            } else if (state.equals(State.LEAVER) || state.equals(State.INACTIVE)) {
                state = State.NORMAL;
            }
        }

        @Override
        public String toString() {
            return "Player{" +
                    "customScoreboard=" + customScoreboard +
                    ", name='" + name + '\'' +
                    ", points=" + Arrays.toString(points) +
                    ", buildRound=" + buildRound +
                    ", isUser=" + isUser +
                    ", inactiveTicks=" + inactiveTicks +
                    ", state=" + state +
                    '}';
        }
    }
}
