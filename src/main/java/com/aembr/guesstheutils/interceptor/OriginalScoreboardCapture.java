package com.aembr.guesstheutils.interceptor;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;

import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class OriginalScoreboardCapture {
    
    // Store original scoreboard data before any modifications
    private static volatile String originalScoreboardTitle = "";
    private static final Map<String, Integer> originalScoreboardLines = new ConcurrentHashMap<>();
    private static final List<String> orderedOriginalLines = new ArrayList<>();
    private static volatile long lastCaptureTime = 0;
    private static boolean isHookInstalled = false;
    private static Scoreboard originalScoreboard = null;
    private static ScoreboardProxy proxyScoreboard = null;
    private static net.minecraft.world.World currentWorld = null;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return;
        }
        
        // Check if world has changed
        if (currentWorld != mc.theWorld) {
            currentWorld = mc.theWorld;
            isHookInstalled = false;
            originalScoreboardLines.clear();
            orderedOriginalLines.clear();
            originalScoreboardTitle = "";
            GuessTheUtils.LOGGER.debug("World changed, reinstalling scoreboard hook");
        }
        
        // Install hook if not already installed
        if (!isHookInstalled) {
            installScoreboardHook();
        }
    }
    
    private void installScoreboardHook() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) {
                return;
            }
            
            // Get the world's scoreboard field using reflection
            // Try both obfuscated and deobfuscated field names
            Field scoreboardField = null;
            try {
                scoreboardField = mc.theWorld.getClass().getDeclaredField("field_96442_D"); // obfuscated
            } catch (NoSuchFieldException e) {
                try {
                    scoreboardField = mc.theWorld.getClass().getDeclaredField("worldScoreboard"); // deobfuscated
                } catch (NoSuchFieldException e2) {
                    // Try to find the field by type
                    for (Field field : mc.theWorld.getClass().getDeclaredFields()) {
                        if (field.getType() == Scoreboard.class) {
                            scoreboardField = field;
                            break;
                        }
                    }
                    if (scoreboardField == null) {
                        throw new NoSuchFieldException("Could not find scoreboard field");
                    }
                }
            }
            scoreboardField.setAccessible(true);
            
            originalScoreboard = (Scoreboard) scoreboardField.get(mc.theWorld);
            if (originalScoreboard != null && !(originalScoreboard instanceof ScoreboardProxy)) {
                proxyScoreboard = new ScoreboardProxy(originalScoreboard);
                scoreboardField.set(mc.theWorld, proxyScoreboard);
                isHookInstalled = true;
                GuessTheUtils.LOGGER.debug("Successfully installed scoreboard hook");
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to install scoreboard hook: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Scoreboard proxy class that intercepts all scoreboard modifications
    private static class ScoreboardProxy extends Scoreboard {
        private final Scoreboard wrapped;
        
        public ScoreboardProxy(Scoreboard wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public Score getValueFromObjective(String name, ScoreObjective objective) {
            Score score = wrapped.getValueFromObjective(name, objective);
            
            // Capture sidebar data when it's accessed
            if (objective != null && wrapped.getObjectiveInDisplaySlot(1) == objective) {
                captureScoreboardState();
            }
            
            return score;
        }
        
        @Override
        public void removeObjectiveFromEntity(String name, ScoreObjective objective) {
            wrapped.removeObjectiveFromEntity(name, objective);
            
            // Capture state after removal
            if (objective != null && wrapped.getObjectiveInDisplaySlot(1) == objective) {
                captureScoreboardState();
            }
        }
        
        @Override
        public Collection<Score> getSortedScores(ScoreObjective objective) {
            return wrapped.getSortedScores(objective);
        }
        
        @Override
        public ScoreObjective getObjectiveInDisplaySlot(int slot) {
            return wrapped.getObjectiveInDisplaySlot(slot);
        }
        
        @Override
        public ScorePlayerTeam getPlayersTeam(String name) {
            return wrapped.getPlayersTeam(name);
        }
        
        // Capture current scoreboard state
        private void captureScoreboardState() {
            try {
                ScoreObjective sidebarObjective = wrapped.getObjectiveInDisplaySlot(1);
                if (sidebarObjective == null) {
                    originalScoreboardTitle = "";
                    originalScoreboardLines.clear();
                    orderedOriginalLines.clear();
                    return;
                }
                
                // Capture title
                String displayName = sidebarObjective.getDisplayName();
                originalScoreboardTitle = displayName != null ? displayName : "";
                
                // Capture score lines
                Map<String, Integer> tempLines = new HashMap<>();
                List<String> tempOrderedLines = new ArrayList<>();
                Collection<Score> scores = wrapped.getSortedScores(sidebarObjective);
                
                for (Score score : scores) {
                    if (score == null) continue;
                    
                    String playerName = score.getPlayerName();
                    int scoreValue = score.getScorePoints();
                    
                    if (playerName != null) {
                        ScorePlayerTeam team = wrapped.getPlayersTeam(playerName);
                        String formattedLine = ScorePlayerTeam.formatPlayerName(team, playerName);
                        
                        tempLines.put(playerName, scoreValue);
                        tempOrderedLines.add(formattedLine);
                    }
                }
                
                // Update the concurrent collections
                originalScoreboardLines.clear();
                originalScoreboardLines.putAll(tempLines);
                orderedOriginalLines.clear();
                orderedOriginalLines.addAll(tempOrderedLines);
                
            } catch (Exception e) {
                GuessTheUtils.LOGGER.debug("Error capturing scoreboard state: " + e.getMessage());
            }
        }
        
        // Delegate all other methods to the wrapped scoreboard
        @Override
        public ScoreObjective getObjective(String name) {
            return wrapped.getObjective(name);
        }
        
        @Override
        public ScoreObjective addScoreObjective(String name, IScoreObjectiveCriteria criteria) {
            return wrapped.addScoreObjective(name, criteria);
        }
        
        @Override
        public Collection<ScoreObjective> getScoreObjectives() {
            return wrapped.getScoreObjectives();
        }
        
        @Override
        public Collection<ScoreObjective> getObjectivesFromCriteria(IScoreObjectiveCriteria criteria) {
            return wrapped.getObjectivesFromCriteria(criteria);
        }
        
        @Override
        public boolean entityHasObjective(String name, ScoreObjective objective) {
            return wrapped.entityHasObjective(name, objective);
        }
        
        @Override
        public Collection<Score> getScores() {
            return wrapped.getScores();
        }
        
        @Override
        public Collection<String> getObjectiveNames() {
            return wrapped.getObjectiveNames();
        }
        
        @Override
        public void removeObjective(ScoreObjective objective) {
            wrapped.removeObjective(objective);
        }
        
        @Override
        public void setObjectiveInDisplaySlot(int slot, ScoreObjective objective) {
            wrapped.setObjectiveInDisplaySlot(slot, objective);
        }
        

        
        @Override
        public ScorePlayerTeam getTeam(String name) {
            return wrapped.getTeam(name);
        }
        
        @Override
        public ScorePlayerTeam createTeam(String name) {
            return wrapped.createTeam(name);
        }
        
        @Override
        public void removeTeam(ScorePlayerTeam team) {
            wrapped.removeTeam(team);
        }
        
        @Override
        public boolean addPlayerToTeam(String player, String team) {
            return wrapped.addPlayerToTeam(player, team);
        }
        
        @Override
        public boolean removePlayerFromTeams(String player) {
            return wrapped.removePlayerFromTeams(player);
        }
        
        @Override
        public void removePlayerFromTeam(String player, ScorePlayerTeam team) {
            wrapped.removePlayerFromTeam(player, team);
        }
        
        @Override
        public Collection<String> getTeamNames() {
            return wrapped.getTeamNames();
        }
        
        @Override
        public Collection<ScorePlayerTeam> getTeams() {
            return wrapped.getTeams();
        }
        
        @Override
        public void broadcastTeamCreated(ScorePlayerTeam team) {
            wrapped.broadcastTeamCreated(team);
        }
        
        @Override
        public void sendTeamUpdate(ScorePlayerTeam team) {
            wrapped.sendTeamUpdate(team);
        }
    }
    
    // Public methods to access original scoreboard data
    public static String getOriginalScoreboardTitle() {
        return originalScoreboardTitle;
    }
    
    public static Map<String, Integer> getOriginalScoreboardLines() {
        return new HashMap<>(originalScoreboardLines);
    }
    
    public static List<String> getOriginalScoreboardLinesFormatted() {
        return new ArrayList<>(orderedOriginalLines);
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
        
        for (String line : orderedOriginalLines) {
            sb.append("  ").append(line).append("\n");
        }
        
        return sb.toString();
    }
}