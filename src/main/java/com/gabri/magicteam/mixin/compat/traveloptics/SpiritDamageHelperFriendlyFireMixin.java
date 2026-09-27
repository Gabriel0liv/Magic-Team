package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Spirit Damage Helper executes after the originating hit and directly damages
 * entities around the Tremorsaurus spirit. The helper wearer is the true attacker,
 * so gate that delayed damage explicitly against scoreboard friendly fire.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.SpiritDamageHelperEffect", remap = false)
public abstract class SpiritDamageHelperFriendlyFireMixin {

    @Redirect(
            method = "findAndDamageTremorsaurus(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$gateDelayedSpiritDamage(Entity target,
                                                       DamageSource damageSource,
                                                       float amount,
                                                       LivingEntity attacker,
                                                       int amplifier) {
        if (attacker != null && target != null && TeamUtils.shouldBlockFriendlyFire(attacker, target)) {
            return false;
        }
        return target.hurt(damageSource, amount);
    }
}
