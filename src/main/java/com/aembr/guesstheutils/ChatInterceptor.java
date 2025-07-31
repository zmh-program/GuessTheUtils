package com.aembr.guesstheutils;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;

import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatInterceptor {
    private static final String HANDLER_NAME = "guesstheutils_chat_interceptor";
    private static boolean isInjected = false;
    private static int injectionAttempts = 0;
    private static int ticksSinceLastAttempt = 0;
    
    private static final ConcurrentLinkedQueue<String> pendingChatMessages = new ConcurrentLinkedQueue<>();
    private static volatile String lastActionBarMessage = null;
    

    
    public static void init() {
        GuessTheUtils.LOGGER.info("ChatInterceptor initialized with netty handler injection");
    }
    
    public static void forceReinject() {
        isInjected = false;
        injectionAttempts = 0;
        ticksSinceLastAttempt = 0;
        GuessTheUtils.LOGGER.info("Forced reinjection requested");
    }
    
    public static void extractPendingMessages(Tick tick) {
        if (tick == null) return;
        
        if (!pendingChatMessages.isEmpty()) {
            if (tick.chatMessages == null) {
                tick.chatMessages = new ArrayList<>();
            }
            
            String message;
            while ((message = pendingChatMessages.poll()) != null) {
                tick.chatMessages.add(message);
            }
        }
        
        if (lastActionBarMessage != null) {
            tick.actionBarMessage = lastActionBarMessage;
            lastActionBarMessage = null;
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        if (!isInjected && injectionAttempts < 30) {
            ticksSinceLastAttempt++;
            
            boolean shouldAttempt = false;
            if (injectionAttempts < 5) {
                shouldAttempt = true;
            } else if (ticksSinceLastAttempt >= 20) {
                shouldAttempt = true;
            }
            
            if (shouldAttempt) {
                injectionAttempts++;
                ticksSinceLastAttempt = 0;
                GuessTheUtils.LOGGER.debug("Injection attempt " + injectionAttempts + "/30 (delay: " + (injectionAttempts < 5 ? "none" : "1s") + ")");
                tryInjectNettyHandler();
            }
        }
    }
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        GuessTheUtils.LOGGER.info("Client connected to server event fired");
        resetInjectionState();
        tryInjectNettyHandler();
    }
    
    @SubscribeEvent
    public void onClientDisconnectedFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        GuessTheUtils.LOGGER.info("Client disconnected from server event fired");
        removeNettyHandler();
        resetInjectionState();
    }
    
    private void resetInjectionState() {
        isInjected = false;
        injectionAttempts = 0;
        ticksSinceLastAttempt = 0;
        GuessTheUtils.LOGGER.debug("Injection state reset");
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
                
                String[] targetHandlers = {"packet_handler", "fml:packet_handler", "handler", "inbound_config", "decoder"};
                boolean injected = false;
                
                for (String targetHandler : targetHandlers) {
                    if (networkManager.channel().pipeline().get(targetHandler) != null) {
                        try {
                            networkManager.channel().pipeline().addBefore(targetHandler, HANDLER_NAME, new ChatPacketHandler());
                            isInjected = true;
                            injected = true;
                            GuessTheUtils.LOGGER.info("Successfully injected chat packet handler before '" + targetHandler + "'");
                            break;
                        } catch (Exception e) {
                            GuessTheUtils.LOGGER.debug("Failed to inject before '" + targetHandler + "': " + e.getMessage());
                        }
                    }
                }
                
                if (!injected) {
                    try {
                        networkManager.channel().pipeline().addLast(HANDLER_NAME, new ChatPacketHandler());
                        isInjected = true;
                        GuessTheUtils.LOGGER.info("Successfully injected chat packet handler at the end of pipeline");
                    } catch (Exception e) {
                        if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Failed to inject at end of pipeline: " + e.getMessage());
                    }
                }
            } else {
                if (!isInjected) {
                    isInjected = true;
                    GuessTheUtils.LOGGER.info("Handler already in pipeline, marking as injected");
                }
                if (injectionAttempts <= 5) GuessTheUtils.LOGGER.debug("Chat packet handler already injected");
            }
        } catch (Exception e) {
            if (injectionAttempts <= 5) GuessTheUtils.LOGGER.error("Failed to inject netty handler", e);
        }
    }
    
    private void removeNettyHandler() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getNetHandler() != null) {
                NetworkManager networkManager = mc.getNetHandler().getNetworkManager();
                
                if (networkManager != null && networkManager.channel() != null && networkManager.channel().pipeline() != null) {
                    if (networkManager.channel().pipeline().get(HANDLER_NAME) != null) {
                        networkManager.channel().pipeline().remove(HANDLER_NAME);
                        GuessTheUtils.LOGGER.info("Successfully removed chat packet handler");
                    } else {
                        GuessTheUtils.LOGGER.debug("Handler not found in pipeline during removal");
                    }
                } else {
                    GuessTheUtils.LOGGER.debug("Network components null during handler removal");
                }
            } else {
                GuessTheUtils.LOGGER.debug("Minecraft or NetHandler null during handler removal");
            }
            isInjected = false;
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to remove netty handler", e);
            isInjected = false;
        }
    }
    
    public static class ChatPacketHandler extends ChannelDuplexHandler {
        
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof C01PacketChatMessage) {
                C01PacketChatMessage chatPacket = (C01PacketChatMessage) msg;
                String message = getChatMessage(chatPacket);
                
                if (GuessTheUtils.chatCooldown != null && matchAvailableMessage(message)) {
                    if (GuessTheUtils.events.isInGtb()) {
                        GuessTheUtils.chatCooldown.onMessageSent();
                    }
                }
            }
            
            super.write(ctx, msg, promise);
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof S02PacketChat) {
                S02PacketChat chatPacket = (S02PacketChat) msg;
                handleIncomingChatPacket(chatPacket);
                    }
            
            super.channelRead(ctx, msg);
        }
        
        private void handleIncomingChatPacket(S02PacketChat packet) {
            try {
                IChatComponent chatComponent = getChatComponent(packet);
                byte chatType = getChatType(packet);
                
                if (chatComponent != null) {
                    String message = chatComponent.getUnformattedText();

                    if (chatType == 0) {
                        addChatMessage(message);
                    } else if (chatType == 2) {
                        setActionBarMessage(message);
                    }
                }
            } catch (Exception e) {
                GuessTheUtils.LOGGER.error("Failed to handle incoming chat packet", e);
            }
        }
        
        private IChatComponent getChatComponent(S02PacketChat packet) {
            try {
                // Try different possible field names for 1.8.9
                String[] possibleFieldNames = {"chatComponent", "field_148919_a", "a"};
                
                for (String fieldName : possibleFieldNames) {
                    try {
                        Field chatComponentField = S02PacketChat.class.getDeclaredField(fieldName);
                        chatComponentField.setAccessible(true);
                        return (IChatComponent) chatComponentField.get(packet);
                    } catch (NoSuchFieldException ignored) {}
                }
                
                GuessTheUtils.LOGGER.debug("Could not find chat component field. Available fields: " + java.util.Arrays.toString(S02PacketChat.class.getDeclaredFields()));
                return null;
            } catch (Exception e) {
                GuessTheUtils.LOGGER.error("Failed to get chat component from packet", e);
                return null;
            }
        }
        
        private byte getChatType(S02PacketChat packet) {
            try {
                // Try different possible field names for 1.8.9
                String[] possibleFieldNames = {"type", "field_148918_b", "b"};
                
                for (String fieldName : possibleFieldNames) {
                    try {
                        Field typeField = S02PacketChat.class.getDeclaredField(fieldName);
                        typeField.setAccessible(true);
                        return typeField.getByte(packet);
                    } catch (NoSuchFieldException ignored) {}
                }
                
                GuessTheUtils.LOGGER.debug("Could not find chat type field. Available fields: " + java.util.Arrays.toString(S02PacketChat.class.getDeclaredFields()));
                return 0;
            } catch (Exception e) {
                GuessTheUtils.LOGGER.error("Failed to get chat type from packet", e);
                return 0;
            }
        }
        
        private void addChatMessage(String message) {
            pendingChatMessages.offer(message);
        }
        
        private void setActionBarMessage(String message) {
            lastActionBarMessage = message;
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