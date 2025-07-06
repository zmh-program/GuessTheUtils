package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CustomScoreboard implements HudElement {
    public enum PlayerBadge { BUILDER, INACTIVE, LEAVER }
    public final EnumMap<PlayerBadge, String> BADGES = new EnumMap<>(Map.of(
            PlayerBadge.BUILDER, "§b\uea03",
            PlayerBadge.INACTIVE, "§7\uea04",
            PlayerBadge.LEAVER, "§c\uea05"
    ));
    public final String[] POINTS_ICONS = new String[] {"\uea00", "\uea01", "\uea02"};

    public final int INACTIVE_PLAYER_THRESHOLD_SECONDS = 180;

    GameTracker tracker;
    Identifier identifier = Identifier.of("guess_the_utils_scoreboard");

    int lineSpacing = 1;
    int lineItemSpacing = 4;
    int defaultSeparatorHeight = 8;
    int linePadding = 2;
    int heightOffset = 30;

    public boolean isRendering() {
        return tracker != null && tracker.game != null;
    }

    public CustomScoreboard(GameTracker tracker) {
        this.tracker = tracker;
        HudElementRegistry.attachElementAfter(Identifier.ofVanilla("chat"), identifier, this);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (tracker == null || tracker.game == null) return;

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

        // Determine total width
        AtomicInteger longestNameLength = new AtomicInteger();
        tracker.game.players.forEach(p -> {
            String nameWithEmblem = p.emblem == null ? p.name : p.name + Formatting.strip(p.emblem.getString());
            int width = renderer.getWidth(nameWithEmblem) + lineItemSpacing;
            if (width > longestNameLength.get()) longestNameLength.set(width);
        });
        int width = renderer.getWidth("  \uea00 00") + longestNameLength.get();

        if (GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE)) {
            int timerLineWidth = renderer.getWidth("Next Round 00:00");
            if (timerLineWidth > width) width = timerLineWidth;
        }

        if (!tracker.game.currentTheme.isEmpty()) {
            int themeLineWidth = renderer.getWidth(tracker.game.currentTheme);
            if (themeLineWidth > width) width = themeLineWidth;
        }

        List<ScoreboardLine> lines = new ArrayList<>();
        lines.add(new TextLine(
                List.of("§fRound"),
                List.of(),
                List.of("§a" + tracker.game.currentRound + "§7/" + "§a" + tracker.game.totalRounds)));

        String timerState = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) ? "§fStarts In" :
                GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD) ? "§fTime Left" : "§fNext Round";

        lines.add(new TextLine(
                List.of(timerState),
                List.of(),
                List.of("§a" + tracker.game.currentTimer)));

        lines.add(new SeparatorLine(defaultSeparatorHeight));

        List<GameTracker.Player> sortedPlayers = List.copyOf(tracker.game.players).stream()
                .sorted(Comparator.comparingInt(GameTracker.Player::getTotalPoints).reversed()).toList();

        sortedPlayers.forEach(p -> {
            List<PlayerBadge> badges = new ArrayList<>();
            if (!p.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) badges.add(PlayerBadge.LEAVER);
            else if (p.inactiveTicks > INACTIVE_PLAYER_THRESHOLD_SECONDS * 20) badges.add(PlayerBadge.INACTIVE);
            if (Objects.equals(p, tracker.game.currentBuilder)) badges.add(PlayerBadge.BUILDER);

            boolean hasBuilt = p.buildRound > 0;

            MutableText name = Text.literal(p.name).formatted(p.rank);
            if (p.emblem != null) name.append(Text.literal(" ").append(p.emblem));

            int pointsThisRound = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) ?
                    0 : p.points[tracker.game.currentRound - 1];

            int points = p.getTotalPoints();
            lines.add(new PlayerLine(badges, hasBuilt, name, pointsThisRound, points));
        });

        lines.add(new SeparatorLine(defaultSeparatorHeight));

        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? "§fTheme:" : "§fTheme (§a" + tracker.game.currentTheme.length() + "§f):"),
                List.of()));

        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? "§c???" : "§a" + tracker.game.currentTheme),
                List.of()));

        // TODO: maybe if the theme length is short enough, single line would work
        AtomicInteger height = new AtomicInteger();

        lines.forEach(l -> height.addAndGet(
                (l instanceof SeparatorLine ? ((SeparatorLine) l).height() : renderer.fontHeight + linePadding + lineSpacing))
        );

        int x = context.getScaledWindowWidth() - (width - linePadding * 2);
        int y = context.getScaledWindowHeight() / 2 - (height.get() + linePadding * 2) / 2 - heightOffset;

        for (ScoreboardLine line : lines) {
            int lineHeight = renderer.fontHeight;
            int itemX = x + linePadding;

            if (line instanceof SeparatorLine) {
                lineHeight = ((SeparatorLine) line).height();

            } else if (line instanceof TextLine) {
                context.fill(x, y - lineSpacing, context.getScaledWindowWidth(), y + renderer.fontHeight - 1, 0x88000000);
                for (String item : ((TextLine) line).left()) {
                    context.drawText(renderer, item, itemX, y, 0xFFFFFFFF, false);
                    itemX += renderer.getWidth(item) + lineItemSpacing;
                }

                AtomicInteger centerItemsWidth = new AtomicInteger();
                ((TextLine) line).center().forEach(i -> centerItemsWidth.addAndGet(renderer.getWidth(i)));
                centerItemsWidth.addAndGet((((TextLine) line).center().size() - 1) * lineItemSpacing);

                itemX = x + width / 2 - centerItemsWidth.get() / 2;

                for (String item : ((TextLine) line).center()) {
                    context.drawText(renderer, item, itemX, y, 0xFFFFFFFF, false);
                    itemX += renderer.getWidth(item) + lineItemSpacing;
                }

                itemX = context.getScaledWindowWidth() - linePadding;

                for (String item : ((TextLine) line).right()) {
                    drawTextRightAligned(context, renderer, item, itemX, y, 0xFFFFFFFF, false);
                    itemX -= renderer.getWidth(item) + lineItemSpacing;
                }
            } else if (line instanceof PlayerLine) {
                context.fill(x, y - lineSpacing, context.getScaledWindowWidth(), y + renderer.fontHeight - 1, 0x88000000);
                itemX = x - 1;
                for (PlayerBadge badge : ((PlayerLine) line).badges()) {
                    drawTextRightAligned(context, renderer, BADGES.get(badge), itemX, y, 0xFFFFFFFF, false);
                    itemX -= renderer.getWidth(BADGES.get(badge)) + lineItemSpacing;
                }

                if (((PlayerLine) line).hasBuilt()) {
                    context.fill(x - 1, y - 1, x, y + renderer.fontHeight - 1, 0xFF55FFFF);
                }

                context.drawText(renderer, ((PlayerLine) line).name(), x + linePadding, y, 0xFFFFFFFF, false);

                itemX = context.getScaledWindowWidth() - linePadding;

                String pointsThisRound = "";
                String points = String.valueOf(((PlayerLine) line).points);
                int pointsBG = 0x66000000;

                if (!GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) && ((PlayerLine) line).pointsThisRound > 0) {
                    pointsThisRound = POINTS_ICONS[((PlayerLine) line).pointsThisRound - 1];
                    points = "§0" + points;
                    if (((PlayerLine) line).badges.contains(PlayerBadge.BUILDER)) {
                        pointsThisRound = "§b" + pointsThisRound;
                        pointsBG = 0xFF55FFFF;
                    } else {
                        pointsThisRound = "§a" + pointsThisRound;
                        pointsBG = 0xFF55FF55;
                    }

                } else {
                    points = "§2" + points;
                }

                context.fill(itemX - renderer.getWidth("00") - linePadding, y - lineSpacing, context.getScaledWindowWidth(), y + renderer.fontHeight - 1, pointsBG);
                drawTextRightAligned(context, renderer, points, itemX, y, 0xFFFFFFFF, false);

                itemX -= renderer.getWidth("00") + 2;
                drawTextRightAligned(context, renderer, pointsThisRound, itemX, y, 0xFFFFFFFF, false);

            }

            y += lineHeight + lineSpacing;
        }
    }

    private static void drawTextRightAligned(DrawContext context, TextRenderer renderer, String string, int x, int y, int color, boolean shadow) {
        context.drawText(renderer, string, x - renderer.getWidth(string), y, color, shadow);
    }

    public interface ScoreboardLine {}
    public record SeparatorLine(int height) implements ScoreboardLine {}
    public record TextLine(List<String> left, List<String> center, List<String> right) implements ScoreboardLine {}
    public record PlayerLine(List<PlayerBadge> badges, boolean hasBuilt, Text name, int pointsThisRound,
                             int points) implements ScoreboardLine {}
}
