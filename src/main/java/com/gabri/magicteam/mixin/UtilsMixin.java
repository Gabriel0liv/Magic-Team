package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin {

    /**
     * Intercepts general beneficial spell targeting (Mass Heal, Greater Heal, Buffs).
     * Corrected signature for ISS 3.15.3: uses (Entity, Entity) instead of (LivingEntity, LivingEntity).
     */
    @Inject(method = "shouldHealEntity", at = @At("HEAD"), cancellable = true)
    private static void onShouldHealEntity(Entity healer, Entity target, CallbackInfoReturnable<Boolean> cir) {
        // Cast to LivingEntity for team logic checks if both are living
        if (healer instanceof LivingEntity livingHealer && target instanceof LivingEntity livingTarget) {
            cir.setReturnValue(TeamUtils.shouldAllowBeneficial(livingHealer, livingTarget));
        }
    }
}
