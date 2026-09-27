package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownPotion.class)
public class ThrownPotionMixin {

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"))
    private void onHitStart(HitResult hitResult, CallbackInfo ci) {
        ThrownPotion potion = (ThrownPotion) (Object) this;
        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution == null && TeamUtils.isEnabled()) {
            attribution = MagicAttributionIndex.get(potion, potion.level().getGameTime());
        }

        if (attribution != null) {
            MagicTeamEffectContext.push(potion, attribution);
        } else {
            MagicTeamEffectContext.pushVanillaPotion(potion);
        }
    }

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("RETURN"))
    private void onHitEnd(HitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
