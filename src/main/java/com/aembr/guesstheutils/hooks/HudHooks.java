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
                // More aggressive approach: cancel the entire TEXT event to prevent vanilla scoreboard rendering
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null && mc.theWorld != null) {
                    ScoreObjective objective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
                    if (objective != null) {
                        // Temporarily clear the sidebar objective during text rendering
                        disableVanillaScoreboard();
                        
                        // Cancel the event to prevent vanilla scoreboard from rendering
                        // This is more effective than just clearing objectives since server can restore them
                        event.setCanceled(true);
                        return;
                    }
                }
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
        
        // Additional safety check: ensure vanilla scoreboard stays hidden during custom rendering
        if (com.aembr.guesstheutils.modules.CustomScoreboard.isRendering()) {
            // Final pass to ensure vanilla scoreboard is hidden
            // This catches any server packets that might have restored it between Pre and Post events
            ensureVanillaScoreboardHidden();
        }
        
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
     * Disables vanilla scoreboard by clearing the overlay objective
     * Enhanced for third-party servers where objectives may be restored by server packets
     */
    private void disableVanillaScoreboard() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            
            // Method 1: Try to clear GUI field (most reliable method)
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null) {
                overlayObjectiveField.setAccessible(true);
                GuiIngame gui = mc.ingameGUI;
                
                // Always clear the overlay objective, even if already cleared
                // This ensures it stays cleared even if server packets restore it
                ScoreObjective currentObjective = (ScoreObjective) overlayObjectiveField.get(gui);
                if (currentObjective != null) {
                    // Save for restoration only if we haven't saved one yet
                    if (savedObjective == null) {
                        savedObjective = currentObjective;
                    }
                    overlayObjectiveField.set(gui, null);
                }
            }
            
            // Method 2: Clear sidebar objective from world scoreboard
            // Apply every time to counter server packet updates
            if (mc.theWorld != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null) {
                    ScoreObjective currentSidebarObjective = worldScoreboard.getObjectiveInDisplaySlot(1);
                    if (currentSidebarObjective != null) {
                        // Save for restoration only if we haven't saved one yet
                        if (sidebarObjectiveBackup == null) {
                            sidebarObjectiveBackup = currentSidebarObjective;
                        }
                        // Always clear it to counter server updates
                        worldScoreboard.setObjectiveInDisplaySlot(1, null);
                    }
                }
            }
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to disable vanilla scoreboard", e);
        }
    }
    
    /**
     * Ensures vanilla scoreboard stays hidden by quickly clearing any restored objectives
     * Used as a safety net against server packet interference
     */
    private void ensureVanillaScoreboardHidden() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            
            // Quick check and clear GUI overlay objective if present
            if (overlayObjectiveField != null) {
                overlayObjectiveField.setAccessible(true);
                ScoreObjective currentObjective = (ScoreObjective) overlayObjectiveField.get(mc.ingameGUI);
                if (currentObjective != null) {
                    overlayObjectiveField.set(mc.ingameGUI, null);
                }
            }
            
            // Quick check and clear world scoreboard sidebar if present
            if (mc.theWorld != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null) {
                    ScoreObjective sidebarObjective = worldScoreboard.getObjectiveInDisplaySlot(1);
                    if (sidebarObjective != null) {
                        worldScoreboard.setObjectiveInDisplaySlot(1, null);
                    }
                }
            }
            
        } catch (Exception e) {
            // Silent fail to avoid spam in logs since this is called frequently
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