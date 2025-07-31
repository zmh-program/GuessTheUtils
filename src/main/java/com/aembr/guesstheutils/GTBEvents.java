package com.aembr.guesstheutils;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;
import java.util.function.Consumer;

public class GTBEvents {
    public interface BaseEvent {}
    
    public static class GameStartEvent implements BaseEvent {
        private final Set<InitialPlayerData> players;
        public GameStartEvent(Set<InitialPlayerData> players) { this.players = players; }
        public Set<InitialPlayerData> players() { return players; }
    }
    
    public static class BuilderChangeEvent implements BaseEvent {
        private final String previous, current;
        public BuilderChangeEvent(String previous, String current) { this.previous = previous; this.current = current; }
        public String previous() { return previous; }
        public String current() { return current; }
    }
    
    public static class UserBuilderEvent implements BaseEvent {}
    
    public static class RoundStartEvent implements BaseEvent {
        private final int currentRound, totalRounds;
        public RoundStartEvent(int currentRound, int totalRounds) { this.currentRound = currentRound; this.totalRounds = totalRounds; }
        public int currentRound() { return currentRound; }
        public int totalRounds() { return totalRounds; }
    }
    
    public static class CorrectGuessEvent implements BaseEvent {
        private final List<String> players;
        public CorrectGuessEvent(List<String> players) { this.players = players; }
        public List<String> players() { return players; }
    }
    
    public static class RoundEndEvent implements BaseEvent {
        private final boolean skipped;
        public RoundEndEvent(boolean skipped) { this.skipped = skipped; }
        public boolean skipped() { return skipped; }
    }
    
    public static class RoundSkipEvent implements BaseEvent {}
    
    public static class ThemeUpdateEvent implements BaseEvent {
        private final String theme;
        public ThemeUpdateEvent(String theme) { this.theme = theme; }
        public String theme() { return theme; }
    }
    
    public static class GameEndEvent implements BaseEvent {
        private final Map<String, Integer> scores;
        public GameEndEvent(Map<String, Integer> scores) { this.scores = scores; }
        public Map<String, Integer> scores() { return scores; }
    }
    
    public static class StateChangeEvent implements BaseEvent {
        private final GameState previous, current;
        public StateChangeEvent(GameState previous, GameState current) { this.previous = previous; this.current = current; }
        public GameState previous() { return previous; }
        public GameState current() { return current; }
    }
    
    public static class TrueScoresUpdateEvent implements BaseEvent {
        private final List<TrueScore> scores;
        public TrueScoresUpdateEvent(List<TrueScore> scores) { this.scores = scores; }
        public List<TrueScore> scores() { return scores; }
    }
    
    public static class UserLeaveEvent implements BaseEvent {}
    public static class UserRejoinEvent implements BaseEvent {}
    public static class TickUpdateEvent implements BaseEvent {}
    
    public static class PlayerChatEvent implements BaseEvent {
        private final String player, message;
        public PlayerChatEvent(String player, String message) { this.player = player; this.message = message; }
        public String player() { return player; }
        public String message() { return message; }
    }
    
    public static class OneSecondAlertEvent implements BaseEvent {}
    
    public static class TimerUpdateEvent implements BaseEvent {
        private final String timer;
        public TimerUpdateEvent(String timer) { this.timer = timer; }
        public String timer() { return timer; }
    }
    
    public static class UserCorrectGuessEvent implements BaseEvent {}

    private final Map<Consumer<?>, Module> modules = new HashMap<>();
    private final Map<Class<? extends BaseEvent>, List<Consumer<?>>> subscribers = new HashMap<>();

    public enum GameState { NONE, LOBBY, SETUP, ROUND_PRE, ROUND_BUILD, ROUND_END, POST_GAME }

    private final Utils.FixedSizeBuffer<List<String>> scoreboardLineHistory = new Utils.FixedSizeBuffer<>(3);
    private final Utils.FixedSizeBuffer<List<String>> playerListEntryHistory = new Utils.FixedSizeBuffer<>(3);

    private final String[] validEmblems = new String[]{"~", "a", "O", "$", "p", "f"};
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
        if (tick.chatMessages != null) {
            System.out.println("Chat Messages: " + tick.chatMessages);
        }
        if (tick.actionBarMessage != null) {
            System.out.println("Action Bar Message: " + tick.actionBarMessage);
        }
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
        List<String> stringLines = scoreboardLines.stream().map(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line)).collect(java.util.stream.Collectors.toList());
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
                Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && mc.thePlayer.getName().equals(name);
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
        public enum ErrorAction {
            STOP,
            RESTART,
            LOG_AND_CONTINUE
        }

        protected GTBEvents events;

        public Module(GTBEvents events) {
            this.events = events;
        }

        public ErrorAction getErrorAction() {
            return ErrorAction.STOP;
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

    public static class FormattedName {
        private final String name;

        public FormattedName(String name) {
            this.name = name;
        }

        public String name() { return name; }
    }

    public boolean isInGtb() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) {
                return false;
            }

            if (GuessTheUtils.debugMode) {
                return true;
            }
            
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) {
                return false;
            }
            
            // Check sidebar scoreboard (slot 1)
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective != null) {
                String displayName = sidebarObjective.getDisplayName();
                if (displayName != null) {
                    // Check if sidebar title contains "Guess The Build" (case insensitive)
                    return displayName.toLowerCase().contains("guess the build");
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
} 