package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.Utils;
import net.minecraft.client.Minecraft;

import java.util.*;

public class GameTracker extends GTBEvents.Module {
    public static GTBEvents.GameState state = GTBEvents.GameState.NONE;

    Game game;
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

    public void onTick() {
        if (game != null) {
            game.onTick();
        }
    }

    public static class Player {
        public final String name;
        public final String rankColor;
        public final String title;
        public final String emblem;
        public final boolean isUser;

        public Player(String name, String rankColor, String title, String emblem, boolean isUser) {
            this.name = name;
            this.rankColor = rankColor;
            this.title = title;
            this.emblem = emblem;
            this.isUser = isUser;
        }
    }

    public static class Game {
        private GameTracker tracker;
        private Set<Player> players;
        public String currentTheme = "";
        public String currentTimer = "";
        public GTBEvents.GameState leaveState;
        public int leaveRound = -1;
        public String leaveBuilder;

        public Game(GameTracker tracker, Set<Player> players) {
            this.tracker = tracker;
            this.players = players;
        }

        public void onBuilderChange(String builder) {
        }

        public void onRoundStart(int currentRound, int totalRounds) {
        }

        public void onRoundEnd(boolean skipped) {
        }

        public void onRoundSkipped() {
        }

        public void onCorrectGuess(List<String> players) {
        }

        public void onThemeUpdate(String theme) {
            this.currentTheme = theme;
        }

        public void onGameEnd(Map<String, Integer> scores) {
        }

        public void onTrueScoresUpdate(Map<String, Integer> scores) {
        }

        public void onUserLeave() {
            this.leaveState = GameTracker.state;
            this.leaveRound = 0; // TODO: track current round
        }

        public void onUserRejoin() {
        }

        public void onPlayerChat(String player, String message) {
        }

        public void onOneSecondAlert() {
        }

        public void onTick() {
        }
    }
} 