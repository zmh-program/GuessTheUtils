package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.GTBEvents;
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

    private static String getPlayerColorCode(GameTracker.Player player, GameTracker.Game game) {
        if (player.prefix != null) {
            String prefix = player.prefix.trim();
            String extractTitle = GTBEvents.extractTitle(prefix);
            if (extractTitle != null && extractTitle.length() > 0) {
                prefix = prefix.replace(extractTitle, "[" + String.valueOf(extractTitle.charAt(0)) + "]").trim();
            }
            return prefix + " ";
        }

        if (player.leaverState == GameTracker.Player.LeaverState.LEAVER) {
            return "§7"; // Gray for leavers
        }
        
        if (player.equals(game.currentBuilder)) {
            return "§b"; // Aqua for builder
        }
        
        if (player.isUser) {
            return "§e"; // Yellow for user
        }
        
        return "§f"; // White default
    }
    
    /**
     * Generate custom scoreboard lines based on game state
     */
    public static String[] getCustomScoreboardLines() {
        try {
            if (GuessTheUtils.gameTracker == null || GuessTheUtils.gameTracker.game == null) {
                // Extract info from original scoreboard during waiting phase
                return getWaitingScoreboardLines();
            }
            
            GameTracker.Game game = GuessTheUtils.gameTracker.game;
            if (game == null) {
                return new String[0];
            }
            
            List<String> lines = new ArrayList<>();
            
            // Add empty line for spacing
            lines.add(EmptyLine);
            
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
            
            List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
            int count = sortedPlayers.size();
            
            for (int i = 0; i < count; i++) {
                GameTracker.Player player = sortedPlayers.get(i);
                String rank;
                if (player.equals(game.currentBuilder)) {
                    String spinnerFrame = getSpinnerFrame();
                    rank = "§3" + spinnerFrame + ". ";
                } else {
                    rank = "§" + (i == 0 ? "6" : i == 1 ? "e" : i == 2 ? "f" : "7") + (i + 1) + ". ";
                }
                String pointsStr = "§f:§7 " + player.getTotalPoints();
                
                Integer thisRoundPoints = player.getCurrentRoundPoints(game.currentRound);
                String thisRoundPointsStr = thisRoundPoints > 0 ? " (§a+" + thisRoundPoints + "§7)" : "";
                String playerColor = getPlayerColorCode(player, game);
                lines.add(rank + playerColor + player.name + pointsStr + thisRoundPointsStr);
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

            if (playerCount.isEmpty() && timeLeft.isEmpty()) {
                return null;
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
            case "NONE": return "Waiting";
            case "LOBBY": return "In Lobby";
            case "SETUP": return "Setting Up";
            case "ROUND_PRE": return "Starting";
            case "ROUND_BUILD": return "Building";
            case "ROUND_END": return "Round End";
            case "POST_GAME": return "Game End";
            default: return stateName.replace("_", " ");
        }
    }
}