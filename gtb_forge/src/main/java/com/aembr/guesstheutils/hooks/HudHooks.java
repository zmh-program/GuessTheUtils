package com.aembr.guesstheutils.hooks;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public class HudHooks {
    private static Field overlayObjectiveField;
    private static boolean fieldInitialized = false;
    private static ScoreObjective savedObjective = null;
    private static ScoreObjective sidebarObjectiveBackup = null;
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        // Handle vanilla scoreboard hiding when our custom one is active
        if (com.aembr.guesstheutils.modules.CustomScoreboard.isRendering()) {
            
            // For MC 1.8.9, we need to intercept the TEXT element which renders the scoreboard
            if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
                // Temporarily clear the sidebar objective during text rendering
                disableVanillaScoreboard();
            }
            
            // Also disable during player list rendering to prevent any potential sidebar display there
            if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null && mc.theWorld != null) {
                    ScoreObjective objective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
                    if (objective != null) {
                        event.setCanceled(true);
                        return;
                    }
                }
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
            Minecraft mc = Minecraft.getMinecraft();
            
            // Method 1: Try to clear GUI field
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null) {
                overlayObjectiveField.setAccessible(true);
                GuiIngame gui = mc.ingameGUI;
                
                // Save current objective if not already saved
                if (savedObjective == null) {
                    savedObjective = (ScoreObjective) overlayObjectiveField.get(gui);
                }
                
                // Clear the objective to hide vanilla scoreboard
                overlayObjectiveField.set(gui, null);
            }
            
            // Method 2: Temporarily clear sidebar objective from world scoreboard
            if (mc.theWorld != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null && sidebarObjectiveBackup == null) {
                    sidebarObjectiveBackup = worldScoreboard.getObjectiveInDisplaySlot(1);
                    if (sidebarObjectiveBackup != null) {
                        worldScoreboard.setObjectiveInDisplaySlot(1, null);
                    }
                }
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
            Minecraft mc = Minecraft.getMinecraft();
            
            // Method 1: Restore GUI field
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null && savedObjective != null) {
                overlayObjectiveField.setAccessible(true);
                overlayObjectiveField.set(mc.ingameGUI, savedObjective);
                savedObjective = null; // Clear saved state
            }
            
            // Method 2: Restore sidebar objective to world scoreboard
            if (mc.theWorld != null && sidebarObjectiveBackup != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null) {
                    worldScoreboard.setObjectiveInDisplaySlot(1, sidebarObjectiveBackup);
                    sidebarObjectiveBackup = null; // Clear backup
                }
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
            // The most likely candidates for 1.8.9
            String[] fieldNames = {
                "field_175196_v",       // Known obfuscated name for MC 1.8.9
                "field_94529_c",        // Alternative obfuscated name
                "overlayObjective",     // Deobfuscated name
                "objectiveToRender",    // Another possible name  
                "scoreObjective"        // Simple name
            };
            
            for (String fieldName : fieldNames) {
                try {
                    overlayObjectiveField = ReflectionHelper.findField(GuiIngame.class, fieldName);
                    GuessTheUtils.LOGGER.info("Successfully found scoreboard field: " + fieldName);
                    break;
                } catch (Exception e) {
                    GuessTheUtils.LOGGER.debug("Field " + fieldName + " not found: " + e.getMessage());
                }
            }
            
            if (overlayObjectiveField == null) {
                // Try to find any ScoreObjective field in GuiIngame
                try {
                    java.lang.reflect.Field[] fields = GuiIngame.class.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getType() == ScoreObjective.class) {
                            overlayObjectiveField = field;
                            GuessTheUtils.LOGGER.info("Found ScoreObjective field by type: " + field.getName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    GuessTheUtils.LOGGER.error("Failed to find field by type", e);
                }
            }
            
            if (overlayObjectiveField == null) {
                GuessTheUtils.LOGGER.warn("Could not find any scoreboard overlay field - vanilla scoreboard hiding may not work");
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Error initializing scoreboard field", e);
        } finally {
            fieldInitialized = true;
        }
    }
} 