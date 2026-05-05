package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonCloud;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PoisonCloud.class, remap = false)
public class PoisonCloudMixin {

    @Inject(method = "applyEffect(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false)
    private void onApplyEffectStart(LivingEntity target, CallbackInfo ci) {
        MagicTeamEffectContext.push((PoisonCloud) (Object) this);
    }

    @Inject(method = "applyEffect(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"), remap = false)
    private void onApplyEffectEnd(LivingEntity target, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
