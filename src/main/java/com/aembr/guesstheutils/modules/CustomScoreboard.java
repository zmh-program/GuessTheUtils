package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.aembr.guesstheutils.GuessTheUtils.CLIENT;

public class CustomScoreboard implements HudElement {
    public static final String[] BUILDING_SPINNER = new String[] {"\uea00", "\uea01", "\uea02", "\uea03", "\uea04",
            "\uea05", "\uea06", "\uea07", "\uea08", "\uea09", "\uea10", "\uea11", "\uea12", "\uea13", "\uea14", "\uea15"};
    public static final String[] POINTS_ICONS = new String[] {"\uea16", "\uea17", "\uea18"};
    public static final String INACTIVE_ICON = "\uea19";
    public static final String LEAVER_ICON = "\uea20";
    public static final String BUILD_BG_ICON = "\uea21";
    public static final String BUILD_CHECK_ICON = "\uea22";

    public static final int INACTIVE_PLAYER_THRESHOLD_SECONDS = 180;
    public static int tickCounter = 0;

    public static boolean shadow = true;
    public static Formatting backgroundColor = Formatting.BLACK;
    public static float backgroundOpacity = 0.5f;
    public static Formatting textColor = Formatting.GRAY;
    public static float foregroundOpacity = 1.0f;
    public static float foregroundOpacityInactive = 0.3f;

    public static Formatting accentColor = Formatting.GREEN;
    public static Formatting accentColorBuilder = Formatting.AQUA;
    public static float backgroundHighlightOpacity = 0.13f;

    public static Formatting notBuiltIconColor = Formatting.DARK_GRAY;
    public static float notBuiltIconOpacity = 0.5f;
    public static Formatting inactiveIconColor = Formatting.DARK_GRAY;
    public static Formatting leaverIconColor = Formatting.RED;

    public static Formatting pointsThisRoundColor1 = Formatting.DARK_GREEN;
    public static Formatting pointsThisRoundColor2 = Formatting.GREEN;
    public static Formatting pointsThisRoundColor3 = Formatting.YELLOW;
    public static float pointsThisRoundOpacity = 0.7f;

    public static Formatting pointsColor = Formatting.DARK_GREEN;
    public static Formatting pointsColorHighlight = Formatting.GREEN;

    public static Formatting unknownThemeColor = Formatting.RED;
    public static String unknownThemeString = "???";

    public static int lineSpacing = 1;
    public static int lineItemSpacing = 3;
    public static int defaultSeparatorHeight = 8;
    public static boolean drawSeparatorBg = false;
    public static int linePadding = 1;
    public static int heightOffset = 20;
    public static int playerNameRightPad = 4;

    GameTracker tracker;
    Identifier identifier = Identifier.of("guess_the_utils_scoreboard");

    public boolean isRendering() {
        return tracker != null && tracker.game != null && GuessTheUtils.events.isInGtb();
    }

    public CustomScoreboard(GameTracker tracker) {
        this.tracker = tracker;
        try {
            HudElementRegistry.attachElementAfter(Identifier.ofVanilla("chat"), identifier, this);
        } catch (Exception e) {
            HudElementRegistry.replaceElement(identifier, hudElement -> this);
        }
    }

    public static String getSpinnerFrame(String[] frames, int ticksPerFrame, int tickCounter) {
        int index = (tickCounter / ticksPerFrame) % frames.length;
        return frames[frames.length - 1 - index]; // uhh it's reversed lol
    }

    @SuppressWarnings({"DataFlowIssue"})
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        boolean extended = CLIENT.options.playerListKey.isPressed();

        boolean includePlaces = extended;
        boolean includeTitles = extended;
        boolean includeEmblems = true;

        if (tracker == null || tracker.game == null) return;

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

