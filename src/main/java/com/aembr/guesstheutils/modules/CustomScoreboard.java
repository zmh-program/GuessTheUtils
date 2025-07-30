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
    private static final String[] BUILDING_SPINNER = new String[] {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    public static int tickCounter = 0;
    
    private GameTracker gameTracker;
    private Minecraft mc;

    public CustomScoreboard(GameTracker gameTracker) {
        this.gameTracker = gameTracker;
        this.mc = Minecraft.getMinecraft();
    }

    public static boolean isRendering() {
        // Check if custom scoreboard should be active
        if (!GuessTheUtilsConfig.enableCustomScoreboard) {
            return false;
        }
        
        // We need gameTracker to exist
        if (GuessTheUtils.gameTracker == null) {
            return false;
        }
        
        // Check if we're in GTB
        boolean inGtb = GuessTheUtils.events.isInGtb();
        if (!inGtb) {
            return false;
        }
        
        // If we have a game object, definitely render
        if (GuessTheUtils.gameTracker.game != null) {
            return true;
        }
        
        // Even if we don't have a game object yet, if we're in GTB, 
        // we should still try to render to hide vanilla scoreboard
        // This prevents vanilla scoreboard from showing during game start delay
        return true;
    }

    public void render() {
        if (!isRendering() || mc.currentScreen != null) return;

        GameTracker.Game game = gameTracker.game;
        
        // If no game object yet, show a simple "Waiting for game..." message
        if (game == null) {
            renderWaitingMessage();
            return;
        }

        tickCounter++;
        
        ScaledResolution scaledRes = new ScaledResolution(mc);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        FontRenderer fontRenderer = mc.fontRendererObj;

        // Calculate scoreboard position
        int maxWidth = 110;
        int x = screenWidth - maxWidth - 8;
        int y = 20;
        int padding = 3;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // Main info section with background
        int startY = y;
        int infoHeight = 0;
        
        // Calculate total height needed for info section with proper spacing
        infoHeight += fontRenderer.FONT_HEIGHT + 3; // State
        if (game.currentRound > 0) infoHeight += fontRenderer.FONT_HEIGHT + 3; // Round
        if (!game.currentTheme.isEmpty()) infoHeight += fontRenderer.FONT_HEIGHT + 3; // Theme
        if (!game.currentTimer.isEmpty()) infoHeight += fontRenderer.FONT_HEIGHT + 3; // Timer
        if (game.currentBuilder != null) infoHeight += fontRenderer.FONT_HEIGHT + 5; // Builder
        
        // Draw info background with vanilla alpha
        Gui.drawRect(x - padding, y - 2, x + maxWidth, y + infoHeight + 1, 0x50000000);
        
        // Game state - simplified display with better formatting
        String stateText = formatStateName(GameTracker.state.name());
        fontRenderer.drawStringWithShadow(stateText, x, y, 0xFF88CCFF);
        y += fontRenderer.FONT_HEIGHT + 3;

        // Round info with better formatting
        if (game.currentRound > 0) {
            String roundText = "Round " + game.currentRound + "/" + game.totalRounds;
            fontRenderer.drawStringWithShadow(roundText, x, y, 0xFFFFDD88);
            y += fontRenderer.FONT_HEIGHT + 3;
        }

        // Theme with prefix
        if (!game.currentTheme.isEmpty()) {
            fontRenderer.drawStringWithShadow("> " + game.currentTheme, x, y, 0xFF88FF88);
            y += fontRenderer.FONT_HEIGHT + 3;
        }

        // Timer with symbol
        if (!game.currentTimer.isEmpty()) {
            fontRenderer.drawStringWithShadow("[T] " + game.currentTimer, x, y, 0xFFFFFF88);
            y += fontRenderer.FONT_HEIGHT + 3;
        }

        // Current builder with spinner
        if (game.currentBuilder != null) {
            String spinnerFrame = getSpinnerFrame();
            fontRenderer.drawStringWithShadow(spinnerFrame + " " + game.currentBuilder.name, x, y, 0xFF88FFFF);
            y += fontRenderer.FONT_HEIGHT + 5;
        }

        // Gap between cards for professional look
        y += 6;

        // Players section
        List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
        
        // Players header with background - proper spacing calculation
        int playersStartY = y;
        int playersHeight = (fontRenderer.FONT_HEIGHT + 4) + (sortedPlayers.size() * (fontRenderer.FONT_HEIGHT + 3)) + 2;
        Gui.drawRect(x - padding, y - 2, x + maxWidth, y + playersHeight, 0x50000000);
        
        fontRenderer.drawStringWithShadow("Players (" + sortedPlayers.size() + ")", x, y, 0xFFFFFFFF);
        y += fontRenderer.FONT_HEIGHT + 4;

        for (GameTracker.Player player : sortedPlayers) {
            String playerLine = renderPlayerLine(player, game);
            int color = getPlayerColor(player, game);
            
            // Enhanced background for current user - more subtle
            if (player.isUser) {
                Gui.drawRect(x - 1, y - 1, x + maxWidth - padding, y + fontRenderer.FONT_HEIGHT + 1, 0x30FFFF88);
            }
            
            // Highlight builder with subtle background - more subtle
            if (player.equals(game.currentBuilder)) {
                Gui.drawRect(x - 1, y - 1, x + maxWidth - padding, y + fontRenderer.FONT_HEIGHT + 1, 0x2088FFFF);
            }
            
            fontRenderer.drawStringWithShadow(playerLine, x, y, color);
            y += fontRenderer.FONT_HEIGHT + 3; // Increased spacing to prevent overlap
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
            line.append("~ ");
        } else {
            line.append("+ ");
        }
        
        // Player name
        line.append(player.name);
        
        // Points with better formatting
        int totalPoints = player.getTotalPoints();
        line.append(" ").append(totalPoints);
        
        // Current round points with + symbol
        if (game.currentRound > 0 && game.currentRound <= player.points.length) {
            int currentRoundPoints = player.points[game.currentRound - 1];
            if (currentRoundPoints > 0) {
                line.append(" (+").append(currentRoundPoints).append(")");
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
    
    private String formatStateName(String stateName) {
        switch (stateName) {
            case "PRE_GAME": return "Waiting";
            case "SETTING_UP": return "Setting Up";
            case "BUILDING": return "Building";
            case "GUESSING": return "Guessing";
            case "GAME_END": return "Game End";
            case "TRANSITIONING": return "Next Round";
            default: return stateName.replace("_", " ");
        }
    }
    
    private void renderWaitingMessage() {
        tickCounter++;
        
        ScaledResolution scaledRes = new ScaledResolution(mc);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        FontRenderer fontRenderer = mc.fontRendererObj;

        // Calculate position
        int maxWidth = 110;
        int x = screenWidth - maxWidth - 8;
        int y = 20;
        int padding = 3;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // Simple waiting message
        String spinnerFrame = getSpinnerFrame();
        String waitingText = spinnerFrame + " Waiting for game...";
        
        int textHeight = fontRenderer.FONT_HEIGHT + 6;
        
        // Draw background
        Gui.drawRect(x - padding, y - 2, x + maxWidth, y + textHeight, 0x50000000);
        
        // Draw text
        fontRenderer.drawStringWithShadow(waitingText, x, y + 2, 0xFF88CCFF);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}