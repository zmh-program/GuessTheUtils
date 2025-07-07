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
import java.util.stream.Stream;

public class CustomScoreboard implements HudElement {
    public enum PlayerBadge { BUILDER, INACTIVE, LEAVER }
    public static final EnumMap<PlayerBadge, String> BADGES = new EnumMap<>(Map.of(
            PlayerBadge.BUILDER, "§b\uea03",
            PlayerBadge.INACTIVE, "§7\uea04",
            PlayerBadge.LEAVER, "§c\uea05"
    ));
    public static final String[] POINTS_ICONS = new String[] {"\uea00", "\uea01", "\uea02"};

    public static final int INACTIVE_PLAYER_THRESHOLD_SECONDS = 180;

    GameTracker tracker;
    Identifier identifier = Identifier.of("guess_the_utils_scoreboard");

    public boolean isRendering() {
        return tracker != null && tracker.game != null;
    }

    public CustomScoreboard(GameTracker tracker) {
        this.tracker = tracker;
        try {
            HudElementRegistry.attachElementAfter(Identifier.ofVanilla("chat"), identifier, this);
        } catch (Exception e) {
            HudElementRegistry.replaceElement(identifier, hudElement -> this);
        }
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        int lineSpacing = 1;
        int lineItemSpacing = 4;
        int defaultSeparatorHeight = 6;
        int linePadding = 1;
        int heightOffset = 20;

        boolean includeTitles = false;
        boolean includeEmblems = true;

        if (tracker == null || tracker.game == null) return;

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

        // Technically, round starts when the theme is picked, but I think it's confusing
        int visualCurrentRound = tracker.game.currentRound;
        if (GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE)) visualCurrentRound++;

        List<ScoreboardLine> lines = new ArrayList<>();
        lines.add(new TextLine(
                List.of("§7Round"),
                List.of(),
                List.of("§a" + visualCurrentRound + "§7/" + "§a" + tracker.game.totalRounds)));