        // Technically, round starts when the theme is picked, but I think it's confusing
        int visualCurrentRound = tracker.game.currentRound;
        if (GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE)) visualCurrentRound++;

        List<ScoreboardLine> lines = new ArrayList<>();

        // Round line
        lines.add(new TextLine(
                List.of(Text.literal("Round").formatted(textColor)),
                List.of(),
                List.of(Text.literal(String.valueOf(visualCurrentRound)).formatted(accentColor)
                        .append(Text.literal("/").formatted(textColor))
                        .append(Text.literal(String.valueOf(tracker.game.totalRounds)).formatted(accentColor)))));

        // Timer line
        String timerState = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) ? "Starts In" :
                GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD) ? "Time Left" : "Next Round";

        lines.add(new TextLine(
                List.of(Text.literal(timerState).formatted(textColor)),
                List.of(),
                List.of(Text.literal(tracker.game.currentTimer.substring(1)).formatted(accentColor))));

        // Separator
        lines.add(new SeparatorLine(defaultSeparatorHeight));

        // Player Lines
        List<GameTracker.Player> sortedPlayers = List.copyOf(tracker.game.players).stream()
                .sorted(Comparator.comparingInt(GameTracker.Player::getTotalPoints).reversed()).toList();

        sortedPlayers.forEach(p -> lines.add(new PlayerLine(p)));

        // Separator
        lines.add(new SeparatorLine(defaultSeparatorHeight));

        // Theme title
        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? Text.literal("Theme:").formatted(textColor)
                        : Text.literal("Theme [").formatted(textColor)
                        .append(Text.literal(String.valueOf(tracker.game.currentTheme.length()))
                                .formatted(accentColor).append(Text.literal("]:").formatted(textColor)))),
                List.of()));

        // Theme line
        lines.add(new TextLine(
                List.of(),
                List.of(tracker.game.currentTheme.isEmpty() ? Text.literal(unknownThemeString)
                        .formatted(unknownThemeColor) : Text.literal(tracker.game.currentTheme).formatted(accentColor)),
                List.of()));

        // TODO: maybe if the theme length is short enough, single line would work
        AtomicInteger height = new AtomicInteger();

        lines.forEach(l -> height.addAndGet(
                (l instanceof SeparatorLine ?
                        ((SeparatorLine) l).height() : renderer.fontHeight - 2 + linePadding * 2 + lineSpacing))
        );

        int width = getTotalWidth(renderer, lines, linePadding, includeTitles, includeEmblems, lineItemSpacing,
                playerNameRightPad, includePlaces, tracker.game);

        int x = context.getScaledWindowWidth() - width;
        int y = context.getScaledWindowHeight() / 2 - height.get() / 2 - heightOffset;

        int bgColor = rgbToArgb(TextColor.fromFormatting(backgroundColor).getRgb(), backgroundOpacity);
        int fgColor = rgbToArgb(0xFFFFFF, foregroundOpacity);
        int fgColorInactive = rgbToArgb(0xFFFFFF, foregroundOpacityInactive);
        int fgColorPointsThisRound = rgbToArgb(0xFFFFFF, pointsThisRoundOpacity);
        int backgroundHighlightColor = rgbToArgb(TextColor.fromFormatting(accentColor).getRgb(), backgroundHighlightOpacity);
        int backgroundHighlightColorBuilder = rgbToArgb(TextColor.fromFormatting(accentColorBuilder).getRgb(), backgroundHighlightOpacity);

        int playerPlace = 1;
        for (ScoreboardLine line : lines) {
            int lineHeight = drawLine(context, renderer, line, x, y, width, linePadding, includeTitles, includeEmblems,
                    lineItemSpacing, lineSpacing, bgColor, fgColor, textColor, fgColorInactive, fgColorPointsThisRound,
                    accentColor, accentColorBuilder, backgroundHighlightColor, backgroundHighlightColorBuilder,
                    notBuiltIconColor, notBuiltIconOpacity, inactiveIconColor, leaverIconColor, pointsThisRoundColor1, pointsThisRoundColor2,
                    pointsThisRoundColor3, pointsColor, pointsColorHighlight, tracker.game, playerPlace, shadow, drawSeparatorBg, includePlaces);
            if (line instanceof PlayerLine) playerPlace++;
            y += lineHeight;
        }
    }

    private static void drawTextRightAligned(DrawContext context, TextRenderer renderer, String string, int x, int y,
                                             int color, boolean shadow) {
        context.drawText(renderer, string, x - renderer.getWidth(string), y, color, shadow);
    }

    private static void drawTextRightAligned(DrawContext context, TextRenderer renderer, Text text, int x, int y,
                                             int color, boolean shadow) {
        context.drawText(renderer, text, x - renderer.getWidth(text), y, color, shadow);
    }

    @SuppressWarnings({"unused", "SameParameterValue", "DuplicateExpressions"})
    private static int drawLine(DrawContext context, TextRenderer renderer, ScoreboardLine line, int x, int y,
                                int width, int linePadding, boolean includeTitles, boolean includeEmblems,
                                int lineItemSpacing, int lineSpacing, int backgroundColor, int textColor,
                                Formatting textColorFormatting, int textColorInactive, int textColorPointsThisRound,
                                Formatting accentColor, Formatting accentColorBuilder, int backgroundHighlightColor,
                                int backgroundHighlightColorBuilder, Formatting notBuiltIconColor,
                                float notBuildIconOpacity, Formatting inactiveIconColor, Formatting leaverIconColor,
                                Formatting pointsThisRoundColor1, Formatting pointsThisRoundColor2,
                                Formatting pointsThisRoundColor3, Formatting pointsColor,
                                Formatting pointsColorHighlight, GameTracker.Game game, int playerPlace, boolean shadow,
                                boolean drawSeparatorBg, boolean includePlaces) {

        if (line instanceof SeparatorLine) {
            if (drawSeparatorBg) {
                context.fill(x, y, x + width, y + ((SeparatorLine) line).height - 2 + linePadding * 2, backgroundColor);
            }
            return ((SeparatorLine) line).height;
        }

        context.fill(x, y, x + width, y + renderer.fontHeight - 2 + linePadding * 2, backgroundColor);
        int itemX = x + linePadding;
        int itemY = y + linePadding;

        if (line instanceof PlayerLine) {
            GameTracker.Player player = ((PlayerLine) line).player;

            boolean isBuilder = Objects.equals(game.currentBuilder, player);
            boolean isBuildingThisRound = player.buildRound == game.currentRound;
            boolean isRoundPre = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE);
            int pointsThisRound = isRoundPre ? 0 : player.points[game.currentRound - 1];

            int fgColor = !player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL) ? textColorInactive : textColor;
            int bottom = y + renderer.fontHeight - 2 + linePadding * 2;

            // Highlight
            int highlightColor = isBuildingThisRound ? backgroundHighlightColorBuilder : backgroundHighlightColor;
            if ((pointsThisRound > 0 || isBuildingThisRound) && !isRoundPre) {
                context.fill(x, y, x + width, bottom, highlightColor);
            }

            // Build Icon BG
            Text builderIconBg = Text.literal(BUILD_BG_ICON).formatted(notBuiltIconColor);
            drawTextRightAligned(context, renderer, builderIconBg, itemX - linePadding - lineItemSpacing, itemY, rgbToArgb(textColor, notBuildIconOpacity), shadow);

            // Build Icon Check or Spinner
            if (player.buildRound != 0) {
                Text builderIconFg = Text.literal(BUILD_CHECK_ICON).formatted(accentColorBuilder);
                if (isBuildingThisRound && GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD)) {
                    builderIconFg = Text.literal(getSpinnerFrame(BUILDING_SPINNER, 1, tickCounter))
                            .formatted(accentColorBuilder);
                }
                context.drawText(renderer, builderIconFg, itemX - linePadding - lineItemSpacing - renderer.getWidth(builderIconBg), itemY, textColor, shadow);
            }

            // Places
            if (includePlaces) {
                int placeWidth = game.players.size() == 10 ? renderer.getWidth("00") : renderer.getWidth("0");
                Text place = Text.literal(String.valueOf(playerPlace)).formatted(textColorFormatting);
                drawTextRightAligned(context, renderer, place, itemX + placeWidth, itemY, fgColor, shadow);
                itemX += placeWidth + lineItemSpacing;
            }

            // Leaver Badge
            Text badge = Text.empty();
            if (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) {
                badge = Text.literal(LEAVER_ICON).formatted(leaverIconColor);
            } else if (player.inactiveTicks > INACTIVE_PLAYER_THRESHOLD_SECONDS * 20) {
                badge = Text.literal(INACTIVE_ICON).formatted(inactiveIconColor);
            }
            if (!badge.getString().isEmpty()) {
                context.drawText(renderer, badge, itemX, itemY, textColor, shadow);
                itemX += renderer.getWidth(badge) + lineItemSpacing;
            }

            // Name
            Formatting rankColor = player.rank == null ? Formatting.GRAY : player.rank;
            MutableText name = Text.literal(player.name).formatted(rankColor);

            if (includeEmblems && player.emblem != null && !player.emblem.getString().isEmpty()) {
                name.append(Text.literal(" ")).append(player.emblem);
            }
            if (includeTitles && player.title != null) {
                MutableText title = player.title.copy();
                name = title.append(Text.literal(" ")).append(name);
            }

            context.drawText(renderer, name, itemX, itemY, fgColor, shadow);

            // Points
            itemX = x + width - linePadding;
            MutableText points = Text.literal(String.valueOf(player.getTotalPoints()));
            if (!isRoundPre && pointsThisRound > 0) {
                if (isBuildingThisRound) points.formatted(accentColorBuilder);
                else points.formatted(pointsColorHighlight);
            } else points.formatted(pointsColor);

            int pointsWidth = (player.getTotalPoints() < 10 ? renderer.getWidth("0") : renderer.getWidth("00")) + 1;

            drawTextRightAligned(context, renderer, points, itemX, itemY, fgColor, shadow);

            // Points this round
            if (!isRoundPre && pointsThisRound > 0) {
                itemX -= pointsWidth + lineItemSpacing / 2;
                MutableText pointsThisRoundIcon = Text.literal(POINTS_ICONS[pointsThisRound - 1]);
                Formatting pointsThisRoundColor;
                if (isBuildingThisRound) pointsThisRoundIcon.formatted(accentColorBuilder);
                else {
                    switch (pointsThisRound) {
                        case 3: pointsThisRoundIcon.formatted(pointsThisRoundColor3);
                        case 2: pointsThisRoundIcon.formatted(pointsThisRoundColor2);
                        case 1: pointsThisRoundIcon.formatted(pointsThisRoundColor1);
                    }
                }
                drawTextRightAligned(context, renderer, pointsThisRoundIcon, itemX, itemY, textColorPointsThisRound, shadow);
            }

        }

        if (line instanceof TextLine) {
            for (Text item : ((TextLine) line).left()) {
                context.drawText(renderer, item, itemX, itemY, textColor, shadow);
                itemX += renderer.getWidth(item) + lineItemSpacing;
            }

            AtomicInteger centerItemsWidth = new AtomicInteger();
            ((TextLine) line).center().forEach(i -> centerItemsWidth.addAndGet(renderer.getWidth(i)));
            centerItemsWidth.addAndGet((((TextLine) line).center().size() - 1) * lineItemSpacing);

            itemX = x + width / 2 - centerItemsWidth.get() / 2;

            for (Text item : ((TextLine) line).center()) {
                context.drawText(renderer, item, itemX, itemY, textColor, shadow);
                itemX += renderer.getWidth(item) + lineItemSpacing;
            }

            itemX = x + width - linePadding;

            for (Text item : ((TextLine) line).right()) {
                drawTextRightAligned(context, renderer, item, itemX, itemY, textColor, shadow);
                itemX -= renderer.getWidth(item) + lineItemSpacing;
            }
        }
        return renderer.fontHeight - 2 + linePadding * 2 + lineSpacing;
    }

    private static int getTotalWidth(TextRenderer renderer, List<ScoreboardLine> lines, int linePadding,
                                     boolean includeTitles, boolean includeEmblems, int lineItemSpacing,
                                     int playerNameRightPad, boolean includePlaces, GameTracker.Game game) {
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
                if (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) name += LEAVER_ICON;

                int placeWidth = !includePlaces ? 0 : game.players.size() == 10 ? renderer.getWidth("00")
                        : renderer.getWidth("0") + lineItemSpacing;

                int totalWidth = linePadding * 2 + lineItemSpacing + renderer.getWidth(name) + placeWidth +
                        renderer.getWidth("\uea00 " + player.getTotalPoints()) + playerNameRightPad;

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

    public static int rgbToArgb(int rgb, float alpha) {
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        int alphaInt = (int) (alpha * 255);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (alphaInt << 24) | (red << 16) | (green << 8) | blue;
    }

    public interface ScoreboardLine {}
    public record SeparatorLine(int height) implements ScoreboardLine {}
    public record TextLine(List<Text> left, List<Text> center, List<Text> right) implements ScoreboardLine {}
    public record PlayerLine(GameTracker.Player player) implements ScoreboardLine {}
}
