package com.aembr.guesstheutils.mixin.item_hider;

import com.aembr.guesstheutils.modules.DisallowedItemHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.Item;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.item.ItemGroup$EntriesImpl")
public abstract class ItemGroup_EntriesImplMixin {

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;isEnabled(Lnet/minecraft/resource/featuretoggle/FeatureSet;)Z"))
    private boolean removeDisallowedItems(Item instance, FeatureSet featureSet, Operation<Boolean> original) {
        boolean originalVal = original.call(instance, featureSet);
        return DisallowedItemHider.isAllowed(instance) && originalVal;
    }
}
