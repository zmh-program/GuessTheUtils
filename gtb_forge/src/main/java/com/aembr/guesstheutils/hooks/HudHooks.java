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
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // Disable vanilla scoreboard by clearing the objective
        if (GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.scoreboard != null) {
            disableVanillaScoreboard();
        }
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // Render custom scoreboard
        if (GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.scoreboard != null) {
            GuessTheUtils.gameTracker.scoreboard.render();
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
        // try {
        //     if (!fieldInitialized) {
        //         // Try different possible field names for the overlay objective
        //         try {
        //             overlayObjectiveField = ReflectionHelper.findField(GuiIngame.class, "overlayObjective");
        //         } catch (Exception e1) {
        //             try {
        //                 overlayObjectiveField = ReflectionHelper.findField(GuiIngame.class, "field_94529_c");
        //             } catch (Exception e2) {
        //                 GuessTheUtils.LOGGER.warn("Could not find scoreboard overlay field");
        //                 return;
        //             }
        //         }
        //         fieldInitialized = true;
        //     }
            
        //     if (overlayObjectiveField != null) {
        //         overlayObjectiveField.setAccessible(true);
        //         overlayObjectiveField.set(Minecraft.getMinecraft().ingameGUI, null);
        //     }
        // } catch (Exception e) {
        //     GuessTheUtils.LOGGER.error("Failed to disable vanilla scoreboard", e);
        // }
    }
} 