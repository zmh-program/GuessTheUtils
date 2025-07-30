package com.aembr.guesstheutils.hooks;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public class HudHooks {
    private static Field overlayObjectiveField;
    private static boolean fieldInitialized = false;
    private static ScoreObjective savedObjective = null;
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        // Cancel vanilla scoreboard rendering when our custom one is active
        if (com.aembr.guesstheutils.modules.CustomScoreboard.isRendering()) {
            // Cancel various elements that might render scoreboard
            if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST ||
                event.type == RenderGameOverlayEvent.ElementType.DEBUG ||
                event.type == RenderGameOverlayEvent.ElementType.TEXT) {
                
                // Check if this is scoreboard-related rendering
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null && mc.theWorld != null) {
                    ScoreObjective objective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1); // Sidebar slot
                    if (objective != null) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            
            // Also disable the scoreboard objective during ALL rendering
            if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
                disableVanillaScoreboard();
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // Render custom scoreboard if enabled
        if (GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.scoreboard != null) {
            GuessTheUtils.gameTracker.scoreboard.render();
        } else {
            // Restore vanilla scoreboard if custom one is not active
            restoreVanillaScoreboard();
        }
        
        // Render chat cooldown timer
        if (GuessTheUtils.chatCooldown != null) {
            GuessTheUtils.chatCooldown.render();
        }
    }
    
    /**
     * Disables vanilla scoreboard by temporarily clearing the overlay objective
     */
    private void disableVanillaScoreboard() {
        try {
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null) {
                overlayObjectiveField.setAccessible(true);
                GuiIngame gui = Minecraft.getMinecraft().ingameGUI;
                
                // Save current objective if not already saved
                if (savedObjective == null) {
                    savedObjective = (ScoreObjective) overlayObjectiveField.get(gui);
                }
                
                // Clear the objective to hide vanilla scoreboard
                overlayObjectiveField.set(gui, null);
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to disable vanilla scoreboard", e);
        }
    }
    
    /**
     * Restores vanilla scoreboard by setting back the saved objective
     */
    private void restoreVanillaScoreboard() {
        try {
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null && savedObjective != null) {
                overlayObjectiveField.setAccessible(true);
                overlayObjectiveField.set(Minecraft.getMinecraft().ingameGUI, savedObjective);
                savedObjective = null; // Clear saved state
            }
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to restore vanilla scoreboard", e);
        }
    }
    
    /**
     * Initialize the scoreboard field using reflection
     */
    private void initializeScoreboardField() {
        try {
            // Try multiple possible field names for MC 1.8.9
            String[] fieldNames = {
                "overlayObjective",      // Common name
                "field_94529_c",        // Obfuscated name 1.8.9
                "field_175196_v",       // Alternative obfuscated name
                "scoreboardObjective",   // Alternative name
                "sidebarObjective"      // Another possible name
            };
            
            for (String fieldName : fieldNames) {
                try {
                    overlayObjectiveField = ReflectionHelper.findField(GuiIngame.class, fieldName);
                    GuessTheUtils.LOGGER.info("Found scoreboard field: " + fieldName);
                    break;
                } catch (Exception e) {
                    // Continue trying other names
                }
            }
            
            if (overlayObjectiveField == null) {
                GuessTheUtils.LOGGER.warn("Could not find any scoreboard overlay field");
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error initializing scoreboard field", e);
        } finally {
            fieldInitialized = true;
        }
    }
} 