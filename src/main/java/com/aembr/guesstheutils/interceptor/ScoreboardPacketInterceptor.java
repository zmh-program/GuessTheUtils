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
        if (msg instanceof Packet) {
            Packet<?> packet = (Packet<?>) msg;
            
            if (packet instanceof S3BPacketScoreboardObjective) {
                handleObjectivePacket((S3BPacketScoreboardObjective) packet);
            } else if (packet instanceof S3CPacketUpdateScore) {
                handleScorePacket((S3CPacketUpdateScore) packet);
            } else if (packet instanceof S3DPacketDisplayScoreboard) {
                handleDisplayPacket((S3DPacketDisplayScoreboard) packet);
            } else if (packet instanceof S3EPacketTeams) {
                handleTeamsPacket((S3EPacketTeams) packet);
            }
        }
        
        super.channelRead(ctx, msg);
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
            String playerName = packet.getPlayerName();
            String objectiveName = packet.getObjectiveName();
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
            GuessTheUtils.LOGGER.debug("Error handling score packet: " + e.getMessage());
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
            String teamName = packet.func_149312_c();
            int action = packet.func_149307_h();
            
            if (action == 0 || action == 2) { // Create or update team
                String prefix = packet.func_149311_e();
                String suffix = packet.func_149309_f();
                teamPrefixes.put(teamName, prefix != null ? prefix : "");
                teamSuffixes.put(teamName, suffix != null ? suffix : "");
                
                if (action == 0) { // Create team
                    Collection<String> players = packet.func_149310_g();
                    if (players != null) {
                        for (String player : players) {
                            playerTeams.put(player, teamName);
                        }
                    }
                }
            } else if (action == 1) { // Remove team
                teamPrefixes.remove(teamName);
                teamSuffixes.remove(teamName);
                playerTeams.values().removeAll(Collections.singleton(teamName));
            } else if (action == 3) { // Add players to team
                Collection<String> players = packet.func_149310_g();
                if (players != null) {
                    for (String player : players) {
                        playerTeams.put(player, teamName);
                    }
                }
            } else if (action == 4) { // Remove players from team
                Collection<String> players = packet.func_149310_g();
                if (players != null) {
                    for (String player : players) {
                        playerTeams.remove(player);
                    }
                }
            }
            
            updateOrderedLines();
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error handling teams packet: " + e.getMessage());
        }
    }
    
    private void updateOrderedLines() {
        try {
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(originalScores.entrySet());
            sortedEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            
            orderedLines.clear();
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                String playerName = entry.getKey();
                String teamName = playerTeams.get(playerName);
                
                String formattedLine = playerName;
                if (teamName != null) {
                    String prefix = teamPrefixes.getOrDefault(teamName, "");
                    String suffix = teamSuffixes.getOrDefault(teamName, "");
                    formattedLine = prefix + playerName + suffix;
                }
                
                orderedLines.add(formattedLine);
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error updating ordered lines: " + e.getMessage());
        }
    }
    
    public static String getOriginalScoreboardTitle() {
        return objectiveDisplayName;
    }
    
    public static List<String> getOriginalScoreboardLines() {
        return new ArrayList<>(orderedLines);
    }
    
    public static boolean hasOriginalData() {
        return !orderedLines.isEmpty();
    }
}