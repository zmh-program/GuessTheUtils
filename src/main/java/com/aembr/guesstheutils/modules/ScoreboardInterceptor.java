package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.modules.GameTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class ScoreboardInterceptor {
    
    private static boolean isIntercepting = false;
    private static String originalTitle = "";
    private static final Map<String, Integer> originalScores = new HashMap<>();
    private static final List<String> originalScoreboardLines = new ArrayList<>();
    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100; // Update every 100ms
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) return;
        lastUpdateTime = currentTime;
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) return;
            
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) return;
            
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective == null) return;
            
            // Check if this is a GTB scoreboard we should modify
            String displayName = sidebarObjective.getDisplayName();
            boolean shouldCustomize = shouldCustomizeScoreboard(displayName);
            
            if (shouldCustomize) {
                if (!isIntercepting) {
                    // First time intercepting - save original state
                    saveOriginalScoreboard(scoreboard, sidebarObjective);
                    isIntercepting = true;
                }
                updateCustomScoreboard(scoreboard, sidebarObjective);
            } else if (isIntercepting) {
                // Stop intercepting - restore original if needed
                isIntercepting = false;
                originalScores.clear();
                originalScoreboardLines.clear();
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error in scoreboard interception: " + e.getMessage());
        }
    }
    
    private boolean shouldCustomizeScoreboard(String displayName) {
        if (displayName == null) return false;
        
        // Check if it's a GTB scoreboard and if custom scoreboard is enabled
        boolean isGTB = displayName.toLowerCase().contains("guess the build");
        boolean shouldCustomize = com.aembr.guesstheutils.modules.CustomScoreboard.isRendering();
        
        return isGTB && shouldCustomize;
    }
    
    private void saveOriginalScoreboard(Scoreboard scoreboard, ScoreObjective objective) {
        try {
            originalTitle = objective.getDisplayName();
            originalScores.clear();
            originalScoreboardLines.clear();
            
            // Save original scores
            Collection<Score> scores = scoreboard.getSortedScores(objective);
            for (Score score : scores) {
                originalScores.put(score.getPlayerName(), score.getScorePoints());
            }
            
            // Save original scoreboard lines (same logic as Utils.getScoreboardLines())
            for (Score score : scores) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                originalScoreboardLines.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
            }
            
            if (objective.getDisplayName() != null) {
                originalScoreboardLines.add(0, EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName()));
            }
            
            GuessTheUtils.LOGGER.debug("Saved original scoreboard: " + originalTitle + " (" + originalScoreboardLines.size() + " lines)");
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error saving original scoreboard", e);
        }
    }
    
    private void updateCustomScoreboard(Scoreboard scoreboard, ScoreObjective objective) {
        try {
            // Get custom scoreboard lines
            String[] customLines = getCustomScoreboardLines();
            if (customLines == null) return;
            
            // Clear existing scores
            Collection<Score> existingScores = new ArrayList<>(scoreboard.getSortedScores(objective));
            for (Score score : existingScores) {
                scoreboard.removeObjectiveFromEntity(score.getPlayerName(), objective);
            }
            
            // Add custom scores
            for (int i = 0; i < customLines.length; i++) {
                String line = customLines[i];
                if (line != null && !line.trim().isEmpty()) {
                    // Use reverse index for proper ordering (higher scores appear at top)
                    int scoreValue = customLines.length - i;
                    scoreboard.getValueFromObjective(line, objective).setScorePoints(scoreValue);
                }
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error updating custom scoreboard: " + e.getMessage());
        }
    }
    
    private String[] getCustomScoreboardLines() {
        try {
            // System.out.println("Getting custom scoreboard lines: " + originalScoreboardLines);
            
            if (GuessTheUtils.gameTracker == null || GuessTheUtils.gameTracker.game == null) {
                // Extract info from original scoreboard during waiting phase
                return getWaitingScoreboardLines();
            }
            
            GameTracker.Game game = GuessTheUtils.gameTracker.game;
            java.util.List<String> lines = new java.util.ArrayList<>();
            
            // Add empty line for spacing
            lines.add("");
            
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
            lines.add("");
            
            // Top 3 players
            java.util.List<GameTracker.Player> sortedPlayers = getSortedPlayers(game);
            int count = Math.min(3, sortedPlayers.size());
            for (int i = 0; i < count; i++) {
                GameTracker.Player player = sortedPlayers.get(i);
                String prefix = player.isUser ? "§e" : "§f";
                if (player.equals(game.currentBuilder)) prefix = "§3";
                lines.add(prefix + player.name + " §7" + player.getTotalPoints());
            }
            
            // Empty line
            lines.add("");
            
            return lines.toArray(new String[0]);
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error generating custom scoreboard lines", e);
            return null;
        }
    }
    

    
    // Helper methods from CustomScoreboard
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
    
    private String getSpinnerFrame() {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        long time = System.currentTimeMillis();
        int index = (int) ((time / 200) % frames.length); // Change every 200ms
        return frames[index];
    }
    
    private java.util.List<GameTracker.Player> getSortedPlayers(GameTracker.Game game) {
        java.util.List<GameTracker.Player> players = new java.util.ArrayList<>(game.players);
        
        // Sort by total points (descending), then by name
        players.sort((p1, p2) -> {
            int pointsCompare = Integer.compare(p2.getTotalPoints(), p1.getTotalPoints());
            if (pointsCompare != 0) return pointsCompare;
            return p1.name.compareTo(p2.name);
        });
        
        return players;
    }
    
    // Public method to get original scoreboard lines for GTBEvents
    public static List<String> getOriginalScoreboardLines() {
        if (isIntercepting && !originalScoreboardLines.isEmpty()) {
            return new ArrayList<>(originalScoreboardLines);
        }
        
        // If not intercepting, fall back to current scoreboard
        return com.aembr.guesstheutils.Utils.getScoreboardLines();
    }
    
    private String[] getWaitingScoreboardLines() {
        try {
            List<String> lines = new ArrayList<>();
            String playerCount = "";
            String timeLeft = "";
            
            // Parse original scoreboard lines to extract information
            for (String line : originalScoreboardLines) {
                String cleanLine = line.trim();
                
                // Extract player count (e.g., "Players: 9/10")
                if (cleanLine.startsWith("Players:")) {
                    playerCount = cleanLine;
                }
                
                // Extract time (e.g., "Starting in 00:25 to allow time for additional players")
                if (cleanLine.contains("Starting in")) {
                    // Find the time pattern (mm:ss format)
                    java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("Starting in (\\d{2}:\\d{2})");
                    java.util.regex.Matcher matcher = timePattern.matcher(cleanLine);
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
            
            // Build custom waiting screen
            lines.add("");
            
            if (!timeLeft.isEmpty()) {
                lines.add("§e" + timeLeft);
            } else {
                String spinnerFrame = getSpinnerFrame();
                lines.add("§b" + spinnerFrame + " Waiting for game...");
            }
            
            if (!playerCount.isEmpty()) {
                lines.add("§a" + playerCount);
            }
            
            lines.add("");
            return lines.toArray(new String[0]);
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error parsing waiting scoreboard: " + e.getMessage());
            // Fallback to simple waiting message with spinner
            String spinnerFrame = getSpinnerFrame();
            return new String[] {
                "",
                "§b" + spinnerFrame + " Waiting for game...",
                ""
            };
        }
    }
}