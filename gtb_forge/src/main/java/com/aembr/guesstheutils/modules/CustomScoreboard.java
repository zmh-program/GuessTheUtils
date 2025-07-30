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
    // Fixed-width spinner characters to prevent visual flickering
    private static final String[] BUILDING_SPINNER = new String[] {"o", "O", "0", "@"};
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

        // Calculate scoreboard position (further right side of screen)
        int maxWidth = 140;
        int x = screenWidth - maxWidth - 5;
        int y = 20;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // Main info section with background
        int startY = y;
        int infoHeight = 0;
        
        // Calculate total height needed for info section
        infoHeight += fontRenderer.FONT_HEIGHT + 2; // State
        if (game.currentRound > 0) infoHeight += fontRenderer.FONT_HEIGHT + 2; // Round
        if (!game.currentTheme.isEmpty()) infoHeight += fontRenderer.FONT_HEIGHT + 2; // Theme
        if (!game.currentTimer.isEmpty()) infoHeight += fontRenderer.FONT_HEIGHT + 2; // Timer
        if (game.currentBuilder != null) infoHeight += fontRenderer.FONT_HEIGHT + 4; // Builder
        
        // Draw info background
        Gui.drawRect(x - 3, y - 2, x + maxWidth + 1, y + infoHeight + 2, 0x40000000);
        
        // Game state - simplified display
        String stateText = GameTracker.state.name().replace("_", " ");
        fontRenderer.drawStringWithShadow(stateText, x, y, 0xFF88CCFF);
        y += fontRenderer.FONT_HEIGHT + 2;

        // Round info with better formatting
        if (game.currentRound > 0) {
            String roundText = "Round " + game.currentRound + "/" + game.totalRounds;
            fontRenderer.drawStringWithShadow(roundText, x, y, 0xFFFFDD88);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Theme with prefix
        if (!game.currentTheme.isEmpty()) {
            fontRenderer.drawStringWithShadow("> " + game.currentTheme, x, y, 0xFF88FF88);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Timer with symbol
        if (!game.currentTimer.isEmpty()) {
            fontRenderer.drawStringWithShadow("T " + game.currentTimer, x, y, 0xFFFFFF88);
            y += fontRenderer.FONT_HEIGHT + 2;
        }

        // Current builder with spinner
        if (game.currentBuilder != null) {
            String spinnerFrame = getSpinnerFrame();
            fontRenderer.drawStringWithShadow(spinnerFrame + " " + game.currentBuilder.name, x, y, 0xFF88FFFF);
            y += fontRenderer.FONT_HEIGHT + 4;
        }

        // Players section
        List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
        
        // Players header with background
        int playersStartY = y;
        int playersHeight = (sortedPlayers.size() + 1) * (fontRenderer.FONT_HEIGHT + 1) + 4;
        Gui.drawRect(x - 3, y - 2, x + maxWidth + 1, y + playersHeight, 0x40000000);
        
        fontRenderer.drawStringWithShadow("Players (" + sortedPlayers.size() + ")", x, y, 0xFFFFFFFF);
        y += fontRenderer.FONT_HEIGHT + 2;

        for (GameTracker.Player player : sortedPlayers) {
            String playerLine = renderPlayerLine(player, game);
            int color = getPlayerColor(player, game);
            
            // Enhanced background for current user
            if (player.isUser) {
                Gui.drawRect(x - 1, y - 1, x + maxWidth - 2, y + fontRenderer.FONT_HEIGHT + 1, 0x60FFFF88);
            }
            
            // Highlight builder with subtle background
            if (player.equals(game.currentBuilder)) {
                Gui.drawRect(x - 1, y - 1, x + maxWidth - 2, y + fontRenderer.FONT_HEIGHT + 1, 0x4088FFFF);
            }
            
            fontRenderer.drawStringWithShadow(playerLine, x, y, color);
            y += fontRenderer.FONT_HEIGHT + 1;
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private String renderPlayerLine(GameTracker.Player player, GameTracker.Game game) {
        StringBuilder line = new StringBuilder();
        
        // Better status icons
        if (player.leaverState == GameTracker.Player.LeaverState.LEAVER) {
            line.append("X ");
        } else if (player.leaverState == GameTracker.Player.LeaverState.POTENTIAL_LEAVER) {
            line.append("? ");
        } else if (player.inactiveTicks > 3600) {
            line.append("o ");
        } else {
            line.append("* ");
        }
        
        // Player name
        line.append(player.name);
        
        // Points with better formatting
        int totalPoints = player.getTotalPoints();
        line.append(" (").append(totalPoints).append(")");
        
        // Current round points with + symbol
        if (game.currentRound > 0 && game.currentRound <= player.points.length) {
            int currentRoundPoints = player.points[game.currentRound - 1];
            if (currentRoundPoints > 0) {
                line.append(" +").append(currentRoundPoints);
            }
        }
        
        // Builder indicator - removed since we show it with background color and spinner
        
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