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
        
        if (objective.getDisplayName() != null) {
            lines.add(0, EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName()));
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
    
    public static String stripFormatting(String text) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(text);
    }
    
    public static class FixedSizeBuffer<T> {
        private final int maxSize;
        private final List<T> buffer;

        public FixedSizeBuffer(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("Max size must be greater than 0");
            }
            this.maxSize = maxSize;
            this.buffer = new ArrayList<T>(maxSize);
        }

        public void add(T element) {
            buffer.add(0, element);
            if (buffer.size() > maxSize) {
                buffer.remove(buffer.size() - 1);
            }
        }

        public T get(int index) {
            if (index < 0 || index >= buffer.size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + buffer.size());
            }
            return buffer.get(index);
        }

        public int size() {
            return buffer.size();
        }
    }
    
    public static String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
} 