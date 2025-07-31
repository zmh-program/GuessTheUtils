package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.interceptor.ScoreboardInterceptor;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CustomScoreboard {
    private static final String[] BUILDING_SPINNER = new String[] {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    public static String EmptyLine = "§7";
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
        
        return true;
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
    
    /**
     * Generate custom scoreboard lines based on game state
     */
    public static String[] getCustomScoreboardLines() {
        try {
            System.out.println("CustomScoreboard: Getting custom scoreboard lines");
            
            if (GuessTheUtils.gameTracker == null || GuessTheUtils.gameTracker.game == null) {
                // Extract info from original scoreboard during waiting phase
                return getWaitingScoreboardLines();
            }
            
            GameTracker.Game game = GuessTheUtils.gameTracker.game;
            List<String> lines = new ArrayList<>();
            
            // Add empty line for spacing
            lines.add(EmptyLine);
            
            // Game state
            String stateText = formatStateName(GameTracker.state.name());
            lines.add("§b" + stateText);
            
            // Round info
            if (game.currentRound > 0) {
                lines.add("§6Round " + game.currentRound + "/" + game.totalRounds);
            }
            
            // Theme
            if (!game.currentTheme.isEmpty()) {
                lines.add("§a> " + game.currentTheme);
            }
            
            // Timer
            if (!game.currentTimer.isEmpty()) {
                lines.add("§e[T] " + game.currentTimer);
            }
            
            // Current builder
            if (game.currentBuilder != null) {
                String spinnerFrame = getSpinnerFrame();
                lines.add("§3" + spinnerFrame + " " + game.currentBuilder.name);
            }
            
            // Empty line for separation
            lines.add(EmptyLine);
            
            // Top 3 players
            List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
            int count = Math.min(3, sortedPlayers.size());
            
            for (int i = 0; i < count; i++) {
                GameTracker.Player player = sortedPlayers.get(i);
                String rank = "§" + (i == 0 ? "6" : i == 1 ? "e" : "f") + (i + 1) + ". ";
                String pointsStr = player.getTotalPoints() > 0 ? " §7(" + player.getTotalPoints() + ")" : "";
                lines.add(rank + "§f" + player.name + pointsStr);
            }
            
            // Server footer
            lines.add(EmptyLine);
            lines.add("§ewww.hypixel.net");
            
            return lines.toArray(new String[0]);
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error generating custom scoreboard lines", e);
            return new String[0];
        }
    }
    
    /**
     * Generate scoreboard lines for waiting phase
     */
    private static String[] getWaitingScoreboardLines() {
        try {
            List<String> lines = new ArrayList<>();
            String playerCount = "";
            String timeLeft = "";
            
            // Get CURRENT scoreboard data
            List<String> currentScoreboardLines = ScoreboardInterceptor.getOriginalScoreboardLines();

            // Parse CURRENT scoreboard lines to extract information  
            for (String line : currentScoreboardLines) {
                String cleanLine = line.trim();
                
                // Extract player count (e.g., "Players: 9/10")
                if (cleanLine.startsWith("Players:")) {
                    playerCount = cleanLine;
                }
                
                // Extract time (e.g., "Starting in 00:25 to allow time for additional players")
                if (cleanLine.contains("Starting in")) {
                    // Find the time pattern (mm:ss format)
                    Pattern timePattern = Pattern.compile("Starting in (\\d{2}:\\d{2})");
                    Matcher matcher = timePattern.matcher(cleanLine);
                    if (matcher.find()) {
                        timeLeft = "Starting in " + matcher.group(1);
                    } else {
                        // Fallback for simpler format
                        String[] parts = cleanLine.split("Starting in ");
                        if (parts.length > 1) {
                            String time = parts[1].split(" ")[0];
                            timeLeft = "Starting in " + time;
                        }
                    }
                }
            }
            
            // Build our custom waiting scoreboard
            String spinnerFrame = getSpinnerFrame();
            lines.add(EmptyLine);
            lines.add("§b" + spinnerFrame + " Waiting for Players");
            lines.add(EmptyLine);
            
            if (!playerCount.isEmpty()) {
                lines.add("§f" + playerCount);
            }
            
            if (!timeLeft.isEmpty()) {
                lines.add("§e" + timeLeft);
            } else {
                lines.add("§c" + spinnerFrame + " Waiting...");
            }
            
            lines.add(EmptyLine);
            lines.add("§ewww.hypixel.net");
            
            return lines.toArray(new String[0]);
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error generating waiting scoreboard lines", e);
            return new String[]{"§cError loading scoreboard"};
        }
    }
    
    /**
     * Get animated spinner frame for current builder indicator
     */
    private static String getSpinnerFrame() {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        long time = System.currentTimeMillis();
        int index = (int) ((time / 200) % frames.length); // Change every 200ms
        return frames[index];
    }
    
    /**
     * Get sorted players list by total points
     */
    private static List<GameTracker.Player> getSortedPlayers(GameTracker.Game game) {
        List<GameTracker.Player> players = new ArrayList<>(game.players);
        
        // Sort by total points (descending), then by name
        players.sort((p1, p2) -> {
            int pointsCompare = Integer.compare(p2.getTotalPoints(), p1.getTotalPoints());
            if (pointsCompare != 0) return pointsCompare;
            return p1.name.compareTo(p2.name);
        });
        
        return players;
    }
    
    /**
     * Format game state name for display
     */
    private static String formatStateName(String stateName) {
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
}