package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractMagicProjectile.class, remap = false)
public class AbstractMagicProjectileMixin {

    @Inject(method = "handleHitDetection", at = @At("HEAD"), remap = false)
    private void onHandleHitDetectionStart(CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();

        if (attribution != null && TeamUtils.isEnabled()) {
            MagicAttributionIndex.record(projectile, attribution);
        } else if (TeamUtils.isEnabled()) {
            attribution = MagicAttributionIndex.get(projectile, projectile.level().getGameTime());
        }

        if (attribution != null) {
            MagicTeamEffectContext.push(projectile, attribution);
        } else {
            MagicTeamEffectContext.push(projectile);
        }
    }

    @Inject(method = "handleHitDetection", at = @At("RETURN"), remap = false)
    private void onHandleHitDetectionEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
