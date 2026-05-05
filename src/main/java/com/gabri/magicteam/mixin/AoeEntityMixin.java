package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AoeEntity.class, remap = false)
public class AoeEntityMixin {

    @Inject(method = "checkHits", at = @At("HEAD"), remap = false)
    private void onCheckHitsStart(CallbackInfo ci) {
        MagicTeamEffectContext.push((AoeEntity) (Object) this);
    }

    @Inject(method = "checkHits", at = @At("RETURN"), remap = false)
    private void onCheckHitsEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
