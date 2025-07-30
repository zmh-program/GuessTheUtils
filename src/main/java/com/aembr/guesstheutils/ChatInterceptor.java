package com.aembr.guesstheutils;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;

/**
 * ChatInterceptor for Minecraft Forge 1.8.9
 * 
 * Since ClientChatEvent doesn't exist in 1.8.9, this class provides an alternative approach
 * to intercept chat messages sent by the player. It works by:
 * 
 * 1. Listening for GuiOpenEvent to detect when the chat GUI is opened
 * 2. Replacing the standard GuiChat with a custom subclass
 * 3. Overriding the sendChatMessage method to trigger the chat cooldown
 * 
 * This allows the mod to detect when the player sends a message and activate
 * the chat cooldown timer accordingly.
 */
public class ChatInterceptor {
    
    public static void init() {
        GuessTheUtils.LOGGER.info("ChatInterceptor initialized");
    }
    
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChat && !(event.gui instanceof CustomGuiChat)) {
            GuiChat originalChat = (GuiChat) event.gui;
            event.gui = new CustomGuiChat(originalChat);
        }
    }
    
    public static class CustomGuiChat extends GuiChat {
        
        public CustomGuiChat(GuiChat original) {
            super();
            
            try {
                Field inputFieldField = GuiChat.class.getDeclaredField("inputField");
                inputFieldField.setAccessible(true);
                GuiTextField originalInputField = (GuiTextField) inputFieldField.get(original);
                
                if (originalInputField != null) {
                    String currentText = originalInputField.getText();
                    inputFieldField.set(this, originalInputField);
                    originalInputField.setText(currentText);
                }
            } catch (Exception e) {
                GuessTheUtils.LOGGER.error("Failed to copy input field from original GuiChat", e);
            }
        }

        public boolean matchAvailableMessage(String msg)  {
            if (msg == null) return false;
            
            String cleanedMsg = msg.trim();
            return !cleanedMsg.isEmpty() && !cleanedMsg.startsWith("/");
        }
        
        @Override
        public void sendChatMessage(String msg, boolean addToChat) {
            if (matchAvailableMessage(msg)) {
                if (GuessTheUtils.chatCooldown != null) {
                    GuessTheUtils.chatCooldown.onMessageSent();
                }
            }
            super.sendChatMessage(msg, addToChat);
        }
    }
}