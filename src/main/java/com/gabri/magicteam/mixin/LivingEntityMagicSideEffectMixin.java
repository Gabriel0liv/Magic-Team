package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicEffectAttributionIndex;
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
        // Removing a harmful effect is a beneficial cleanse. Mixed spells may
        // both cleanse allies and harm enemies, so classify the concrete side
        // effect rather than blindly inheriting the spell-wide HOSTILE label.
        if (effect == null || !effect.isBeneficial()) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        if (MagicSideEffectPolicy.shouldBlock(target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "removeEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
            at = @At("RETURN")
    )
    private void magicTeam$clearRemovedEffectAttribution(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect != null && cir.getReturnValueZ()) {
            MagicEffectAttributionIndex.remove((LivingEntity) (Object) this, effect);
        }
    }

    @Inject(method = "removeAllEffects()Z", at = @At("HEAD"), cancellable = true)
    private void magicTeam$gateRemoveAllEffects(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (MagicSideEffectPolicy.shouldBlock(target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeAllEffects()Z", at = @At("RETURN"))
    private void magicTeam$clearAllRemovedEffectAttribution(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            MagicEffectAttributionIndex.removeAll((LivingEntity) (Object) this);
        }
    }
}
