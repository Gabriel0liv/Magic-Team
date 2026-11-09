package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AoeEntity.class, remap = false)
public class AoeEntityMixin {

    @Inject(method = "checkHits", at = @At("HEAD"), remap = false)
    private void onCheckHitsStart(CallbackInfo ci) {
        AoeEntity aoe = (AoeEntity) (Object) this;
        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();

        if (attribution != null && TeamUtils.isEnabled()) {
            MagicAttributionIndex.record(aoe, attribution);
        } else if (TeamUtils.isEnabled()) {
            attribution = MagicAttributionIndex.get(aoe, aoe.level().getGameTime());
        }

        if (attribution != null) {
            MagicTeamEffectContext.push(aoe, attribution);
        } else {
            MagicTeamEffectContext.push(aoe);
        }
    }

    @Inject(method = "checkHits", at = @At("RETURN"), remap = false)
    private void onCheckHitsEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
