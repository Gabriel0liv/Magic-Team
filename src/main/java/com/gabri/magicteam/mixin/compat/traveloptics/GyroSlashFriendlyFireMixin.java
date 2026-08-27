package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Gyro Slash applies Flare Vacuum immediately after DamageSources.applyDamage.
 * The damage helper already honors friendly fire, but the effect application did
 * not. Gate only the debuff call so the projectile's victim bookkeeping remains
 * unchanged and protected targets are not retried every tick.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.gyro_slash.GyroSlashProjectile", remap = false)
public abstract class GyroSlashFriendlyFireMixin {

    @Redirect(
            method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$gateFlareVacuum(LivingEntity target, MobEffectInstance effectInstance) {
        Entity projectile = (Entity) (Object) this;
        if (TeamUtils.shouldBlockFriendlyFire(projectile, target)) {
            return false;
        }
        return target.addEffect(effectInstance);
    }
}
