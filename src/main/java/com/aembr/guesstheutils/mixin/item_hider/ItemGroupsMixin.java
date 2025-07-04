package com.aembr.guesstheutils.mixin.item_hider;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemGroups.class)
public abstract class ItemGroupsMixin {

    @Shadow
    @Nullable
    private static ItemGroup.@Nullable DisplayContext displayContext;

    @Shadow
    private static void updateEntries(ItemGroup.DisplayContext displayContext) {
    }

    @Unique
    private static boolean isInGtb;

    @Inject(method = "updateDisplayContext", at = @At("HEAD"), cancellable = true)
    private static void trackLastVersion(FeatureSet enabledFeatures, boolean operatorEnabled, RegistryWrapper.WrapperLookup lookup, CallbackInfoReturnable<Boolean> cir) {
        if (GuessTheUtils.events.isInGtb() != isInGtb) {
            isInGtb = GuessTheUtils.events.isInGtb();
            displayContext = new ItemGroup.DisplayContext(enabledFeatures, operatorEnabled, lookup);
            updateEntries(displayContext);
            cir.setReturnValue(true);
        }
    }
}
