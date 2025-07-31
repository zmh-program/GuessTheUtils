package com.aembr.guesstheutils;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

public class ChatInterceptor {
    private static final String HANDLER_NAME = "guesstheutils_chat_interceptor";
    private static boolean isInjected = false;
    private static int injectionAttempts = 0;
    
    public static void init() {
        GuessTheUtils.LOGGER.info("ChatInterceptor initialized with netty handler injection");
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        if (!isInjected && injectionAttempts < 100) {
            injectionAttempts++;
            tryInjectNettyHandler();
        }
    }
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        GuessTheUtils.LOGGER.info("Client connected to server event fired");
        tryInjectNettyHandler();
    }
    
    @SubscribeEvent
    public void onClientDisconnectedFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        GuessTheUtils.LOGGER.info("Client disconnected from server event fired");
        removeNettyHandler();
        injectionAttempts = 0;
    }
    
    private void tryInjectNettyHandler() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Minecraft instance is null");
                return;
            }
            
            if (mc.getNetHandler() == null) {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("NetHandler is null");
                return;
            }
            
            NetworkManager networkManager = mc.getNetHandler().getNetworkManager();
            if (networkManager == null) {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("NetworkManager is null");
                return;
            }
            
            if (networkManager.channel() == null) {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Channel is null");
                return;
            }
            
            if (networkManager.channel().pipeline() == null) {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Pipeline is null");
                return;
            }
            
            if (networkManager.channel().pipeline().get(HANDLER_NAME) == null) {
                GuessTheUtils.LOGGER.info("Attempting to inject handler. Pipeline names: " + networkManager.channel().pipeline().names());
                
                if (networkManager.channel().pipeline().get("packet_handler") != null) {
                    networkManager.channel().pipeline().addBefore("packet_handler", HANDLER_NAME, new ChatPacketHandler());
                    isInjected = true;
                    GuessTheUtils.LOGGER.info("Successfully injected chat packet handler before 'packet_handler'");
                } else if (networkManager.channel().pipeline().get("fml:packet_handler") != null) {
                    networkManager.channel().pipeline().addBefore("fml:packet_handler", HANDLER_NAME, new ChatPacketHandler());
                    isInjected = true;
                    GuessTheUtils.LOGGER.info("Successfully injected chat packet handler before 'fml:packet_handler'");
                } else {
                    networkManager.channel().pipeline().addLast(HANDLER_NAME, new ChatPacketHandler());
                    isInjected = true;
                    GuessTheUtils.LOGGER.info("Successfully injected chat packet handler at the end of pipeline");
                }
            } else {
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Chat packet handler already injected");
            }
        } catch (Exception e) {
            if (injectionAttempts <= 5) GuessTheUtils.LOGGER.error("Failed to inject netty handler", e);
        }
    }
    
    private void removeNettyHandler() {
        try {
            if (isInjected) {
                NetworkManager networkManager = Minecraft.getMinecraft().getNetHandler().getNetworkManager();
                
                if (networkManager != null && networkManager.channel() != null) {
                    if (networkManager.channel().pipeline().get(HANDLER_NAME) != null) {
                        networkManager.channel().pipeline().remove(HANDLER_NAME);
                        GuessTheUtils.LOGGER.info("Successfully removed chat packet handler");
                    }
                }
                isInjected = false;
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to remove netty handler", e);
        }
    }
    
    public static class ChatPacketHandler extends ChannelDuplexHandler {
        
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof C01PacketChatMessage) {
                C01PacketChatMessage chatPacket = (C01PacketChatMessage) msg;
                String message = getChatMessage(chatPacket);
                
                System.out.println("Message: " + message);
                if (matchAvailableMessage(message)) {
                    if (GuessTheUtils.chatCooldown != null) {
                        GuessTheUtils.chatCooldown.onMessageSent();
                    }
                }
            }
            
            super.write(ctx, msg, promise);
        }
        
        private String getChatMessage(C01PacketChatMessage packet) {
            try {
                Field messageField = C01PacketChatMessage.class.getDeclaredField("message");
                messageField.setAccessible(true);
                return (String) messageField.get(packet);
            } catch (Exception e) {
                GuessTheUtils.LOGGER.error("Failed to get message from chat packet", e);
                return "";
            }
        }
        
        private boolean matchAvailableMessage(String msg) {
            if (msg == null) return false;
            
            String cleanedMsg = msg.trim();
            return !cleanedMsg.isEmpty() && !cleanedMsg.startsWith("/");
        }
    }
}