package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class CustomScoreboard {
    private GameTracker gameTracker;
    private Minecraft mc;

    public CustomScoreboard(GameTracker gameTracker) {
        this.gameTracker = gameTracker;
        this.mc = Minecraft.getMinecraft();
    }

    public void render() {
        if (mc.currentScreen != null) return;
        if (gameTracker.game == null) return;

        ScaledResolution scaledRes = new ScaledResolution(mc);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, 1.0f);

        int x = screenWidth - 120;
        int y = 10;

        mc.fontRendererObj.drawStringWithShadow("Game State: " + GameTracker.state.name(), x, y, 0xFFFFFF);
        y += 10;

        if (!gameTracker.game.currentTheme.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("Theme: " + gameTracker.game.currentTheme, x, y, 0x00FF00);
            y += 10;
        }

        if (!gameTracker.game.currentTimer.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("Timer: " + gameTracker.game.currentTimer, x, y, 0xFFFF00);
        }

        GlStateManager.popMatrix();
    }
} 