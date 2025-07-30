package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CustomScoreboard {
    private static final String[] BUILDING_SPINNER = new String[] {"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};
    public static int tickCounter = 0;
    
    private GameTracker gameTracker;
    private Minecraft mc;

    public CustomScoreboard(GameTracker gameTracker) {
        this.gameTracker = gameTracker;
        this.mc = Minecraft.getMinecraft();
    }

    public static boolean isRendering() {
        return GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.game != null 
                && GuessTheUtils.events.isInGtb() && GuessTheUtilsConfig.enableCustomScoreboard;
    }

    public void render() {
        if (!isRendering() || mc.currentScreen != null) return;

        GameTracker.Game game = gameTracker.game;
        if (game == null) return;

        tickCounter++;
        
        ScaledResolution scaledRes = new ScaledResolution(mc);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        FontRenderer fontRenderer = mc.fontRendererObj;

        // Calculate scoreboard position (right side of screen)
        int maxWidth = 150;
        int x = screenWidth - maxWidth - 10;
        int y = 30;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // Title
        String title = "Guess the Build";
        int titleWidth = fontRenderer.getStringWidth(title);
        int titleX = x + (maxWidth - titleWidth) / 2;
        
        // Title background
        Gui.drawRect(titleX - 2, y - 2, titleX + titleWidth + 2, y + fontRenderer.FONT_HEIGHT + 2, 0x80000000);
        fontRenderer.drawStringWithShadow(title, titleX, y, 0xFFFFFFFF);
        y += fontRenderer.FONT_HEIGHT + 6;

        // Game state
        String stateText = "State: " + GameTracker.state.name();
        fontRenderer.drawStringWithShadow(stateText, x, y, 0xFFAAAAAA);
        y += fontRenderer.FONT_HEIGHT + 2;

        // Round info
        if (game.currentRound > 0) {
            String roundText = "Round: " + game.currentRound + "/" + game.totalRounds;
            fontRenderer.drawStringWithShadow(roundText, x, y, 0xFFAAAAAA);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Theme
        if (!game.currentTheme.isEmpty()) {
            String themeText = "Theme: " + game.currentTheme;
            fontRenderer.drawStringWithShadow(themeText, x, y, 0xFF55FF55);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Timer
        if (!game.currentTimer.isEmpty()) {
            String timerText = "Timer: " + game.currentTimer;
            fontRenderer.drawStringWithShadow(timerText, x, y, 0xFFFFFF55);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Current builder
        if (game.currentBuilder != null) {
            String builderText = "Builder: " + game.currentBuilder.name;
            String spinnerFrame = getSpinnerFrame();
            fontRenderer.drawStringWithShadow(spinnerFrame + " " + builderText, x, y, 0xFF55FFFF);
            y += fontRenderer.FONT_HEIGHT + 4;
        }

        // Players list
        List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
        
        fontRenderer.drawStringWithShadow("Players:", x, y, 0xFFFFFFFF);
        y += fontRenderer.FONT_HEIGHT + 2;

        for (GameTracker.Player player : sortedPlayers) {
            String playerLine = renderPlayerLine(player, game);
            int color = getPlayerColor(player, game);
            
            // Background for current user
            if (player.isUser) {
                int lineWidth = fontRenderer.getStringWidth(playerLine);
                Gui.drawRect(x - 2, y - 1, x + lineWidth + 2, y + fontRenderer.FONT_HEIGHT + 1, 0x40555555);
            }
            
            fontRenderer.drawStringWithShadow(playerLine, x, y, color);
            y += fontRenderer.FONT_HEIGHT + 1;
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private String renderPlayerLine(GameTracker.Player player, GameTracker.Game game) {
        StringBuilder line = new StringBuilder();
        
        // Status icon
        if (player.leaverState == GameTracker.Player.LeaverState.LEAVER) {
            line.append("✗ ");
        } else if (player.leaverState == GameTracker.Player.LeaverState.POTENTIAL_LEAVER) {
            line.append("? ");
        } else if (player.inactiveTicks > 3600) { // 3 minutes inactive
            line.append("○ ");
        } else {
            line.append("● ");
        }
        
        // Player name
        line.append(player.name);
        
        // Points
        int totalPoints = player.getTotalPoints();
        line.append(" (").append(totalPoints).append(")");
        
        // Current round points
        if (game.currentRound > 0 && game.currentRound <= player.points.length) {
            int currentRoundPoints = player.points[game.currentRound - 1];
            if (currentRoundPoints > 0) {
                line.append(" +").append(currentRoundPoints);
            }
        }
        
        // Builder indicator
        if (player.equals(game.currentBuilder)) {
            line.append(" ⚒");
        }
        
        return line.toString();
    }

    private int getPlayerColor(GameTracker.Player player, GameTracker.Game game) {
        if (player.leaverState == GameTracker.Player.LeaverState.LEAVER) {
            return 0xFF666666; // Gray for leavers
        }
        
        if (player.equals(game.currentBuilder)) {
            return 0xFF55FFFF; // Cyan for builder
        }
        
        if (player.isUser) {
            return 0xFFFFFF55; // Yellow for user
        }
        
        // Rank colors
        if (player.rank != null) {
            switch (player.rank) {
                case RED: return 0xFFFF5555;
                case GOLD: return 0xFFFFAA00;
                case YELLOW: return 0xFFFFFF55;
                case GREEN: return 0xFF55FF55;
                case AQUA: return 0xFF55FFFF;
                case BLUE: return 0xFF5555FF;
                case LIGHT_PURPLE: return 0xFFFF55FF;
                case DARK_PURPLE: return 0xFFAA00AA;
                default: return 0xFFFFFFFF;
            }
        }
        
        return 0xFFFFFFFF; // White default
    }

    private List<GameTracker.Player> getSortedPlayers(GameTracker.Game game) {
        List<GameTracker.Player> players = new ArrayList<GameTracker.Player>(game.players);
        
        // Sort by total points (descending), then by name
        Collections.sort(players, new Comparator<GameTracker.Player>() {
            @Override
            public int compare(GameTracker.Player p1, GameTracker.Player p2) {
                int pointsCompare = Integer.compare(p2.getTotalPoints(), p1.getTotalPoints());
                if (pointsCompare != 0) return pointsCompare;
                return p1.name.compareTo(p2.name);
            }
        });
        
        return players;
    }

    private String getSpinnerFrame() {
        int index = (tickCounter / 4) % BUILDING_SPINNER.length;
        return BUILDING_SPINNER[index];
    }
}