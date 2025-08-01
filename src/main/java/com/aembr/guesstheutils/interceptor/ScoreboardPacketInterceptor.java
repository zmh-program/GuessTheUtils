package com.aembr.guesstheutils.interceptor;

import com.aembr.guesstheutils.GuessTheUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardPacketInterceptor extends ChannelDuplexHandler {
    
    private static final Map<String, Integer> originalScores = new ConcurrentHashMap<>();
    private static final Map<String, String> teamPrefixes = new ConcurrentHashMap<>();
    private static final Map<String, String> teamSuffixes = new ConcurrentHashMap<>();
    private static final Map<String, String> playerTeams = new ConcurrentHashMap<>();
    private static String currentObjective = "";
    private static String objectiveDisplayName = "";
    private static final List<String> orderedLines = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean shouldPassthrough = true;
        
        try {
            if (msg instanceof Packet) {
                Packet<?> packet = (Packet<?>) msg;
                
                if (packet instanceof S3BPacketScoreboardObjective) {
                    handleObjectivePacket((S3BPacketScoreboardObjective) packet);
                } else if (packet instanceof S3CPacketUpdateScore) {
                    handleScorePacket((S3CPacketUpdateScore) packet);
                } else if (packet instanceof S3DPacketDisplayScoreboard) {
                    handleDisplayPacket((S3DPacketDisplayScoreboard) packet);
                } else if (packet instanceof S3EPacketTeams) {
                    shouldPassthrough = handleTeamsPacketSafely((S3EPacketTeams) packet);
                }
            }
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                GuessTheUtils.LOGGER.debug("Error in packet interception: " + e.getMessage(), e);
            }
        }
        
        if (shouldPassthrough) {
            super.channelRead(ctx, msg);
        }
    }
    
    private boolean handleTeamsPacketSafely(S3EPacketTeams packet) {
        try {
            if (packet == null) {
                return false;
            }
            
            String teamName = packet.func_149312_c();
            if (teamName == null || teamName.isEmpty()) {
                return false;
            }
            
            int action = packet.func_149307_h();
            
            if (action == 1) {
                handleTeamsPacket(packet);
                return false;
            }
            
            handleTeamsPacket(packet);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private void handleObjectivePacket(S3BPacketScoreboardObjective packet) {
        try {
            String objectiveName = packet.func_149339_c();
            String displayName = packet.func_149337_d();
            int action = packet.func_149338_e();
            
            if (action == 0 || action == 2) { // Create or update
                if (objectiveName.equals(currentObjective)) {
                    objectiveDisplayName = displayName;
                }
            } else if (action == 1) { // Remove
                if (objectiveName.equals(currentObjective)) {
                    originalScores.clear();
                    orderedLines.clear();
                }
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error handling objective packet: " + e.getMessage());
        }
    }
    
    private void handleScorePacket(S3CPacketUpdateScore packet) {
        try {
            if (packet == null) {
                return;
            }
            
            String playerName = packet.getPlayerName();
            String objectiveName = packet.getObjectiveName();
            if (playerName == null || objectiveName == null) {
                return;
            }
            
            int score = packet.getScoreValue();
            S3CPacketUpdateScore.Action action = packet.getScoreAction();
            
            if (objectiveName.equals(currentObjective)) {
                if (action == S3CPacketUpdateScore.Action.CHANGE) {
                    originalScores.put(playerName, score);
                } else if (action == S3CPacketUpdateScore.Action.REMOVE) {
                    originalScores.remove(playerName);
                }
                updateOrderedLines();
            }
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                GuessTheUtils.LOGGER.debug("Error handling score packet: " + e.getMessage(), e);
            }
        }
    }
    
    private void handleDisplayPacket(S3DPacketDisplayScoreboard packet) {
        try {
            int position = packet.func_149371_c();
            String objectiveName = packet.func_149370_d();
            
            if (position == 1) { // Sidebar
                currentObjective = objectiveName;
                updateOrderedLines();
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error handling display packet: " + e.getMessage());
        }
    }
    
    private void handleTeamsPacket(S3EPacketTeams packet) {
        try {
            if (packet == null) {
                return;
            }
            
            String teamName = packet.func_149312_c();
            if (teamName == null) {
                return;
            }
            
            int action = packet.func_149307_h();
            
            if (action == 0 || action == 2) { // Create or update team
                String prefix = packet.func_149311_e();
                String suffix = packet.func_149309_f();
                teamPrefixes.put(teamName, prefix != null ? prefix : "");
                teamSuffixes.put(teamName, suffix != null ? suffix : "");
                
                Collection<String> players = null;
                if (action == 0) { // Create team
                    players = packet.func_149310_g();
                    if (players != null) {
                        for (String player : players) {
                            if (player != null) {
                                playerTeams.put(player, teamName);
                            }
                        }
                    }
                }
            } else if (action == 1) { // Remove team
                teamPrefixes.remove(teamName);
                teamSuffixes.remove(teamName);
                playerTeams.entrySet().removeIf(entry -> teamName.equals(entry.getValue()));
            } else if (action == 3) { // Add players to team
                Collection<String> players = packet.func_149310_g();
                if (players != null) {
                    for (String player : players) {
                        if (player != null) {
                            playerTeams.put(player, teamName);
                        }
                    }
                }
            } else if (action == 4) { // Remove players from team
                Collection<String> players = packet.func_149310_g();
                if (players != null) {
                    for (String player : players) {
                        if (player != null) {
                            playerTeams.remove(player);
                        }
                    }
                }
            }
            
            updateOrderedLines();
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                GuessTheUtils.LOGGER.debug("Error handling teams packet: " + e.getMessage(), e);
            }
        }
    }
    
    private void updateOrderedLines() {
        try {
            if (originalScores == null || orderedLines == null) {
                return;
            }
            
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(originalScores.entrySet());
            sortedEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            
            orderedLines.clear();
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                if (entry == null || entry.getKey() == null) {
                    continue;
                }
                
                String playerName = entry.getKey();
                String teamName = playerTeams.get(playerName);
                
                String formattedLine = playerName;
                if (teamName != null && teamPrefixes != null && teamSuffixes != null) {
                    String prefix = teamPrefixes.getOrDefault(teamName, "");
                    String suffix = teamSuffixes.getOrDefault(teamName, "");
                    formattedLine = prefix + playerName + suffix;
                }
                
                orderedLines.add(formattedLine);
            }
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                GuessTheUtils.LOGGER.debug("Error updating ordered lines: " + e.getMessage(), e);
            }
        }
    }
    
    public static String getOriginalScoreboardTitle() {
        return objectiveDisplayName;
    }
    
    public static String getOriginalScoreboardObjective() {
        return currentObjective;
    }
    
    public static List<String> getOriginalScoreboardLines() {
        return new ArrayList<>(orderedLines);
    }
    
    public static boolean hasOriginalData() {
        return !orderedLines.isEmpty();
    }
    
    public static String getPlayerDisplayName(String playerName) {
        if (playerName == null) return null;
        
        String teamName = playerTeams.get(playerName);
        if (teamName == null) return playerName;
        
        String prefix = teamPrefixes.getOrDefault(teamName, "");
        String suffix = teamSuffixes.getOrDefault(teamName, "");
        
        return prefix + playerName + suffix;
    }
    
    public static Map<String, String> getAllPlayerDisplayNames() {
        Map<String, String> result = new HashMap<>();
        for (String playerName : playerTeams.keySet()) {
            result.put(playerName, getPlayerDisplayName(playerName));
        }
        return result;
    }
    
    public static String getPlayerPrefix(String playerName) {
        if (playerName == null) return "";
        
        String teamName = playerTeams.get(playerName);
        if (teamName == null) return "";
        
        return teamPrefixes.getOrDefault(teamName, "");
    }
    
    public static String getPlayerSuffix(String playerName) {
        if (playerName == null) return "";
        
        String teamName = playerTeams.get(playerName);
        if (teamName == null) return "";
        
        return teamSuffixes.getOrDefault(teamName, "");
    }
}