package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicSideEffectPolicy;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Global gate for hostile magic that removes effects without dealing damage. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMagicSideEffectMixin {

    @Inject(
            method = "removeEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void magicTeam$gateRemoveEffect(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (MagicSideEffectPolicy.shouldBlock(target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeAllEffects()Z", at = @At("HEAD"), cancellable = true)
    private void magicTeam$gateRemoveAllEffects(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (MagicSideEffectPolicy.shouldBlock(target)) {
            cir.setReturnValue(false);
        }
    }
}
