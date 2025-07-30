package com.aembr.guesstheutils.hooks;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HudHooks {
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        // In Forge 1.8.9, scoreboard has no dedicated ElementType
        // We need to overlay after ALL event to cover vanilla scoreboard
        if (GuessTheUtils.gameTracker != null && GuessTheUtils.gameTracker.scoreboard != null) {
            // Clear vanilla scoreboard area and render custom scoreboard
            clearVanillaScoreboardArea();
            GuessTheUtils.gameTracker.scoreboard.render();
            GuessTheUtils.LOGGER.debug("Custom scoreboard rendered");
        }
        
        if (GuessTheUtils.chatCooldown != null) {
            GuessTheUtils.chatCooldown.render();
        }
    }
    
    /**
     * Clears the vanilla scoreboard rendering area
     * This is necessary in Forge 1.8.9 to overlay custom scoreboard since there's no dedicated event to prevent vanilla scoreboard rendering
     */
    private void clearVanillaScoreboardArea() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaledRes = new ScaledResolution(mc);
        int width = scaledRes.getScaledWidth();
        int height = scaledRes.getScaledHeight();
        
        // Use black overlay to cover vanilla scoreboard area
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        
        // Scoreboard is usually on the right side, cover potential scoreboard area
        int x1 = width - 180; // 180 pixels width from right
        int y1 = 5;           // Start from top
        int x2 = width - 5;   // 5 pixels from edge  
        int y2 = height / 2 + 50; // Cover potential scoreboard height
        
        worldRenderer.pos(x1, y2, 0.0D).endVertex();
        worldRenderer.pos(x2, y2, 0.0D).endVertex();
        worldRenderer.pos(x2, y1, 0.0D).endVertex();
        worldRenderer.pos(x1, y1, 0.0D).endVertex();
        
        tessellator.draw();
        
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
} 