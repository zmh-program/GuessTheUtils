package com.aembr.guesstheutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {
    
    public static class PlayerInfo {
        public final String name;
        public final String prefix;
        public final String suffix;
        
        public PlayerInfo(String name, String prefix, String suffix) {
            this.name = name;
            this.prefix = prefix != null ? prefix : "";
            this.suffix = suffix != null ? suffix : "";
        }
        
        public String getFullDisplayName() {
            return prefix + name + suffix;
        }
        
        @Override
        public String toString() {
            return "PlayerInfo{name='" + name + "', prefix='" + prefix + "', suffix='" + suffix + "'}";
        }
    }
    
    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            ChatComponentText prefix = new ChatComponentText("[GuessTheUtils] ");
            prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
            
            ChatComponentText msg = new ChatComponentText(message);
            msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
            
            prefix.appendSibling(msg);
            mc.thePlayer.addChatMessage(prefix);
        }
    }
    
    public static void sendColoredMessage(String message, EnumChatFormatting color) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            ChatComponentText prefix = new ChatComponentText("[GuessTheUtils] ");
            prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
            
            ChatComponentText msg = new ChatComponentText(message);
            msg.getChatStyle().setColor(color);
            
            prefix.appendSibling(msg);
            mc.thePlayer.addChatMessage(prefix);
        }
    }
    
    public static List<String> collectTabListEntries() {
        List<String> entries = new ArrayList<String>();
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return entries;
        
        Collection<NetworkPlayerInfo> playerInfos = mc.getNetHandler().getPlayerInfoMap();
        
        for (NetworkPlayerInfo info : playerInfos) {
            String displayName = info.getDisplayName() != null ? 
                info.getDisplayName().getFormattedText() : 
                info.getGameProfile().getName();
            entries.add(EnumChatFormatting.getTextWithoutFormattingCodes(displayName));
        }
        
        return entries;
    }
    
    public static List<PlayerInfo> collectPlayerInfos() {
        List<PlayerInfo> entries = new ArrayList<PlayerInfo>();
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return entries;
        
        Collection<NetworkPlayerInfo> playerInfos = mc.getNetHandler().getPlayerInfoMap();
        
        for (NetworkPlayerInfo info : playerInfos) {
            String playerName = info.getGameProfile().getName();
            String prefix = com.aembr.guesstheutils.interceptor.ScoreboardPacketInterceptor.getPlayerPrefix(playerName);
            String suffix = com.aembr.guesstheutils.interceptor.ScoreboardPacketInterceptor.getPlayerSuffix(playerName);
            
            entries.add(new PlayerInfo(playerName, prefix, suffix));
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

    public static class Pair<A, B> {
        private final A a;
        private final B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public A a() { return a; }
        public B b() { return b; }
    }
} 