package com.aembr.guesstheutils.hooks;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HudHooks {
    // Track whether we should be customizing for compatibility
    private static boolean isRenderingCustom = false;
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        // Note: ScoreboardInterceptor now handles scoreboard content modification
        // We no longer need to hide the vanilla scoreboard since we're modifying its content directly
        
        // Just track whether we should be customizing for compatibility
        isRenderingCustom = com.aembr.guesstheutils.modules.CustomScoreboard.isRendering();
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // Note: ScoreboardInterceptor now handles scoreboard content modification
        // The vanilla scoreboard will display our custom content automatically
        
        // Only render chat cooldown timer now
        if (GuessTheUtils.chatCooldown != null) {
            GuessTheUtils.chatCooldown.render();
        }
        
        // Reset the rendering flag
        isRenderingCustom = false;
    }
} 