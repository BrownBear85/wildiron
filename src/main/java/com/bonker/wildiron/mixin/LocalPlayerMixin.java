package com.bonker.wildiron.mixin;

import com.bonker.wildiron.item.WildIronItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Redirect(method = {"aiStep", "canStartSprinting"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean wildiron_redirect_isUsingItem(LocalPlayer instance) {
        if (instance.getUseItem().getItem() instanceof WildIronItem) {
            return false;
        }
        return instance.isUsingItem();
    }
}
