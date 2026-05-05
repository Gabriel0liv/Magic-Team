package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractMagicProjectile.class, remap = false)
public class AbstractMagicProjectileMixin {

    @Inject(method = "handleHitDetection", at = @At("HEAD"), remap = false)
    private void onHandleHitDetectionStart(CallbackInfo ci) {
        MagicTeamEffectContext.push((AbstractMagicProjectile) (Object) this);
    }

    @Inject(method = "handleHitDetection", at = @At("RETURN"), remap = false)
    private void onHandleHitDetectionEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
