package com.aembr.guesstheutils;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;
import com.aembr.guesstheutils.interceptor.OriginalScoreboardCapture;

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
    private final Utils.FixedSizeBuffer<List<Utils.PlayerInfo>> playerListInfoEntryHistory = new Utils.FixedSizeBuffer<>(3);

    private static final String[] validEmblems = new String[]{"≈", "α", "Ω", "$", "π", "ƒ"};
    private static final String[] validTitles = new String[]{"Rookie", "Untrained", "Amateur", "Prospect", "Apprentice",
            "Experienced", "Seasoned", "Trained", "Skilled", "Talented", "Professional", "Pro", "Artisan", "Expert",
            "Master", "Legend", "Grandmaster", "Celestial", "Divine", "Ascended"};
    private static final String[] validLeaderboardTitles = new String[]{"#10", "#9", "#8", "#7", "#6", "#5", "#4", "#3", "#2",
            "#1"};
    private static final String[] validRanks = new String[]{"[VIP]", "[VIP+]", "[MVP]", "[MVP+]", "[MVP++]", "[YOUTUBE]"};
    private final String[] roundSkipMessages = new String[]{"The plot owner has left the game! Skipping...",
            "The plot owner is AFK! Skipping...", "The plot owner hasn't placed any blocks! Skipping..."};

    private boolean roundSkipped = false;
    private final Utils.FixedSizeBuffer<List<TrueScore>> trueScoreHistory = new Utils.FixedSizeBuffer<>(2);
    private List<TrueScore> trueScores = null;

    private Tick currentTick;
    public GameState gameState = GameState.NONE;

    private List<String> lobbyPlayerList = new ArrayList<>();
    private List<String> setupPlayerList = new ArrayList<>();
    private List<Utils.PlayerInfo> lobbyPlayerInfoList = new ArrayList<>();
    private List<Utils.PlayerInfo> setupPlayerInfoList = new ArrayList<>();
    private String currentBuilder = "";
    private String currentTheme = "";
    private String currentTimer = "";

    private String prematureBuilder;

    public void processTickUpdate(Tick tick) {
        currentTick = tick;
        emit(new TickUpdateEvent());

        if (tick.scoreboardLines != null) scoreboardLineHistory.add(tick.scoreboardLines);
                if (tick.playerListEntries != null) playerListEntryHistory.add(tick.playerListEntries);
        if (tick.playerListInfoEntries != null) playerListInfoEntryHistory.add(tick.playerListInfoEntries);
        
        if (tick.scoreboardLines != null) onScoreboardUpdate(tick.scoreboardLines);
        if (tick.playerListEntries != null) onPlayerListUpdate(tick.playerListEntries);
        if (tick.playerListInfoEntries != null) onPlayerListInfoUpdate(tick.playerListInfoEntries);
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

    private void onPlayerListInfoUpdate(List<Utils.PlayerInfo> playerListInfoEntries) {
        if (gameState.equals(GameState.LOBBY)) lobbyPlayerInfoList = playerListInfoEntries;
        if (gameState.equals(GameState.SETUP) && setupPlayerInfoList.isEmpty()) {
            setupPlayerInfoList = playerListInfoEntries;
        }
    }

    private String cleanUnicodeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\x20-\\x7E]", "");
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
                .filter(line -> (line.trim().startsWith("Starts In: ")
                        || line.trim().startsWith("Time: ")
                        || line.trim().startsWith("Next Round: ")))
                .reduce((first, second) -> second);
        if (timerLine.isPresent()) {
            String[] timerLineParts = Utils.stripFormatting(timerLine.get()).split(": ", 2);

            if (timerLineParts.length == 2) {
                String timeValue = timerLineParts[1].replaceAll("[^\\d:]", "");
                if (timeValue.matches("\\d{1,2}:\\d{2}") && !timeValue.equals(currentTimer)) {
                    currentTimer = timeValue;
                    emit(new TimerUpdateEvent(currentTimer));
                }
            }
        }

        // solve builder's name will not shown in the text when the builder is current user
        for (int i = 0; i < stringLines.size() - 1; i++) {
            String line = EnumChatFormatting.getTextWithoutFormattingCodes(stringLines.get(i)).trim();
            if (line.startsWith("Builder:")) {
                String currentBuilderName = cleanUnicodeString(EnumChatFormatting.getTextWithoutFormattingCodes(stringLines.get(i + 1))).trim();
                if (currentBuilderName != null && !currentBuilderName.isEmpty() && !currentBuilderName.equals(currentBuilder)) {
                    // if is the current user, trigger the builder change event
                    if (isCurrentUser(currentBuilderName)) {
                        if (gameState.equals(GameState.SETUP) || gameState.equals(GameState.LOBBY)) {
                            prematureBuilder = currentBuilderName;
                        } else {
                            emit(new BuilderChangeEvent(currentBuilder, currentBuilderName));
                            currentBuilder = currentBuilderName;
                        }
                    }
                }
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
        List<String> correctGuessers = new ArrayList<>();
        for (String messageString : chatMessages) {
            String strMessage = EnumChatFormatting.getTextWithoutFormattingCodes(messageString);
            if (strMessage == null || strMessage.isEmpty()) continue;

            if (gameState.equals(GameState.ROUND_PRE) && strMessage.startsWith("Round: ") && strMessage.contains("/")) {
                String[] currentOverTotal = strMessage.replace("Round: ", "").split("/");
                if (currentOverTotal[0].matches("\\d{1,2}") && currentOverTotal[1].matches("\\d{1,2}")) {
                    int current = Integer.parseInt(currentOverTotal[0]);
                    int total = Integer.parseInt(currentOverTotal[1]);
                    emit(new RoundStartEvent(current, total));
                    changeState(GameState.ROUND_BUILD);
                }
            }

            if (strMessage.startsWith("Builder: ")) {
                String builderName = strMessage.replace("Builder: ", "");
                if (!Objects.equals(currentBuilder, builderName)) {
                    if (gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END)) {
                        emit(new BuilderChangeEvent(currentBuilder, builderName));
                        currentBuilder = builderName;
                    } else if (gameState.equals(GameState.SETUP)) {
                        prematureBuilder = builderName;
                    }
                }
            }

            if (gameState.equals(GameState.ROUND_BUILD) && !strMessage.contains(":")
                    && strMessage.endsWith(" correctly guessed the theme!")) {
                String name = strMessage.replace(" correctly guessed the theme!", "");
                correctGuessers.add(name);
            }

            if (gameState.equals(GameState.ROUND_BUILD) && strMessage.startsWith("+") && strMessage.endsWith("(Correct Guess)")) {
                emit(new UserCorrectGuessEvent());
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

    private void onActionBarMessage(String actionBarMessage) {
        String strMessage = EnumChatFormatting.getTextWithoutFormattingCodes(actionBarMessage);
        if (strMessage == null || strMessage.isEmpty()) return;

        if (strMessage.startsWith("The theme is ") && gameState.equals(GameState.ROUND_BUILD)) {
            String theme = strMessage.replace("The theme is ", "");
            if (!currentTheme.equals(theme)) {
                emit(new ThemeUpdateEvent(theme));
                currentTheme = theme;
            }
        }
    }

    public String getCurrentThemeStruct() {
        String[] segments = currentTheme.split(" ");
        String result = "";
        for (String segment : segments) {
            if (result.isEmpty()) {
                result += segment.length();
            } else {
                result += "-" + segment.length();
            }
        }
        return result;
    }

    private void onTitleSet(String title) {
    }

    private void onSubtitleSet(String subtitle) {
        if (gameState.equals(GameState.ROUND_BUILD)
                && Objects.equals(EnumChatFormatting.getTextWithoutFormattingCodes(subtitle), "1 second remaining!")) {
            emit(new OneSecondAlertEvent());
        }
    }

    private void onScreenTitle(String screenTitle) {
        if (!(gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END))) return;
        if (screenTitle.equals("Select a theme to build!")) {
            if (scoreboardLineHistory.size() == 0) return;
            String builderName = getBuilderNameFromScoreboard(scoreboardLineHistory.get(0));
            if (Objects.equals(currentBuilder, builderName)) return;
            emit(new BuilderChangeEvent(currentBuilder, builderName));
            emit(new UserBuilderEvent());
            currentBuilder = builderName;
        }
    }

    public void changeState(GameState newState) {
        if (gameState == newState) return;

        GuessTheUtils.LOGGER.info("Change state from " + gameState + " to " + newState);
        if (newState.equals(GameState.LOBBY)) {
            if (playerListEntryHistory.size() > 0) {
                lobbyPlayerList = playerListEntryHistory.get(0);
            }
            if (playerListInfoEntryHistory.size() > 0) {
                lobbyPlayerInfoList = playerListInfoEntryHistory.get(0);
            }
        }

        if (newState.equals(GameState.ROUND_PRE)) {
            if (gameState.equals(GameState.SETUP) && !setupPlayerList.isEmpty()) {
                if (playerListEntryHistory.get(0).size() < 3) {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(1),
                               lobbyPlayerInfoList, setupPlayerInfoList, playerListInfoEntryHistory.get(1));
                } else {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(0),
                               lobbyPlayerInfoList, setupPlayerInfoList, playerListInfoEntryHistory.get(0));
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

    private InitialPlayerData parsePlayerData(String playerLine) {
        String name = extractPlayerName(playerLine);
        if (name == null) return null;

        String rankColor = extractRankColor(playerLine);
        String title = extractTitle(playerLine);
        String emblem = extractEmblem(playerLine);
        boolean isUser = isCurrentUser(name);

        return new InitialPlayerData(name, rankColor, title, emblem, isUser);
    }

    private InitialPlayerData parsePlayerInfoData(Utils.PlayerInfo playerInfo) {
        String name = playerInfo.name;
        if (name == null) return null;

        String rankColor = extractRankColor(playerInfo.prefix);
        String title = extractTitle(playerInfo.prefix);
        String emblem = extractEmblem(playerInfo.suffix);
        boolean isUser = isCurrentUser(name);

        return new InitialPlayerData(name, rankColor, title, emblem, isUser);
    }

    private String extractPlayerName(String line) {
        return line.replaceAll("\\[.*?\\]", "").trim();
    }

    private String extractRankColor(String line) {
        return line;
    }

    public static String extractTitle(String line) {
        for (String title : validTitles) {
            if (line.contains(title)) {
                return title;
            }
        }
        return "";
    }

    public boolean isBoldTitle(String line) {
        // get the index of line in validTitles
        int index = Arrays.asList(validTitles).indexOf(line);
        if (index == -1) return false;

        // if the title is equal or greater than the index of Legend, return isBold
        return index >= Arrays.asList(validTitles).indexOf("Legend");
    }

    public static String extractEmblem(String line) {
        for (String emblem : validEmblems) {
            if (line.contains(emblem)) {
                return line;
                // return emblem; 
                // use prefix instead
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

    public GameState getStateFromScoreboard(List<String> scoreboardLines) {
        if (scoreboardLines.isEmpty()) return null;

        if (scoreboardLines.contains("Mode: Guess The Build") || isInLobby()) return GameState.LOBBY;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("1. "))) return GameState.POST_GAME;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Starts In: 00:"))) return GameState.ROUND_PRE;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Next Round: 00:0"))) return GameState.ROUND_END;
        if (scoreboardLines.stream()
                .anyMatch(line -> line.startsWith("Time: 0") && line.length() > 7)) return GameState.ROUND_BUILD;
        return null;
    }

    private String getThemeFromScoreboard(List<String> scoreboardLines) {
        int themeLine = scoreboardLines.indexOf("Theme:");
        if (themeLine == -1 || scoreboardLines.size() <= themeLine + 1) return "";

        String themeLineString = scoreboardLines.get(themeLine + 1);
        if (!themeLineString.startsWith(" ")) return "";
        if (themeLineString.trim().isEmpty()) return "";
        if (themeLineString.equals(" ???")) return "";
        return themeLineString.substring(1);
    }

    private String getPlayerNameFromMessage(String message) {
        if (!message.contains(": ")) return null;
        String[] nameParts = message.split(": ")[0].split(" ");
        if (message.startsWith("[GUESSER CHAT]")) {
            return nameParts[nameParts.length - 1];
        } else {
            if (Arrays.stream(validEmblems).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            if (Arrays.stream(validTitles).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            if (Arrays.stream(validLeaderboardTitles)
                    .anyMatch(e -> e.equals(nameParts[0]) && nameParts[1].equals("Builder"))) {
                return nameParts[nameParts.length - 1];
            }
            if (Arrays.stream(validRanks).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            if (nameParts.length == 1 && !nameParts[0].equals("Builder") && !nameParts[0].equals("Round")) {
                return nameParts[0];
            }
        }
        return null;
    }

    private String getBuilderNameFromScoreboard(List<String> scoreboardLines) {
        List<String> strLines = scoreboardLines.stream().map(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line)).collect(java.util.stream.Collectors.toList());
        int builderLine = strLines.indexOf("Builder:");
        if (builderLine == -1 || strLines.size() <= builderLine + 1) return "";

        String builderLineString = strLines.get(builderLine + 1);
        if (!builderLineString.startsWith(" ")) return "";

        return builderLineString.substring(1);
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    private void onGameStart(List<String> lobbyList, List<String> setupList, List<String> finalList,
                            List<Utils.PlayerInfo> lobbyInfoList, List<Utils.PlayerInfo> setupInfoList, List<Utils.PlayerInfo> finalInfoList) {
        Set<InitialPlayerData> players = new HashSet<>();
        
        if (finalInfoList != null && !finalInfoList.isEmpty()) {
            for (Utils.PlayerInfo playerInfo : finalInfoList) {
                if (playerInfo.name.trim().isEmpty()) continue;
                
                InitialPlayerData playerData = parsePlayerInfoData(playerInfo);
                if (playerData != null) {
                    players.add(playerData);
                }
            }
        } else {
            for (String playerEntry : finalList) {
                String cleanEntry = EnumChatFormatting.getTextWithoutFormattingCodes(playerEntry);
                if (cleanEntry.trim().isEmpty()) continue;
                
                InitialPlayerData playerData = parsePlayerData(cleanEntry);
                if (playerData != null) {
                    players.add(playerData);
                }
            }
        }

        currentBuilder = null;
        emit(new GameStartEvent(players));

        lobbyPlayerList = new ArrayList<>();
        setupPlayerList = new ArrayList<>();
        lobbyPlayerInfoList = new ArrayList<>();
        setupPlayerInfoList = new ArrayList<>();

        if (prematureBuilder != null) {
            emit(new BuilderChangeEvent(null, prematureBuilder));
            currentBuilder = prematureBuilder;
            prematureBuilder = null;
        }
    }

    private void onGameEnd(List<String> scoreboardLines) {
        Map<String, Integer> actualScores = new HashMap<>();
        scoreboardLines.stream().map(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line))
                .filter(line -> line != null && !line.trim().isEmpty())
                .map(line -> line.split("\\. ", 2))
                .filter(parts -> parts.length > 1)
                .map(parts -> parts[1].split(": ", 2))
                .filter(parts -> parts.length > 1 && parts[0].length() < 16)
                .forEach(parts -> {
                    try { actualScores.put(parts[0], Integer.valueOf(parts[1])); }
                    catch (NumberFormatException ignored) {}
                });
        emit(new GameEndEvent(actualScores));
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
                    // Remove color codes before checking the text content
                    String cleanDisplayName = EnumChatFormatting.getTextWithoutFormattingCodes(displayName);
                    return cleanDisplayName.toLowerCase().contains("guess the build");
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isInLobby() {
        if (!isInGtb()) return false;

        // Check if the scoreboard objective is "PreScoreboard"
        String originalObjective = OriginalScoreboardCapture.getOriginalScoreboardObjective();
        return originalObjective != null && originalObjective.trim().equals("PreScoreboard");
    }
}
