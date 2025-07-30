package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
//? if >=1.21.6
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
//? if >=1.21.6
import net.minecraft.util.Identifier;

public class ChatCooldownTimer extends GTBEvents.Module /*? >=1.21.6 {*/ implements HudElement /*?}*/ {
    private final SoundEvent sound = SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
    private final int volume = 100; // 0 - 100
    private final int pitch = 12; // 1 - 20

    private boolean enabled = false;
    private long cooldown = 0;
    private long cooldownEndTime = 0;

    //? if >=1.21.6
    Identifier identifier = Identifier.of("guess_the_utils_chat_cooldown_timer");

    public ChatCooldownTimer(GTBEvents events) {
        super(events);
        //? if >=1.21.6 {
        try {
            HudElementRegistry.attachElementAfter(Identifier.ofVanilla("chat"), identifier, this);
        } catch (Exception e) {
            HudElementRegistry.replaceElement(identifier, hudElement -> this);
        }
        //?}

        ClientTickEvents.START_CLIENT_TICK.register(e -> update());
        events.subscribe(GTBEvents.UserCorrectGuessEvent.class, e -> disable(), this);
        events.subscribe(GTBEvents.RoundStartEvent.class, e -> enable(), this);
        events.subscribe(GTBEvents.RoundEndEvent.class, e -> disable(), this);
        events.subscribe(GTBEvents.UserLeaveEvent.class, e -> disable(), this);
    }

    public void render(DrawContext ctx /*? >=1.21.6 {*/ , RenderTickCounter tickCounter /*?}*/) {
        if (cooldown == 3000 || cooldown <= 0 || !GuessTheUtilsConfig.CONFIG.instance().enableChatCooldownModule
                || !GuessTheUtilsConfig.CONFIG.instance().chatCooldownTimer) return;
        String timerText = formatCooldown(cooldown);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int x1 = ctx.getScaledWindowWidth() / 2 + 7;
        int y1 = ctx.getScaledWindowHeight() / 2 - renderer.fontHeight - 7;
        int x2 = x1 + renderer.getWidth(timerText);
        int y2 = y1 + renderer.fontHeight - 1;
        ctx.fill(x1 - 2, y1 - 2, x2 + 1, y2 + 1, 0x66000000);
        ctx.drawText(renderer, timerText, x1, y1, 0xFFFFFF00, true);
    }

    public void onMessageSent() {
        if (!enabled || cooldown != 0) return;

        cooldown = 3000;
        cooldownEndTime = System.currentTimeMillis() + cooldown;
    }

    public void update() {
        if (enabled) {
            long previousCooldown = cooldown;
            cooldown = Math.max(0, cooldownEndTime - System.currentTimeMillis());
            if (cooldown == 0 && previousCooldown != 0) {
                playSound();
            }
        }
    }

    private void playSound() {
        if (!enabled || !GuessTheUtilsConfig.CONFIG.instance().enableChatCooldownModule
                || GuessTheUtilsConfig.CONFIG.instance().chatCooldownPingVolume == 0) return;
        if (GuessTheUtils.CLIENT.player == null || GuessTheUtils.CLIENT.world == null) return; {
            GuessTheUtils.CLIENT.execute(() -> {
                Entity camera = GuessTheUtils.CLIENT.cameraEntity;
                double x = (camera != null) ? camera.getPos().x : GuessTheUtils.CLIENT.player.getX();
                double y = (camera != null) ? camera.getPos().y : GuessTheUtils.CLIENT.player.getY();
                double z = (camera != null) ? camera.getPos().z : GuessTheUtils.CLIENT.player.getZ();
                GuessTheUtils.CLIENT.world.playSound(/*? if >=1.21.5 {*/ camera ,/*?}*/ x, y, z, sound, SoundCategory.PLAYERS,
                        GuessTheUtilsConfig.CONFIG.instance().chatCooldownPingVolume * 0.01F, pitch * 0.1F /*? if <1.21.5 {*//*, false*//*?}*/);
            });
        }
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
