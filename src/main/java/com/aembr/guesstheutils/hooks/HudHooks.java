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
    
    // For Hypixel compatibility - use render-time interception instead of data clearing
    private static boolean isRenderingCustom = false;
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        // Handle vanilla scoreboard hiding when our custom one is active
        if (com.aembr.guesstheutils.modules.CustomScoreboard.isRendering()) {
            isRenderingCustom = true;
            
            // For MC 1.8.9, we need to intercept the TEXT element which renders the scoreboard
            if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
                // Temporarily hide vanilla scoreboard only during this exact render frame
                temporarilyHideVanillaScoreboard();
            }
            
            // Also hide during player list rendering
            if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
                temporarilyHideVanillaScoreboard();
            }
        } else {
            isRenderingCustom = false;
        }
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // Immediately restore any temporarily hidden scoreboard elements after each render type
        if (isRenderingCustom && (event.type == RenderGameOverlayEvent.ElementType.TEXT || 
                                 event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST)) {
            restoreTemporaryHidden();
        }
        
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // Render custom scoreboard if enabled
        if (GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.scoreboard != null) {
            GuessTheUtils.gameTracker.scoreboard.render();
        } else if (!isRenderingCustom) {
            // Only restore vanilla scoreboard if we're not rendering custom one
            restoreVanillaScoreboard();
        }
        
        // Render chat cooldown timer
        if (GuessTheUtils.chatCooldown != null) {
            GuessTheUtils.chatCooldown.render();
        }
        
        // Reset the rendering flag
        isRenderingCustom = false;
    }
    

    
    /**
     * Temporarily hides vanilla scoreboard for this exact render frame only
     * Designed for Hypixel compatibility - no data persistence issues
     */
    private void temporarilyHideVanillaScoreboard() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            
            // Method 1: Temporarily clear GUI overlay objective
            if (!fieldInitialized) {
                initializeScoreboardField();
            }
            
            if (overlayObjectiveField != null) {
                overlayObjectiveField.setAccessible(true);
                GuiIngame gui = mc.ingameGUI;
                
                // Only save if we haven't already
                if (savedObjective == null) {
                    savedObjective = (ScoreObjective) overlayObjectiveField.get(gui);
                }
                
                // Temporarily clear for this render frame
                overlayObjectiveField.set(gui, null);
            }
            
            // Method 2: Temporarily clear world scoreboard sidebar
            if (mc.theWorld != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null) {
                    if (sidebarObjectiveBackup == null) {
                        sidebarObjectiveBackup = worldScoreboard.getObjectiveInDisplaySlot(1);
                    }
                    if (sidebarObjectiveBackup != null) {
                        worldScoreboard.setObjectiveInDisplaySlot(1, null);
                    }
                }
            }
            
        } catch (Exception e) {
            // Silent fail to avoid log spam
        }
    }
    
    /**
     * Immediately restores temporarily hidden scoreboard elements
     * Called after each render frame to prevent persistence issues
     */
    private void restoreTemporaryHidden() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            
            // Restore GUI overlay objective
            if (overlayObjectiveField != null && savedObjective != null) {
                overlayObjectiveField.setAccessible(true);
                overlayObjectiveField.set(mc.ingameGUI, savedObjective);
            }
            
            // Restore world scoreboard sidebar
            if (mc.theWorld != null && sidebarObjectiveBackup != null) {
                Scoreboard worldScoreboard = mc.theWorld.getScoreboard();
                if (worldScoreboard != null) {
                    worldScoreboard.setObjectiveInDisplaySlot(1, sidebarObjectiveBackup);
                }
            }
            
        } catch (Exception e) {
            // Silent fail to avoid log spam
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