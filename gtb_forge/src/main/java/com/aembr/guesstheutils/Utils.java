package com.aembr.guesstheutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {
    
    public static void sendMessage(String message) {
        if (GuessTheUtils.CLIENT.thePlayer != null) {
            ChatComponentText prefix = new ChatComponentText("[GuessTheUtils] ");
            prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
            
            ChatComponentText msg = new ChatComponentText(message);
            msg.getChatStyle().setColor(EnumChatFormatting.WHITE);
            
            prefix.appendSibling(msg);
            GuessTheUtils.CLIENT.thePlayer.addChatMessage(prefix);
        }
    }
    
    public static List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<String>();
        
        if (GuessTheUtils.CLIENT.theWorld == null) return lines;
        
        Scoreboard scoreboard = GuessTheUtils.CLIENT.theWorld.getScoreboard();
        if (scoreboard == null) return lines;
        
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // SIDEBAR
        if (objective == null) return lines;
        
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
        }
        
        return lines;
    }
    
    public static List<String> collectTabListEntries() {
        List<String> entries = new ArrayList<String>();
        
        if (GuessTheUtils.CLIENT.getNetHandler() == null) return entries;
        
        Collection<NetworkPlayerInfo> playerInfos = GuessTheUtils.CLIENT.getNetHandler().getPlayerInfoMap();
        
        for (NetworkPlayerInfo info : playerInfos) {
            String displayName = info.getDisplayName() != null ? 
                info.getDisplayName().getFormattedText() : 
                info.getGameProfile().getName();
            entries.add(EnumChatFormatting.getTextWithoutFormattingCodes(displayName));
        }
        
        return entries;
    }
    
    public static String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
} 