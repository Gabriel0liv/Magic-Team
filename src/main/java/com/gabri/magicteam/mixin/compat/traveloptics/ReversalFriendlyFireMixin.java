package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reversal retaliates several ticks after the original hit with an ownerless
 * vanilla DamageSource and also recreates reflected projectiles without an owner.
 * Gate the delayed raycast explicitly and preserve the reflector as projectile owner.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.Reversal.ReversalEffect", remap = false)
public abstract class ReversalFriendlyFireMixin {

    @Redirect(
            method = "performRaycastAndApplyDamage(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$gateDelayedRetaliation(LivingEntity target,
                                                       DamageSource damageSource,
                                                       float amount,
                                                       LivingEntity reflector,
                                                       float adjustedDamage) {
        if (reflector != null && target != null && TeamUtils.shouldBlockFriendlyFire(reflector, target)) {
            return false;
        }
        return target.hurt(damageSource, amount);
    }

    @Redirect(
            method = "reflectProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$preserveReflectedProjectileOwner(Level level,
                                                                 Entity spawned,
                                                                 LivingEntity reflector,
                                                                 Projectile originalProjectile) {
        if (spawned instanceof Projectile projectile && projectile.getOwner() == null && reflector != null) {
            projectile.setOwner(reflector);
        }
        return level.addFreshEntity(spawned);
    }
}
