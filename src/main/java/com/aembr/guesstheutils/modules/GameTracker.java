package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameTracker extends GTBEvents.Module {
    /// How long should a player be inactive for, for them to be marked as such
    final int inactivePlayerThresholdSeconds = 180;

    GTBEvents.GameState state = GTBEvents.GameState.NONE;
    Game game;

    public GameTracker(GTBEvents events) {
        super(events);

        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);

        events.subscribe(GTBEvents.GameStartEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.StateChangeEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.BuilderChangeEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.RoundStartEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.RoundEndEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.RoundSkipEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.CorrectGuessEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.ThemeUpdateEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.GameEndEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.TrueScoresUpdateEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.UserLeaveEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.UserRejoinEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.TickUpdateEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.PlayerChatEvent.class, this::onEvent, this);
        events.subscribe(GTBEvents.OneSecondAlertEvent.class, this::onEvent, this);
    }

    public void onEvent(GTBEvents.BaseEvent event) {
        if (event instanceof GTBEvents.GameStartEvent) {
            onGameStart(((GTBEvents.GameStartEvent) event).players());
            return;
        }

        if (event instanceof GTBEvents.StateChangeEvent) {
            state = ((GTBEvents.StateChangeEvent) event).current();
            return;
        }

        if (event instanceof GTBEvents.UserRejoinEvent) {
            onUserRejoin();
            return;
        }

        if (game == null) return;

        if (event instanceof GTBEvents.OneSecondAlertEvent) {
            game.rounds.get(game.currentRound - 1).oneSecondAlertReached = true;
            return;
        }

        if (event instanceof GTBEvents.PlayerChatEvent) {
            game.onPlayerActivity(((GTBEvents.PlayerChatEvent) event).player());
            return;
        }

        if (event instanceof GTBEvents.RoundSkipEvent) {
            game.onRoundSkipped();
        }
    }

    private void onTick(MinecraftClient client) {
        throw new NotImplementedException();
    }

    private void onGameStart(Set<GTBEvents.InitialPlayerData> players) {
        throw new NotImplementedException();
    }

    private void onUserRejoin() {
        throw new NotImplementedException();
    }

    static class PlayerAlias {
        enum InactiveState { NORMAL, INACTIVE, POTENTIAL_LEAVER, LEAVER }

        String name;
        Formatting rankColor;
        Text title;
        Text emblem;

        PlayerAlias.InactiveState inactiveState = PlayerAlias.InactiveState.NORMAL;
        int inactiveTicks = 0;
        int buildRound = 0;
        int[] points = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };


        public PlayerAlias(String name, Formatting rankColor, Text title, Text emblem) {
            this.name = name;
            this.rankColor = rankColor;
            this.title = title;
            this.emblem = emblem;
        }
    }

    static class Player {
        /// Players can play under multiple aliases throughout the game, using the /nick command.
        /// Our goal is to associate each alias to a player, hence such a seemingly convoluted approach.
        PlayerAlias latestAlias;
        List<PlayerAlias> unconfirmedAliases = new ArrayList<>();
        List<PlayerAlias> confirmedAliases = new ArrayList<>();
        boolean isUser;

        public Player(String name, Formatting rankColor, Text title, Text emblem, Boolean isUser) {
            this.latestAlias = new PlayerAlias(name, rankColor, title, emblem);
            this.confirmedAliases.add(latestAlias);
            this.isUser = isUser;
        }
    }

    static class Round {
        /// Guesses are stored as clusters so that we can try other permutations in case there is a score mismatch.
        List<GuessCluster> correctGuesses = new ArrayList<>();
        PlayerAlias builder;
        boolean skipped = false;
        String theme = "";
        boolean oneSecondAlertReached = false;

        public Round(PlayerAlias builder) {
            this.builder = builder;
        }
    }

    /// Used to store one or more correct guesses that occurred during the same tick.
    private static class GuessCluster {
        int size;
        List<PlayerAlias> recordedOrder;
        List<List<PlayerAlias>> possiblePermutations;
        List<List<PlayerAlias>> validPermutations;
        List<PlayerAlias> currentOrder;

        public GuessCluster(List<PlayerAlias> players) {
            recordedOrder = players;
            currentOrder = recordedOrder;
            size = recordedOrder.size();
            if (size > 1) {
                possiblePermutations = Utils.generatePermutations(recordedOrder);
                validPermutations = possiblePermutations;
            }
        }

        public void removeCurrentPermutation() {
            validPermutations.remove(currentOrder);
            assert !validPermutations.isEmpty(); // TODO: this should throw something instead
            currentOrder = validPermutations.get(0);
        }

        @Override
        public String toString() {
            return currentOrder.toString();
        }
    }

    static class Game {
        List<Player> players;
        List<Round> rounds = new ArrayList<>();
        int currentRound = 0;
        int skippedRounds = 0;
        int potentialLeaverAmount = 0;

        // if the user leaves mid-game, we need to remember some stuff for when they join back
        GTBEvents.GameState leaveState;
        int leaveRound = -1;
        PlayerAlias leaveBuilder;

        public void onPlayerActivity(String player) {
            throw new NotImplementedException();
        }

        public void onRoundSkipped() {
            throw new NotImplementedException();
        }
    }
}
