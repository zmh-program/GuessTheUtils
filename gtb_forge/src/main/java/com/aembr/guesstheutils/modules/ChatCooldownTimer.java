package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class ChatCooldownTimer extends GTBEvents.Module {
    private int cooldownTicks = 0;
    private Minecraft mc;

    public ChatCooldownTimer(GTBEvents events) {
        super(events);
        this.mc = Minecraft.getMinecraft();
    }

    public void render() {
        if (cooldownTicks <= 0) return;
        if (mc.currentScreen != null) return;

        ScaledResolution scaledRes = new ScaledResolution(mc);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        int x = screenWidth / 2 - 50;
        int y = screenHeight - 50;

        String text = "Chat cooldown: " + (cooldownTicks / 20) + "s";
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFF0000);
    }

    public void onTick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public void startCooldown(int seconds) {
        this.cooldownTicks = seconds * 20;
    }
} 