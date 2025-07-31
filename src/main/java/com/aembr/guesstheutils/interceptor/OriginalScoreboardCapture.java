package com.aembr.guesstheutils.interceptor;

import com.aembr.guesstheutils.GuessTheUtils;
import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.*;

@SideOnly(Side.CLIENT)
public class OriginalScoreboardCapture {
    
    private static boolean isInterceptorInstalled = false;
    private static ScoreboardPacketInterceptor interceptor = null;
    private static Channel currentChannel = null;
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        installPacketInterceptor();
    }
    
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        removePacketInterceptor();
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        // Check if we need to install the interceptor
        if (!isInterceptorInstalled && Minecraft.getMinecraft().thePlayer != null) {
            installPacketInterceptor();
        }
    }
    
    private void installPacketInterceptor() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            NetHandlerPlayClient netHandler = mc.thePlayer.sendQueue;
            if (netHandler == null) {
                return;
            }
            
            // Get the network manager using reflection
            Field networkManagerField = null;
            for (Field field : netHandler.getClass().getDeclaredFields()) {
                if (field.getType() == NetworkManager.class) {
                    networkManagerField = field;
                    break;
                }
            }
            
            if (networkManagerField == null) {
                GuessTheUtils.LOGGER.error("Could not find NetworkManager field");
                return;
            }
            
            networkManagerField.setAccessible(true);
            NetworkManager networkManager = (NetworkManager) networkManagerField.get(netHandler);
            
            if (networkManager == null) {
                return;
            }
            
            Channel channel = networkManager.channel();
            if (channel == null) {
                return;
            }
            
            // Remove old interceptor if exists
            if (currentChannel != null && interceptor != null) {
                try {
                    currentChannel.pipeline().remove(interceptor);
                } catch (Exception ignored) {}
            }
            
            // Add new interceptor
            interceptor = new ScoreboardPacketInterceptor();
            channel.pipeline().addBefore("packet_handler", "scoreboard_interceptor", interceptor);
            
            currentChannel = channel;
            isInterceptorInstalled = true;
            
            GuessTheUtils.LOGGER.info("Successfully installed scoreboard packet interceptor");
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to install packet interceptor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void removePacketInterceptor() {
        try {
            if (currentChannel != null && interceptor != null) {
                currentChannel.pipeline().remove(interceptor);
                GuessTheUtils.LOGGER.info("Removed scoreboard packet interceptor");
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.debug("Error removing packet interceptor: " + e.getMessage());
        } finally {
            isInterceptorInstalled = false;
            interceptor = null;
            currentChannel = null;
        }
    }
    
    
    
    // Public methods to access original scoreboard data
    public static String getOriginalScoreboardTitle() {
        return ScoreboardPacketInterceptor.getOriginalScoreboardTitle();
    }
    
    public static Map<String, Integer> getOriginalScoreboardLines() {
        // This method is deprecated, use getOriginalScoreboardLinesFormatted instead
        return new HashMap<>();
    }
    
    public static List<String> getOriginalScoreboardLinesFormatted() {
        return ScoreboardPacketInterceptor.getOriginalScoreboardLines();
    }
    
    public static boolean hasOriginalScoreboardData() {
        return ScoreboardPacketInterceptor.hasOriginalData();
    }
    
    // Get formatted original scoreboard content for debugging
    public static String getFormattedOriginalScoreboard() {
        if (!hasOriginalScoreboardData()) {
            return "No original scoreboard data available (packet interceptor not installed or no data received)";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Original Scoreboard Title: ").append(getOriginalScoreboardTitle()).append("\n");
        sb.append("Original Scoreboard Lines (from packets):\n");
        
        List<String> lines = getOriginalScoreboardLinesFormatted();
        for (int i = 0; i < lines.size(); i++) {
            sb.append("  ").append(i + 1).append(": ").append(lines.get(i)).append("\n");
        }
        
        sb.append("\nInterceptor Status: ").append(isInterceptorInstalled ? "Installed" : "Not Installed");
        
        return sb.toString();
    }
}