package com.aembr.guesstheutils;

import net.minecraft.util.EnumChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class GTBEvents {
    public interface BaseEvent {}
    
    public static record GameStartEvent(Set<InitialPlayerData> players) implements BaseEvent {}
    public static record BuilderChangeEvent(String previous, String current) implements BaseEvent {}
    public static record UserBuilderEvent() implements BaseEvent {}
    public static record RoundStartEvent(int currentRound, int totalRounds) implements BaseEvent {}
    public static record CorrectGuessEvent(List<String> players) implements BaseEvent {}
    public static record RoundEndEvent(boolean skipped) implements BaseEvent {}
    public static record RoundSkipEvent() implements BaseEvent {}
    public static record ThemeUpdateEvent(String theme) implements BaseEvent {}
    public static record GameEndEvent(Map<String, Integer> scores) implements BaseEvent {}
    public static record StateChangeEvent(GameState previous, GameState current) implements BaseEvent {}
    public static record TrueScoresUpdateEvent(List<TrueScore> scores) implements BaseEvent {}
    public static record UserLeaveEvent() implements BaseEvent {}
    public static record UserRejoinEvent() implements BaseEvent {}
    public static record TickUpdateEvent() implements BaseEvent {}
    public static record PlayerChatEvent(String player, String message) implements BaseEvent {}
    public static record OneSecondAlertEvent() implements BaseEvent {}
    public static record TimerUpdateEvent(String timer) implements BaseEvent {}
    public static record UserCorrectGuessEvent() implements BaseEvent {}

    private final Map<Consumer<?>, Module> modules = new HashMap<>();
    private final Map<Class<? extends BaseEvent>, List<Consumer<?>>> subscribers = new HashMap<>();

    public enum GameState { NONE, LOBBY, SETUP, ROUND_PRE, ROUND_BUILD, ROUND_END, POST_GAME }

    private final Utils.FixedSizeBuffer<List<String>> scoreboardLineHistory = new Utils.FixedSizeBuffer<>(3);
    private final Utils.FixedSizeBuffer<List<String>> playerListEntryHistory = new Utils.FixedSizeBuffer<>(3);

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
    private final Utils.FixedSizeBuffer<List<TrueScore>> trueScoreHistory = new Utils.FixedSizeBuffer<>(2);
    private List<TrueScore> trueScores = null;

    private Tick currentTick;
    public GameState gameState = GameState.NONE;

    private List<String> lobbyPlayerList = new ArrayList<>();
    private List<String> setupPlayerList = new ArrayList<>();
    private String currentBuilder = "";
    private String currentTheme = "";
    private String currentTimer = "";

    private String prematureBuilder;

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

    private void onPlayerListUpdate(List<String> playerListEntries) {
        if (gameState.equals(GameState.LOBBY)) lobbyPlayerList = playerListEntries;
        if (gameState.equals(GameState.SETUP) && setupPlayerList.isEmpty()) {
            setupPlayerList = playerListEntries;
        }
    }

    private void onScoreboardUpdate(List<String> scoreboardLines) {
        List<String> stringLines = scoreboardLines.stream().map(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line)).toList();
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

        Optional<String> timerLine = stringLines.stream()
                .filter(line -> (line.startsWith("Starts In: ")
                        || line.startsWith("Time: ")
                        || line.startsWith("Next Round: ")))
                .reduce((first, second) -> second);
        if (timerLine.isPresent()) {
            String[] timerLineParts = timerLine.get().split(": ", 2);
            if (timerLineParts.length == 2
                    && timerLineParts[1].matches("\\d{2}:\\d{2}")
                    && !timerLineParts[1].equals(currentTimer)) {
                currentTimer = timerLineParts[1];
                emit(new TimerUpdateEvent(currentTimer));
            }
        }

        if (gameState.equals(GameState.ROUND_BUILD)) {
            String theme = getThemeFromScoreboard(stringLines);
            if (theme != null && !theme.equals(currentTheme)) {
                currentTheme = theme;
                emit(new ThemeUpdateEvent(currentTheme));
            }
        }
    }

    private void onChatMessages(List<String> chatMessages) {
        for (String messageString : chatMessages) {
            String strippedMessage = EnumChatFormatting.getTextWithoutFormattingCodes(messageString);

            if (strippedMessage.startsWith("Builder: ")) {
                String builder = strippedMessage.substring(9).trim();
                onBuilderChange(builder);
            }

            if (strippedMessage.endsWith(" guessed the word!")) {
                String playerName = strippedMessage.substring(0, strippedMessage.length() - 18);
                onCorrectGuess(Collections.singletonList(playerName));
            }

            if (Arrays.asList(roundSkipMessages).contains(strippedMessage)) {
                roundSkipped = true;
                emit(new RoundSkipEvent());
            }

            if (strippedMessage.equals("1 second remaining!")) {
                emit(new OneSecondAlertEvent());
            }

            String playerName = extractPlayerNameFromChat(strippedMessage);
            if (playerName != null) {
                emit(new PlayerChatEvent(playerName, strippedMessage));
            }
        }
    }

    private void onActionBarMessage(String actionBarMessage) {
    }

    private void onTitleSet(String title) {
    }

    private void onSubtitleSet(String subtitle) {
    }

    private void onScreenTitle(String screenTitle) {
    }

    private void changeState(GameState newState) {
        if (gameState.equals(newState)) return;

        GameState previousState = gameState;
        gameState = newState;

        emit(new StateChangeEvent(previousState, newState));

        if (newState.equals(GameState.SETUP) && !setupPlayerList.isEmpty()) {
            processGameStart();
        }

        if (newState.equals(GameState.ROUND_PRE)) {
            currentTheme = "";
        }

        if (newState.equals(GameState.ROUND_END)) {
            emit(new RoundEndEvent(roundSkipped));
            roundSkipped = false;
        }
    }

    private void processGameStart() {
        Set<InitialPlayerData> players = new HashSet<>();
        
        for (String entry : setupPlayerList) {
            String cleanEntry = EnumChatFormatting.getTextWithoutFormattingCodes(entry);
            if (cleanEntry.trim().isEmpty()) continue;
            
            InitialPlayerData playerData = parsePlayerData(cleanEntry);
            if (playerData != null) {
                players.add(playerData);
            }
        }

        if (!players.isEmpty()) {
            emit(new GameStartEvent(players));
        }

        if (prematureBuilder != null) {
            onBuilderChange(prematureBuilder);
            prematureBuilder = null;
        }
    }

    private InitialPlayerData parsePlayerData(String playerLine) {
        String name = extractPlayerName(playerLine);
        if (name == null) return null;

        String rankColor = extractRankColor(playerLine);
        String title = extractTitle(playerLine);
        String emblem = extractEmblem(playerLine);
        boolean isUser = isCurrentUser(name);

        return new InitialPlayerData(name, rankColor, title, emblem, isUser);
    }

    private String extractPlayerName(String line) {
        return line.replaceAll("\\[.*?\\]", "").trim();
    }

    private String extractRankColor(String line) {
        return "WHITE";
    }

    private String extractTitle(String line) {
        for (String title : validTitles) {
            if (line.contains(title)) {
                return title;
            }
        }
        return "";
    }

    private String extractEmblem(String line) {
        for (String emblem : validEmblems) {
            if (line.contains(emblem)) {
                return emblem;
            }
        }
        return "";
    }

    private boolean isCurrentUser(String name) {
        return GuessTheUtils.CLIENT.thePlayer != null && 
               GuessTheUtils.CLIENT.thePlayer.getName().equals(name);
    }

    private void onBuilderChange(String newBuilder) {
        if (gameState.equals(GameState.NONE) || gameState.equals(GameState.LOBBY)) {
            prematureBuilder = newBuilder;
            return;
        }

        String previousBuilder = currentBuilder;
        currentBuilder = newBuilder;
        emit(new BuilderChangeEvent(previousBuilder, newBuilder));

        if (isCurrentUser(newBuilder)) {
            emit(new UserBuilderEvent());
        }
    }

    private void onCorrectGuess(List<String> players) {
        emit(new CorrectGuessEvent(players));

        boolean userGuessed = players.stream().anyMatch(this::isCurrentUser);
        if (userGuessed) {
            emit(new UserCorrectGuessEvent());
        }
    }

    private GameState getStateFromScoreboard(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Lobby")) return GameState.LOBBY;
            if (line.contains("Round") && line.contains("/")) return GameState.ROUND_BUILD;
            if (line.contains("Next Round")) return GameState.ROUND_END;
            if (line.contains("Winners")) return GameState.POST_GAME;
        }
        return null;
    }

    private String getThemeFromScoreboard(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("Word: ") || line.startsWith("Theme: ")) {
                return line.substring(line.indexOf(": ") + 2);
            }
        }
        return null;
    }

    private String extractPlayerNameFromChat(String message) {
        if (message.contains(": ")) {
            String prefix = message.substring(0, message.indexOf(": "));
            return EnumChatFormatting.getTextWithoutFormattingCodes(prefix).trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void subscribe(Class<T> eventClass, Consumer<T> consumer, Module module) {
        modules.put(consumer, module);
        subscribers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(consumer);
    }

    @SuppressWarnings("unchecked")
    private void emit(BaseEvent event) {
        List<Consumer<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (Consumer<?> subscriber : eventSubscribers) {
                try {
                    ((Consumer<BaseEvent>) subscriber).accept(event);
                } catch (Exception e) {
                    GuessTheUtils.LOGGER.error("Error in event subscriber", e);
                }
            }
        }
    }

    public static abstract class Module {
        protected GTBEvents events;

        public Module(GTBEvents events) {
            this.events = events;
        }
    }

    public static class InitialPlayerData {
        private final String name;
        private final String rankColor;
        private final String title;
        private final String emblem;
        private final boolean isUser;

        public InitialPlayerData(String name, String rankColor, String title, String emblem, boolean isUser) {
            this.name = name;
            this.rankColor = rankColor;
            this.title = title;
            this.emblem = emblem;
            this.isUser = isUser;
        }

        public String name() { return name; }
        public String rankColor() { return rankColor; }
        public String title() { return title; }
        public String emblem() { return emblem; }
        public boolean isUser() { return isUser; }
    }

    public static class TrueScore {
        private final String player;
        private final int score;

        public TrueScore(String player, int score) {
            this.player = player;
            this.score = score;
        }

        public String getPlayer() { return player; }
        public int getScore() { return score; }
    }
} 