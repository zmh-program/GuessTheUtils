package com.aembr.guesstheutils.interceptor;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.modules.CustomScoreboard;
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
                    isIntercepting = true;
                    GuessTheUtils.LOGGER.debug("Starting to intercept scoreboard");
                }
                
                // Apply our customization
                updateCustomScoreboard(scoreboard, sidebarObjective);
            } else if (isIntercepting) {
                // Stop intercepting
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
        
        String cleanDisplayName = EnumChatFormatting.getTextWithoutFormattingCodes(displayName);
        boolean isGtb = cleanDisplayName.toLowerCase().contains("guess the build");
        boolean shouldCustomize = com.aembr.guesstheutils.modules.CustomScoreboard.isRendering();
        
        return isGtb && shouldCustomize;
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
                if (line != null) {
                    if (line.length() > 40) {
                        line = line.substring(0, 40);

                        if (line.endsWith("§")) {
                            line = line.substring(0, line.length() - 1);
                        }
                    }
                    int scoreValue = customLines.length - i;
                    scoreboard.getValueFromObjective(line, objective).setScorePoints(scoreValue);
                }
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error updating custom scoreboard: " + e.getMessage());
        }
    }
    
    private String[] getCustomScoreboardLines() {
        return CustomScoreboard.getCustomScoreboardLines();
    }
    

    public static List<String> getOriginalScoreboardLines() {
        // Use the new OriginalScoreboardCapture to get truly original data
        List<String> capturedLines = OriginalScoreboardCapture.getOriginalScoreboardLinesFormatted();
        if (!capturedLines.isEmpty()) {
            return capturedLines;
        }
        
        // If no captured data, fall back to current scoreboard
        return getScoreboardLines();
    }

    public static List<String> getScoreboardLines() {
        // deprecated since we are using the scoreboard interceptor
        List<String> lines = new ArrayList<String>();
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return lines;
        
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return lines;
        
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // SIDEBAR
        if (objective == null) return lines;
        
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
        }
        
        if (objective.getDisplayName() != null) {
            lines.add(0, EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName()));
        }
        
        return lines;
    }
}