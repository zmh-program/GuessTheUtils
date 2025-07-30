package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

public class ChatCooldownTimer extends GTBEvents.Module {
    private final String soundName = "note.bell";
    private final int volume = 100; // 0 - 100
    private final float pitch = 1.2f; // 0.5f - 2.0f

    private boolean enabled = false;
    private long cooldown = 0;
    private long cooldownEndTime = 0;

    public ChatCooldownTimer(GTBEvents events) {
        super(events);
        
        events.subscribe(GTBEvents.UserCorrectGuessEvent.class, e -> disable(), this);
        events.subscribe(GTBEvents.RoundStartEvent.class, e -> enable(), this);
        events.subscribe(GTBEvents.RoundEndEvent.class, e -> disable(), this);
        events.subscribe(GTBEvents.UserLeaveEvent.class, e -> disable(), this);
    }

    public void render() {
        if (cooldown == 3000 || cooldown <= 0 || !GuessTheUtilsConfig.enableChatCooldownTimer) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        
        String timerText = formatCooldown(cooldown);
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution scaledRes = new ScaledResolution(mc);
        
        int x1 = scaledRes.getScaledWidth() / 2 + 7;
        int y1 = scaledRes.getScaledHeight() / 2 - fontRenderer.FONT_HEIGHT - 7;
        int x2 = x1 + fontRenderer.getStringWidth(timerText);
        int y2 = y1 + fontRenderer.FONT_HEIGHT - 1;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        
        // Background
        Gui.drawRect(x1 - 2, y1 - 2, x2 + 1, y2 + 1, 0x66000000);
        
        // Text
        fontRenderer.drawStringWithShadow(timerText, x1, y1, 0xFFFFFF00);
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public void onMessageSent() {
        if (!enabled || cooldown != 0) return;

        cooldown = 3000;
        cooldownEndTime = System.currentTimeMillis() + cooldown;
    }

    public void onTick() {
        if (enabled) {
            long previousCooldown = cooldown;
            cooldown = Math.max(0, cooldownEndTime - System.currentTimeMillis());
            if (cooldown == 0 && previousCooldown != 0) {
                playSound();
            }
        }
    }

    private void playSound() {
        if (!enabled || !GuessTheUtilsConfig.enableChatCooldownTimer) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        Entity camera = mc.getRenderViewEntity();
        double x = (camera != null) ? camera.posX : mc.thePlayer.posX;
        double y = (camera != null) ? camera.posY : mc.thePlayer.posY;
        double z = (camera != null) ? camera.posZ : mc.thePlayer.posZ;
        
        mc.theWorld.playSound(x, y, z, soundName, volume * 0.01F, pitch, false);
    }

    public void enable() {
        enabled = true;
        cooldown = 0;
        cooldownEndTime = 0;
    }

    public void disable() {
        enabled = false;
        cooldown = 0;
        cooldownEndTime = 0;
    }

    private String formatCooldown(long cooldown) {
        int seconds = (int) (cooldown / 1000);
        int millis = (int) (cooldown % 1000);
        return String.format("%d.%03d", seconds, millis);
    }
}