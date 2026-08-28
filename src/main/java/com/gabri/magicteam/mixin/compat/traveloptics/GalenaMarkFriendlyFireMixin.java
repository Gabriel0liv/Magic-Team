package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Galena Marks persist for 30 ticks and can outlive the relation state that
 * existed when they were applied. Recheck friendly fire before their periodic
 * damage and delayed magnetic blast, including both pull/push movement writes.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.misc.galena_mark.GalenaMarkEntity", remap = false)
public abstract class GalenaMarkFriendlyFireMixin {

    @Invoker("getTarget")
    protected abstract Entity magicTeam$getTarget();

    @Invoker("getCaster")
    protected abstract Entity magicTeam$getCaster();

    @Inject(
            method = "m_8119_()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private void magicTeam$skipProtectedDamageTick(CallbackInfo ci) {
        Entity caster = magicTeam$getCaster();
        Entity target = magicTeam$getTarget();
        if (caster != null && target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "triggerMagneticBlast()V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$skipProtectedOwnBlast(CallbackInfo ci) {
        Entity caster = magicTeam$getCaster();
        Entity target = magicTeam$getTarget();
        if (caster != null && target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "triggerMagneticBlast()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V",
                    remap = false
            ),
            require = 2,
            remap = false
    )
    private void magicTeam$gateMagneticMovement(Entity target, Vec3 movement) {
        Entity caster = magicTeam$getCaster();
        if (caster != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            return;
        }
        target.setDeltaMovement(movement);
    }
}
