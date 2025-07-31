package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;

import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class OriginalScoreboardCapture {
    
    // Store original scoreboard data before any modifications
    private static volatile String originalScoreboardTitle = "";
    private static final Map<String, Integer> originalScoreboardLines = new ConcurrentHashMap<>();
    private static volatile long lastCaptureTime = 0;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        
        // Capture original scoreboard data before ScoreboardInterceptor modifies it
        captureOriginalScoreboardData();
    }
    
    private void captureOriginalScoreboardData() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
                return;
            }
            
            // Throttle capture to avoid performance issues
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCaptureTime < 100) { // Only capture every 100ms
                return;
            }
            lastCaptureTime = currentTime;
            
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) {
                return;
            }
            
            // Get the sidebar objective (slot 1 is sidebar)
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective == null) {
                originalScoreboardTitle = "";
                originalScoreboardLines.clear();
                return;
            }
            
            // Capture original title
            String displayName = sidebarObjective.getDisplayName();
            originalScoreboardTitle = displayName != null ? displayName : "";
            
            // Capture original score lines
            Map<String, Integer> tempLines = new HashMap<>();
            Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
            
            for (Score score : scores) {
                if (score == null) continue;
                
                String playerName = score.getPlayerName();
                int scoreValue = score.getScorePoints();
                
                if (playerName != null) {
                    tempLines.put(playerName, scoreValue);
                }
            }
            
            // Update the concurrent map atomically
            originalScoreboardLines.clear();
            originalScoreboardLines.putAll(tempLines);
            
            // Debug output (only when data changes to avoid spam)
            if (!tempLines.isEmpty()) {
                System.out.println("DEBUG OriginalScoreboardCapture: Captured " + tempLines.size() + " score lines");
                tempLines.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(3) // Show first 3 lines only
                    .forEach(entry -> {
                        System.out.println("  " + entry.getValue() + ": '" + entry.getKey() + "'");
                    });
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.warn("Error capturing original scoreboard data: " + e.getMessage());
        }
    }
    
    // Public methods to access original scoreboard data
    public static String getOriginalScoreboardTitle() {
        return originalScoreboardTitle;
    }
    
    public static Map<String, Integer> getOriginalScoreboardLines() {
        return new HashMap<>(originalScoreboardLines);
    }
    
    public static boolean hasOriginalScoreboardData() {
        return !originalScoreboardTitle.isEmpty() || !originalScoreboardLines.isEmpty();
    }
    
    // Get formatted original scoreboard content for debugging
    public static String getFormattedOriginalScoreboard() {
        if (!hasOriginalScoreboardData()) {
            return "No original scoreboard data available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Original Scoreboard Title: ").append(originalScoreboardTitle).append("\n");
        sb.append("Original Scoreboard Lines:\n");
        
        originalScoreboardLines.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Sort by score descending
            .forEach(entry -> {
                sb.append("  ").append(entry.getValue()).append(": ").append(entry.getKey()).append("\n");
            });
        
        return sb.toString();
    }
}