package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddEffect(MobEffectInstance effectInstance, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (source != null && source != (Object) this) {
            if (TeamUtils.areAllies(source, (Entity) (Object) this)) {
                if (!effectInstance.getEffect().isBeneficial()) {
                    // DEBUG LOGGING
                    System.out.println("[MagicTeam] LivingEntityMixin: BLOCKING effect " + effectInstance.getEffect().getDisplayName().getString() + " from ally " + source.getName().getString());
                    
                    // Visual Feedback
                    TeamUtils.sendProtectionMessage(source);
                    
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
