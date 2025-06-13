package com.aembr.guesstheutils;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class GTBEvents {
    public interface BaseEvent {}
    /// Emitted as soon as the setup phase is complete and all player info has been collected.
    public record GameStartEvent(Set<InitialPlayerData> players) implements BaseEvent {}
    /// Emitted when the current builder changes. Can be null if the builder has left.
    public record BuilderChangeEvent(String previous, String current) implements BaseEvent {}
    /// Emitted once the builder has picked a theme.
    public record RoundStartEvent(int currentRound, int totalRounds) implements BaseEvent {}
    /// Emitted when one or more players guess correctly. Contains all players that guessed during the same tick.
    public record CorrectGuessEvent(List<String> players) implements BaseEvent {}
    /// Emitted when the building and guessing part of the round has concluded.
    public record RoundEndEvent(boolean skipped) implements BaseEvent {}
    /// Emitted when the builder leaves before picking a theme. Always followed by `BuilderChangeEvent`.
    public record RoundSkipEvent() implements BaseEvent {}
    /// Emitted when the theme hint updates or the theme is revealed.
    public record ThemeUpdateEvent(String theme) implements BaseEvent {}
    /// Emitted when the game is over and all scores are displayed.
    public record GameEndEvent(Map<String, Integer> scores) implements BaseEvent {}
    /// Emitted when the game state changes. See `GameState` for all possible states.
    public record StateChangeEvent(GameState previous, GameState current) implements BaseEvent {}
    /// Emitted when the true scores, as seen on the vanilla scoreboard, have updated.
    /// The first 3 elements are reserved for the top 3 players, and the 4th for the user (if not in top 3).
    public record TrueScoresUpdateEvent(List<Utils.Pair<String, Integer>> scores) implements BaseEvent {}
    ///  Emitted when the user leaves a game that's in progress.
    public record UserLeaveEvent() implements BaseEvent {}
    /// Emitted when the user rejoins a game that's in progress.
    public record UserRejoinEvent() implements BaseEvent {}
    /// Emitted when a new tick update arrives.
    public record TickUpdateEvent() implements BaseEvent {}
    /// Emitted when a player sends any chat message.
    public record PlayerChatEvent(String player, String message) implements BaseEvent {}
    //public record PlayerChatEvent(PlayerChatMessage message) implements BaseEvent {}
    /// Emitted when the 1-second remaining in round alert is received.
    public record OneSecondAlertEvent() implements BaseEvent {}

    private final Map<Class<? extends BaseEvent>, List<EventListener>> listeners = new HashMap<>();

    public enum GameState { NONE, LOBBY, SETUP, ROUND_PRE, ROUND_BUILD, ROUND_END, POST_GAME }

    private final Utils.FixedSizeBuffer<List<Text>> scoreboardLineHistory = new Utils.FixedSizeBuffer<>(3);
    private final Utils.FixedSizeBuffer<List<Text>> playerListEntryHistory = new Utils.FixedSizeBuffer<>(3);

    private final String[] validEmblems = new String[]{"≈", "α", "Ω", "$", "π", "ƒ"};
    private final String[] validTitles = new String[]{"Rookie", "Untrained", "Amateur", "Prospect", "Apprentice",
            "Experienced", "Seasoned", "Trained", "Skilled", "Talented", "Professional", "Artisan", "Expert",
            "Master", "Legend", "Grandmaster", "Celestial", "Divine", "Ascended"};
    private final String[] validLeaderboardTitles = new String[]{"#10", "#9", "#8", "#7", "#6", "#5", "#4", "#3", "#2",
            "#1"};
    private final String[] validRanks = new String[]{"[VIP]", "[VIP+]", "[MVP]", "[MVP+]", "[MVP++]", "[YOUTUBE]"};
    private final String[] roundSkipMessages = new String[]{"The plot owner has left the game! Skipping...",
            "The plot owner is AFK! Skipping...", "The plot owner hasn't placed any blocks! Skipping..."};

    private boolean roundSkipped = false;

    /// We only consider a true score entry valid if it remains the same for at least 2 ticks.
    /// Random corruption that only lasts a tick is surprisingly common, as is slight de-sync between server and tracker.
    private final Utils.FixedSizeBuffer<List<Utils.Pair<String, Integer>>> trueScoreHistory = new Utils.FixedSizeBuffer<>(2);
    private List<Utils.Pair<String, Integer>> trueScores = null;

    private Tick currentTick;
    public GameState gameState = GameState.NONE;

    private List<Text> lobbyPlayerList = new ArrayList<>();
    private List<Text> setupPlayerList = new ArrayList<>();
    private String currentBuilder = "";
    private String currentTheme = "";

    public void processTickUpdate(Tick tick) {
        currentTick = tick;
        emit(new TickUpdateEvent());

        if (tick.scoreboardLines != null) scoreboardLineHistory.add(tick.scoreboardLines);
        if (tick.playerListEntries != null) playerListEntryHistory.add(tick.playerListEntries);

        if (tick.scoreboardLines != null) onScoreboardUpdate(tick.scoreboardLines);
        if (tick.playerListEntries != null) onPlayerListUpdate(tick.playerListEntries);
        if (tick.chatMessages != null) onChatMessages(tick.chatMessages);
        if (tick.actionBarMessage != null) onActionBarMessage(tick.actionBarMessage);
        if (tick.title != null) onTitleSet(tick.title);
        if (tick.subtitle != null) onSubtitleSet(tick.subtitle);
        if (tick.screenTitle != null) onScreenTitle(tick.screenTitle);
    }

    private void onPlayerListUpdate(List<Text> playerListEntries) {
        if (gameState.equals(GameState.LOBBY)) lobbyPlayerList = playerListEntries;
        if (gameState.equals(GameState.SETUP) && setupPlayerList.isEmpty()) {
            //if (playerListEntries.stream().noneMatch(entry -> entry.getSiblings().isEmpty())) return;
            if (playerListEntries.stream().filter(entry -> !entry.getSiblings().isEmpty()).count() != 1) return;
            setupPlayerList = playerListEntries;
        }
    }

    private void onScoreboardUpdate(List<Text> scoreboardLines) {
        List<String> stringLines = scoreboardLines.stream().map(line -> Formatting.strip(line.getString())).toList();
        GameState state = getStateFromScoreboard(stringLines);

        if (gameState.equals(GameState.LOBBY) && state == null) {
            changeState(GameState.SETUP);
            return;
        }

        if (gameState.equals(GameState.SETUP) && (state == null || state.equals(GameState.NONE))) {
            return;
        }

        if (state != null) {
            changeState(state);
        }

        // Extract theme
        if (gameState.equals(GameState.ROUND_BUILD)) {
            String theme = getThemeFromScoreboard(stringLines);
            if (!theme.isEmpty() && !currentTheme.equals(theme)) {
                emit(new ThemeUpdateEvent(theme));
                currentTheme = theme;
            }
        }

        // Extract true scores
        if (gameState.equals(GameState.ROUND_BUILD) || gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END)) {
            List<Utils.Pair<String, Integer>> trueScoreEntries = getTrueScoresFromScoreboard(stringLines);
            //System.out.println("Got true scores from scoreboard: " + trueScoreEntries);
            //System.out.println("True score history is " + trueScoreHistory.size());
            if (trueScoreHistory.size() == 2) {
                if (trueScoreEntries.equals(trueScoreHistory.get(0))
                        && trueScoreEntries.equals(trueScoreHistory.get(1))
                        && !Objects.equals(trueScoreEntries, trueScores)) {
                    trueScores = trueScoreEntries;

                    emit(new TrueScoresUpdateEvent(trueScores));
                }
            }
            trueScoreHistory.add(trueScoreEntries);
        }
    }

    private void onGameStart(List<Text> lobbyList, List<Text> setupList, List<Text> finalList) {
//        System.out.println("lobby list: " + lobbyList);
//        System.out.println("setup list: " + setupList);
//        System.out.println("final list: " + finalList);

        Set<InitialPlayerData> players = new HashSet<>();
        for (Text playerEntry : finalList) {
            String name = playerEntry.getSiblings().isEmpty() ?
                    playerEntry.getString() : playerEntry.getSiblings().get(1).getLiteralString();

            Text title = playerEntry.getSiblings().isEmpty() ?
                    null : playerEntry.getSiblings().get(0).getSiblings().get(0);

            Text emblem = playerEntry.getSiblings().isEmpty() ?
                    null : Text.empty();

            if (emblem != null && !playerEntry.getSiblings().get(2).getSiblings().isEmpty()) {
                emblem = playerEntry.getSiblings().get(2).getSiblings()
                        .get(playerEntry.getSiblings().get(2).getSiblings().size() - 1);
            }

            Text lobbyEntry = lobbyList.stream().filter(
                    entry -> Objects.equals(Formatting.strip(entry.getString()), name)).findFirst().orElse(null);
            assert lobbyEntry != null : name + "'s lobbyEntry is null!";
            TextColor styleColor = lobbyEntry.getStyle().getColor();
            if (styleColor == null) {
                continue;
            }
            Formatting rankColor = Formatting.byName(styleColor.getName());

            Text setupEntry = setupList.stream().filter(
                    entry -> Objects.equals(Formatting.strip(entry.getString()), name)).findFirst().orElse(null);

            boolean isUser = setupEntry != null && !setupEntry.getStyle().isEmpty();

            players.add(new InitialPlayerData(name, title, emblem, rankColor, isUser));
        }

        currentBuilder = null;
        emit(new GameStartEvent(players));

        lobbyPlayerList = new ArrayList<>();
        setupPlayerList = new ArrayList<>();
    }

    private void onGameEnd(List<Text> scoreboardLines) {
        Map<String, Integer> actualScores = new HashMap<>();
        scoreboardLines.stream().map(line -> Formatting.strip(line.getString()))
                .filter(line -> line != null && !line.isBlank())
                .map(line -> line.split("\\. ", 2))
                .filter(parts -> parts.length > 1)
                .map(parts -> parts[1].split(": ", 2))
                .filter(parts -> parts.length > 1 && parts[0].length() < 16)
                .forEach(parts -> {
                    try { actualScores.put(parts[0], Integer.valueOf(parts[1])); }
                    catch (NumberFormatException ignored) {} //yep, score can be malformed and cause a crash
                });
        emit(new GameEndEvent(actualScores));
    }

    private void onScreenTitle(Text screenTitle) {
        if (!(gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END))) return;
        if (screenTitle.getString().equals("Select a theme to build!")) {
            if (scoreboardLineHistory.size() == 0) return;
            String builderName = getBuilderNameFromScoreboard(scoreboardLineHistory.get(0));
            if (Objects.equals(currentBuilder, builderName)) return;
            emit(new BuilderChangeEvent(currentBuilder, builderName));
            currentBuilder = builderName;
        }
    }

    private String getPlayerNameFromMessage(String message) {
        if (!message.contains(": ")) return null;
        String[] nameParts = message.split(": ")[0].split(" ");
        if (message.startsWith("[GUESSER CHAT]")) {
            return nameParts[nameParts.length - 1];
        } else {
            // check if first part is a valid emblem
            if (Arrays.stream(validEmblems).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, let's check if it's a valid title
            if (Arrays.stream(validTitles).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, maybe there's a multi-word title (leaderboard)
            if (Arrays.stream(validLeaderboardTitles)
                    .anyMatch(e -> e.equals(nameParts[0]) && nameParts[1].equals("Builder"))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, let's check if it's a valid rank (for when and titles don't appear at end of round)
            if (Arrays.stream(validRanks).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            // if they don't even have a rank, then there should only be one name part - their name
            // however, we have to manually exclude a few possibilities
            if (nameParts.length == 1 && !nameParts[0].equals("Builder") && !nameParts[0].equals("Round")) {
                return nameParts[0];
            }
        }
        return null;
    }

    // TODO: implement and use this instead of `getPlayerNameFromMessage`
    private PlayerChatMessage validatePlayerChatMessage(Text message) {
        throw new NotImplementedException();
    }

    private void onSubtitleSet(Text subtitle) {
        if (gameState.equals(GameState.ROUND_BUILD)
                && Objects.equals(Formatting.strip(subtitle.getString()), "1 second remaining!")) {
            emit(new OneSecondAlertEvent());
        }
    }

    private void onTitleSet(Text title) {
    }

    private void onActionBarMessage(Text actionBarMessage) {
        String strMessage = Formatting.strip(actionBarMessage.getString());
        if (strMessage == null || strMessage.isEmpty()) return;

        if (strMessage.startsWith("The theme is ") && gameState.equals(GameState.ROUND_BUILD)) {
            String theme = strMessage.replace("The theme is ", "");
            if (!currentTheme.equals(theme)) {
                emit(new ThemeUpdateEvent(theme));
                currentTheme = theme;
            }
        }
    }

    private void onChatMessages(List<Text> chatMessages) {
        List<String> correctGuessers = new ArrayList<>();
        for (Text message : chatMessages) {
            String strMessage = Formatting.strip(message.getString());
            if (strMessage == null || strMessage.isEmpty()) continue;

            if (gameState.equals(GameState.ROUND_PRE) && strMessage.startsWith("Round: ") && strMessage.contains("/")) {
                String[] currentOverTotal = strMessage.replace("Round: ", "").split("/");
                if (!currentOverTotal[0].matches("\\d{1,2}") && !currentOverTotal[1].matches("\\d{1,2}")) {
                    return;
                }

                int current = Integer.parseInt(currentOverTotal[0]);
                int total = Integer.parseInt(currentOverTotal[1]);
                emit(new RoundStartEvent(current, total));
            }

            if ((gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END))
                    && strMessage.startsWith("Builder: ")) {
                String builderName = strMessage.replace("Builder: ", "");
                if (Objects.equals(currentBuilder, builderName)) return;
                emit(new BuilderChangeEvent(currentBuilder, builderName));
                currentBuilder = builderName;
            }

            if (gameState.equals(GameState.ROUND_BUILD) && !strMessage.contains(":")
                    && strMessage.endsWith(" correctly guessed the theme!")) {
                correctGuessers.add(strMessage.replace(" correctly guessed the theme!", ""));
            }

            if (Arrays.asList(roundSkipMessages).contains(strMessage)) {
                roundSkipped = true;
            }

            if (gameState.equals(GameState.ROUND_BUILD) && strMessage.startsWith("The theme was: ")) {
                String theme = strMessage.replace("The theme was: ", "").replace("!", "");
                if (!currentTheme.equals(theme)) {
                    emit(new ThemeUpdateEvent(theme));
                    currentTheme = theme;
                }
            }

            if (strMessage.startsWith("Welcome back! You are building something relevant to the theme ")) {
                emit(new UserRejoinEvent());
                emit(new RoundEndEvent(false));
            }

            if (strMessage.startsWith("Welcome back! It is ") && strMessage.endsWith("'s turn to build. You have to guess the build!")) {
                String builder = strMessage.replace("Welcome back! It is ", "")
                        .replace("'s turn to build. You have to guess the build!", "");

                if (!currentBuilder.equals(builder)) {
                    emit(new BuilderChangeEvent(currentBuilder, builder));
                    currentBuilder = builder;
                }
                emit(new UserRejoinEvent());
            }

            if (gameState.equals(GameState.ROUND_PRE) && strMessage.equals("The owner left! Skipping...")) {
                emit(new RoundSkipEvent());
                emit(new BuilderChangeEvent(currentBuilder, null));
            }

            if (gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_BUILD)
                    || gameState.equals(GameState.ROUND_END)) {
                String playerName = getPlayerNameFromMessage(strMessage);
                if (playerName != null) {
                    String playerMessage = strMessage.split(": ", 2)[1];
                    emit(new PlayerChatEvent(playerName, playerMessage));
                }
            }
        }
        if (!correctGuessers.isEmpty()) emit(new CorrectGuessEvent(correctGuessers));
    }

    private String getBuilderNameFromScoreboard(List<Text> scoreboardLines) {
        List<String> strLines = scoreboardLines.stream().map(line -> Formatting.strip(line.getString())).toList();
        int builderLine = strLines.indexOf("Builder:");
        if (builderLine == -1 || strLines.size() <= builderLine + 1) return "";

        String builderLineString = strLines.get(builderLine + 1);
        if (!builderLineString.startsWith(" ")) return "";

        return builderLineString.substring(1);
    }

    private String getThemeFromScoreboard(List<String> scoreboardLines) {
        int themeLine = scoreboardLines.indexOf("Theme:");
        if (themeLine == -1 || scoreboardLines.size() <= themeLine + 1) return "";

        String themeLineString = scoreboardLines.get(themeLine + 1);
        if (!themeLineString.startsWith(" ")) return "";
        if (themeLineString.isBlank()) return "";
        if (themeLineString.equals(" ???")) return "";
        return themeLineString.substring(1);
    }

    public GameState getStateFromScoreboard(List<String> scoreboardLines) {
        if (scoreboardLines.isEmpty()) return null;
        if (!scoreboardLines.get(0).equals("GUESS THE BUILD")) return GameState.NONE;
        if (scoreboardLines.size() == 1) return GameState.NONE;
        if (scoreboardLines.contains("Mode: Guess The Build")) return GameState.LOBBY;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("1. "))) return GameState.POST_GAME;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Starts In: 00:"))) return GameState.ROUND_PRE;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Next Round: 00:0"))) return GameState.ROUND_END;
        if (scoreboardLines.stream()
                // technically, it's possible for a player named "Time" to be on the scoreboard with 0 points
                .anyMatch(line -> line.startsWith("Time: 0") && line.length() > 7)) return GameState.ROUND_BUILD;
        return null;
    }

    public List<Utils.Pair<String, Integer>> getTrueScoresFromScoreboard(List<String> scoreboardLines) {
        List<Utils.Pair<String, Integer>> trueScores = new ArrayList<>();

        scoreboardLines.stream()
                .filter(line -> line.split(":").length == 2)
                .map(line -> line.split(": "))
                .filter(parts -> parts.length == 2)
                .forEach(parts -> {
                    if (!parts[0].contains(" ") && parts[1].matches("\\d{1,2}")) {
                        trueScores.add(new Utils.Pair<>(parts[0], Integer.parseInt(parts[1])));
                    } else {
                        trueScores.add(null);
                    }
                });
        return trueScores;
    }

    public void changeState(GameState newState) {
        if (gameState == newState) return;

        if (newState.equals(GameState.LOBBY)) {
            if (playerListEntryHistory.size() > 0) {
                lobbyPlayerList = playerListEntryHistory.get(0);
            }
        }

        if (newState.equals(GameState.ROUND_PRE)) {
            if (gameState.equals(GameState.SETUP) && !setupPlayerList.isEmpty()) {
                if (playerListEntryHistory.get(0).size() < 3) {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(1));
                } else {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(0));
                }
            }
            currentTheme = "";
        }

        if (newState.equals(GameState.POST_GAME)) {
            onGameEnd(scoreboardLineHistory.get(0));
        }

        if (gameState.equals(GameState.ROUND_BUILD) && newState.equals(GameState.ROUND_END)) {
            emit(new RoundEndEvent(roundSkipped));
            roundSkipped = false;
        }

        if (newState.equals(GameState.NONE)
                && (gameState.equals(GameState.ROUND_PRE)
                || gameState.equals(GameState.ROUND_BUILD)
                || gameState.equals(GameState.ROUND_END))) {
            emit(new UserLeaveEvent());
        }

        emit(new StateChangeEvent(gameState, newState));
        gameState = newState;
    }

    public interface EventListener {
        void onEvent(BaseEvent event);
    }

    public <T extends BaseEvent> void subscribe(Class<T> eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void emit(BaseEvent event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }

    public record InitialPlayerData(String name, Text title, Text emblem, Formatting rankColor, boolean isUser) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InitialPlayerData that = (InitialPlayerData) o;
            return isUser == that.isUser && Objects.equals(title, that.title) && Objects.equals(name, that.name)
                    && Objects.equals(emblem, that.emblem) && rankColor == that.rankColor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, emblem, rankColor, isUser);
        }

        @Override
        public String toString() {
            return "InitialPlayerData{" + "name='" + name + '\'' + ", title=" + title + ", emblem=" + emblem +
                    ", rankColor=" + rankColor + ", isUser=" + isUser + '}';
        }
    }

    public record PlayerChatMessage(String name, Text title, Text emblem, Text rank, String message) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PlayerChatMessage that = (PlayerChatMessage) o;
            return Objects.equals(rank, that.rank) && Objects.equals(title, that.title) && Objects.equals(name, that.name) && Objects.equals(emblem, that.emblem) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, emblem, rank, message);
        }

        @Override
        public String toString() {
            return "PlayerChatMessage{" +
                    "name='" + name + '\'' +
                    ", title=" + title +
                    ", emblem=" + emblem +
                    ", rank=" + rank +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
