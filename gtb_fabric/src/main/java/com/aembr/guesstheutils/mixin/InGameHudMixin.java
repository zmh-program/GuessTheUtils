package com.aembr.guesstheutils.mixin;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(at = @At("HEAD"), method = "setTitle")
	private void onSetTitle(Text title, CallbackInfo ci) {
		GuessTheUtils.onTitleSet(title);
	}

	@Inject(at = @At("HEAD"), method = "setSubtitle")
	private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
		GuessTheUtils.onSubtitleSet(subtitle);
	}
}