        String timerState = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) ? "§7Starts In" :
                GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD) ? "§7Time Left" : "§7Next Round";

        lines.add(new TextLine(
                List.of(timerState),
                List.of(),
                List.of("§a" + tracker.game.currentTimer.substring(1))));

        lines.add(new SeparatorLine(defaultSeparatorHeight));

        List<GameTracker.Player> sortedPlayers = List.copyOf(tracker.game.players).stream()
                .sorted(Comparator.comparingInt(GameTracker.Player::getTotalPoints).reversed()).toList();

        sortedPlayers.forEach(p -> lines.add(new PlayerLine(p)));

        lines.add(new SeparatorLine(defaultSeparatorHeight));

        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? "§7Theme:" : "§7Theme [§a" + tracker.game.currentTheme.length() + "§7]:"),
                List.of()));

        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? "§c???" : "§a" + tracker.game.currentTheme),
                List.of()));

        // TODO: maybe if the theme length is short enough, single line would work
        AtomicInteger height = new AtomicInteger();

        lines.forEach(l -> height.addAndGet(
                (l instanceof SeparatorLine ?
                        ((SeparatorLine) l).height() : renderer.fontHeight - 2 + linePadding * 2 + lineSpacing))
        );

        int width = getTotalWidth(renderer, lines, linePadding, includeTitles, includeEmblems, lineItemSpacing);

        int x = context.getScaledWindowWidth() - width;
        int y = context.getScaledWindowHeight() / 2 - height.get() / 2 - heightOffset;

        int playerPlace = 1;
        for (ScoreboardLine line : lines) {
            int lineHeight = drawLine(context, renderer, line, x, y, linePadding, includeTitles, includeEmblems,
                    lineItemSpacing, lineSpacing, 0x88000000, 0x1A55FFFF,
                    0xFF55FF55, 0xFF55FFFF, 0x66FFFFFF, width,
                    tracker.game.currentRound, playerPlace);
            if (line instanceof PlayerLine) playerPlace++;
            y += lineHeight;
        }
    }

    private static void drawTextRightAligned(DrawContext context, TextRenderer renderer, String string, int x, int y,
                                             int color, boolean shadow) {
        context.drawText(renderer, string, x - renderer.getWidth(string), y, color, shadow);
    }


    @SuppressWarnings({"unused", "SameParameterValue", "DuplicateExpressions"})
    private static int drawLine(DrawContext context, TextRenderer renderer, ScoreboardLine line, int x, int y,
                                int linePadding, boolean includeTitles, boolean includeEmblems, int lineItemSpacing,
                                int lineSpacing, int bgColor, int bgColorBuilder, int pointsBg,
                                int pointsBgBuilder, int inactiveFg, int width, int currentRound,
                                int playerPlace) {
        if (line instanceof SeparatorLine) return ((SeparatorLine) line).height;

        context.fill(x, y, x + width, y + renderer.fontHeight - 2 + linePadding * 2, bgColor);
        int itemX = x + linePadding;
        int itemY = y + linePadding;

        if (line instanceof PlayerLine) {
            GameTracker.Player player = ((PlayerLine) line).player;

            int fgColor = !player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL) ? inactiveFg : 0xFFFFFFFF;

            int bottom = y + renderer.fontHeight - 2 + linePadding * 2;

            int actualPointsBg = pointsBg;
            String points = "§a" + player.getTotalPoints();
            String pointsThisRound = "";

            if (player.buildRound == currentRound && player.buildRound != 0) {
                context.fill(x, y, x + width, bottom, bgColorBuilder);
                actualPointsBg = pointsBgBuilder;
                points = "§b" + points.substring(2);
            }

            if (GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) || player.points[currentRound - 1] == 0) {
                actualPointsBg = 0xFF000000;
                points = "§2" + points.substring(2);
            } else {
                pointsThisRound = POINTS_ICONS[player.points[currentRound - 1] - 1];
            }

            if (player.buildRound != 0) {
                drawTextRightAligned(context, renderer, BADGES.get(PlayerBadge.BUILDER), itemX - linePadding * 2, itemY, fgColor, true);
            }

            Formatting rankColor = player.rank == null ? Formatting.GRAY : player.rank;
            MutableText name = Text.literal(player.name).formatted(rankColor);
            if (includeEmblems && player.emblem != null && !player.emblem.getString().isEmpty()) {
                name.append(Text.literal(" ")).append(player.emblem);
            }
            if (includeTitles && player.title != null) {
                MutableText title = player.title.copy();
                name = title.append(Text.literal(" ")).append(name);
            }

            MutableText badge = Text.empty();
            if (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) {
                badge = (MutableText) Text.of(BADGES.get(PlayerBadge.LEAVER));
            } else if (player.inactiveTicks > INACTIVE_PLAYER_THRESHOLD_SECONDS * 20) {
                badge = (MutableText) Text.of(BADGES.get(PlayerBadge.INACTIVE));
            }

            if (!badge.getString().isEmpty()) {
                badge.append(Text.literal(" "));
                context.drawText(renderer, badge, itemX, itemY, 0xFFFFFFFF, true);
                itemX += renderer.getWidth(badge);
            }

            context.drawText(renderer, name, itemX, itemY, fgColor, true);

            int pointsWidth = (player.getTotalPoints() < 10 ? renderer.getWidth("0") : renderer.getWidth("00")) + 1;
            itemX = x + width - linePadding;
            drawTextRightAligned(context, renderer, points, itemX, itemY, fgColor, true);
            itemX -= pointsWidth;
            drawTextRightAligned(context, renderer, pointsThisRound, itemX, itemY, actualPointsBg, true);
        }

        if (line instanceof TextLine) {
            for (String item : ((TextLine) line).left()) {
                context.drawText(renderer, item, itemX, itemY, 0xFFFFFFFF, true);
                itemX += renderer.getWidth(item) + lineItemSpacing;
            }

            AtomicInteger centerItemsWidth = new AtomicInteger();
            ((TextLine) line).center().forEach(i -> centerItemsWidth.addAndGet(renderer.getWidth(i)));
            centerItemsWidth.addAndGet((((TextLine) line).center().size() - 1) * lineItemSpacing);

            itemX = x + width / 2 - centerItemsWidth.get() / 2;

            for (String item : ((TextLine) line).center()) {
                context.drawText(renderer, item, itemX, itemY, 0xFFFFFFFF, true);
                itemX += renderer.getWidth(item) + lineItemSpacing;
            }

            itemX = x + width - linePadding;

            for (String item : ((TextLine) line).right()) {
                drawTextRightAligned(context, renderer, item, itemX, itemY, 0xFFFFFFFF, true);
                itemX -= renderer.getWidth(item) + lineItemSpacing;
            }
        }
        return renderer.fontHeight - 2 + linePadding * 2 + lineSpacing;
    }

    private static int getTotalWidth(TextRenderer renderer, List<ScoreboardLine> lines, int linePadding,
                                     boolean includeTitles, boolean includeEmblems, int lineItemSpacing) {
        int width = 0;
        for (ScoreboardLine line : lines) {
            if (line instanceof SeparatorLine) continue;
            if (line instanceof PlayerLine) {
                GameTracker.Player player = ((PlayerLine) line).player;

                String name = player.name;
                if (includeEmblems && player.emblem != null && !player.emblem.getString().isEmpty()) {
                    name += " " + player.emblem.getString();
                }
                if (includeTitles && player.title != null) {
                    name = player.title.getString() + " " + name;
                }
                if (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) name += BADGES.get(PlayerBadge.LEAVER);

                int totalWidth = linePadding * 2 + lineItemSpacing + renderer.getWidth(name) +
                        renderer.getWidth("\uea00 " + player.getTotalPoints());

                if (totalWidth > width) width = totalWidth;
            }
            if (line instanceof TextLine) {
                AtomicInteger lineWidth = new AtomicInteger();
                AtomicInteger items = new AtomicInteger();
                Stream.of(((TextLine) line).left, ((TextLine) line).center, ((TextLine) line).right)
                        .flatMap(List::stream).forEach(i -> {
                            lineWidth.addAndGet(renderer.getWidth(i));
                            items.addAndGet(1);
                        });
                lineWidth.addAndGet(Math.max(0, items.get() - 1) * lineItemSpacing + linePadding * 2);
                if (lineWidth.get() > width) width = lineWidth.get();
            }
        }
        return width;
    }

    public interface ScoreboardLine {}
    public record SeparatorLine(int height) implements ScoreboardLine {}
    public record TextLine(List<String> left, List<String> center, List<String> right) implements ScoreboardLine {}
    public record PlayerLine(GameTracker.Player player) implements ScoreboardLine {}
}
