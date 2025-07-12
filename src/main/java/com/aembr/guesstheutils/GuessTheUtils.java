package com.aembr.guesstheutils;

import com.aembr.guesstheutils.modules.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GuessTheUtils implements ClientModInitializer {
    public static final String MOD_ID = "guesstheutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static final MutableText prefix = Text.literal("[").formatted(Formatting.WHITE)
            .append(Text.literal("GuessTheUtils").formatted(Formatting.GOLD))
            .append(Text.literal("] ").formatted(Formatting.WHITE));

    public static final Replay replay = new Replay();
    public static GTBEvents events = new GTBEvents();

    public static GameTracker gameTracker = new GameTracker(events);
    public static NameAutocomplete nameAutocomplete = new NameAutocomplete(events);
    public static ShortcutReminder shortcutReminder = new ShortcutReminder(events);
    @SuppressWarnings("unused")
    public static BuilderNotification builderNotification = new BuilderNotification(events);

    public static boolean testing = false;
    public static LiveE2ERunner liveE2ERunner;

    private static Tick currentTick;
    private List<Text> previousScoreboardLines = new ArrayList<>();
    private List<Text> previousPlayerListEntries = new ArrayList<>();
    private Text previousActionBarMessage = Text.empty();
    private Text previousScreenTitle = Text.empty();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(
                (commandDispatcher,
                 commandRegistryAccess) -> Commands.register(commandDispatcher));

        ClientTickEvents.START_CLIENT_TICK.register(this::onStartTick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) onActionBarMessage(message);
            else onChatMessage(message);
        });
//        HudLayerRegistrationCallback.EVENT.register((drawContext, v) -> {
//            gameTracker.drawScoreboard(drawContext);
//        });

        replay.initialize();
        shortcutReminder.init();
        liveE2ERunner = new LiveE2ERunner(Replay.load(GuessTheUtils.class.getResourceAsStream("/assets/live_tests/yur25_space_between_name_and_emblem_too_big.json")));
    }

    private void onStartTick(MinecraftClient client) {
        if (client.player == null || events == null) return;
        if (currentTick == null) currentTick = new Tick();

        if (testing) {
            currentTick = liveE2ERunner.getNext();
        }

        if (!currentTick.isEmpty()) {
            replay.addTick(currentTick);
            try {
                Tick tempTick = currentTick;
                // Reset currentTick immediately. ConcurrentModificationException can be thrown if
                // a new message arrives while we're processing, modifying the chatMessage list
                currentTick = new Tick();

                events.processTickUpdate(tempTick);
            } catch (Exception e) {
                String stackTrace = Utils.getStackTraceAsString(e);

                events = null;
                Tick error = new Tick();
                error.error = stackTrace;
                replay.addTick(error);
                Utils.sendMessage("Exception in GTBEvents: " + e.getMessage() + ". Saving details to replay file...");
                Utils.sendMessage("Game restart required.");
                replay.save();
            } finally {
                currentTick = new Tick();
            }
        }

        if (testing) return;

        onScoreboardUpdate(Utils.getScoreboardLines(client));
        onPlayerListUpdate(Utils.collectTabListEntries(client));
        Text screenTitle = CLIENT.currentScreen == null ? Text.empty() : CLIENT.currentScreen.getTitle();
        onScreenUpdate(screenTitle);
    }

    private void onScreenUpdate(Text screenTitle) {
        if (currentTick == null) return;
        if (previousScreenTitle.equals(screenTitle)) return;
        previousScreenTitle = screenTitle;
        currentTick.screenTitle = screenTitle;
    }

    private void onScoreboardUpdate(List<Text> scoreboardLines) {
        if (currentTick == null) return;
        if (previousScoreboardLines.equals(scoreboardLines)) return;
        previousScoreboardLines = scoreboardLines;
        currentTick.scoreboardLines = scoreboardLines;
    }

    private void onPlayerListUpdate(List<Text> playerListEntries) {
        if (currentTick == null) return;
        if (previousPlayerListEntries.equals(playerListEntries)) return;
        previousPlayerListEntries = playerListEntries;
        currentTick.playerListEntries = playerListEntries;
    }

    private void onChatMessage(Text message) {
        // We don't want guild, party, or direct messages to be processed, or end up in replays
        String stripped = Formatting.strip(message.getString());
        if (stripped == null
                || stripped.startsWith("Guild > ")
                || stripped.startsWith("Party > ")
                || stripped.startsWith("From ")) return;

        if (currentTick == null) return;
        if (currentTick.chatMessages == null) currentTick.chatMessages = new ArrayList<>();
        currentTick.chatMessages.add(message);
    }

    private void onActionBarMessage(Text message) {
        if (currentTick == null) return;
        if (previousActionBarMessage.equals(message)) return;
        previousActionBarMessage = message;
        currentTick.actionBarMessage = message;
    }

    public static void onTitleSet(Text title) {
        if (currentTick == null) return;
        currentTick.title = title;
    }

    public static void onSubtitleSet(Text subtitle) {
        if (currentTick == null) return;
        currentTick.subtitle = subtitle;
    }
}