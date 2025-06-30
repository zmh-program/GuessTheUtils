package com.aembr.guesstheutils.mixin;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"))
    private void onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText.startsWith("/")) return;
        //GuessTheUtils.chatCooldown.onMessageSent();
    }
}
